import {
  AutoModelForVision2Seq,
  AutoProcessor,
  RawImage,
  TextStreamer,
  env,
} from '@huggingface/transformers';
import * as pdfjs from 'pdfjs-dist/legacy/build/pdf.mjs';
import {
  MODEL_ARTIFACTS,
  MODEL_CACHE_NAME,
  MODEL_ID,
  MODEL_REMOTE_PATH_TEMPLATE,
  MODEL_REVISION,
  MODEL_TOTAL_BYTES,
  artifactForUrl,
  modelArtifactUrl,
  readVerifiedResponse,
  verifyArtifactBytes,
} from './model-artifacts.mjs';
import { isAllowedResourceRequest, requestHasBody, resolveResourceUrl } from './worker-network-policy.mjs';
import { loadModelWithFallback } from './inference-backend.mjs';

const MAX_DOCUMENT_BYTES = 25 * 1024 * 1024;
const MAX_RASTER_DIMENSION = 12_000;
const MAX_RASTER_PIXELS = 12_000_000;
const MAX_RASTER_BYTES = 25 * 1024 * 1024;
const diagnosticsEnabled = new URL(import.meta.url).searchParams.has('onnxDiagnostics');
const MAX_GENERATION_TOKENS = diagnosticsEnabled ? 32 : 4096;
const baseUrl = new URL('./', import.meta.url);
pdfjs.GlobalWorkerOptions.workerSrc = new URL('./pdf.worker.mjs', import.meta.url).href;
const cachePromise = globalThis.caches?.open(MODEL_CACHE_NAME);
let activeAbortController = null;
let processor = null;
let model = null;
let activeDevice = null;
let currentRequestId = null;
let diagnosticRequestId = null;

if (!cachePromise) {
  // Runtime remains explicit: installation reports an unavailable Cache API instead of
  // silently falling back to unverified network downloads or transient model state.
}

const nativeFetch = globalThis.fetch.bind(globalThis);
globalThis.fetch = (input, init) => {
  const url = resolveResourceUrl(input, self.location.href);
  const request = typeof Request !== 'undefined' && input instanceof Request ? input : null;
  const method = String(init?.method ?? request?.method ?? 'GET');
  const body = requestHasBody(input, init) ? {} : null;
  const artifact = artifactForUrl(url.href);
  if (!isAllowedResourceRequest(url, method, body, baseUrl, artifact !== null)) {
    return Promise.reject(new Error('Red bloqueada: las solicitudes de recursos locales y del modelo solo pueden ser GET/HEAD sin cuerpo.'));
  }
  if (artifact && activeAbortController && url.href === modelArtifactUrl(artifact.path)) {
    return nativeFetch(input, { ...init, method, credentials: 'omit', cache: 'no-store' });
  }
  if (url.origin === self.location.origin && url.pathname.startsWith(baseUrl.pathname)) {
    return nativeFetch(input, init);
  }
  return Promise.reject(new Error('Red bloqueada: el Worker solo carga recursos locales y artefactos del modelo fijado.'));
};

const postProgress = (id, progress) => self.postMessage({ id, kind: 'progress', ...progress });
const postResult = (id, result) => self.postMessage({ id, kind: 'result', ...result });
const postError = (id, error) => self.postMessage({
  id,
  kind: 'error',
  message: String(error?.message || error || 'Error sin detalle en el Worker local.').slice(0, 700),
});

const sessionDiagnosticsReady = diagnosticsEnabled
  ? Promise.all([import('onnxruntime-web/webgpu'), import('./onnx-session-diagnostics.mjs')]).then(([onnx, diagnostics]) => {
      diagnostics.instrumentInferenceSessions(
        onnx.InferenceSession,
        () => diagnosticRequestId,
        (id, event) => postProgress(id, event),
      );
    })
  : Promise.resolve();

function assertImageDimensions(width, height) {
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 ||
      width > MAX_RASTER_DIMENSION || height > MAX_RASTER_DIMENSION ||
      width * height > MAX_RASTER_PIXELS) {
    throw new Error('La imagen excede el límite de 12.000 píxeles por dimensión o 12 megapíxeles.');
  }
}

function fromBase64(value) {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

function toBase64(bytes) {
  let result = '';
  const chunk = 0x8000;
  for (let offset = 0; offset < bytes.length; offset += chunk) {
    result += String.fromCharCode(...bytes.subarray(offset, Math.min(offset + chunk, bytes.length)));
  }
  return btoa(result);
}

async function openCache() {
  if (!cachePromise) throw new Error('Este navegador no ofrece Cache API para guardar los artefactos verificados.');
  return await cachePromise;
}

async function verifyCachedArtifact(cache, artifact) {
  const url = modelArtifactUrl(artifact.path);
  const cached = await cache.match(url);
  if (!cached) return false;
  const bytes = new Uint8Array(await cached.arrayBuffer());
  if (!(await verifyArtifactBytes(artifact, bytes))) {
    await cache.delete(url);
    return false;
  }
  return true;
}

async function verifyAllCachedArtifacts(id, progress = false) {
  const cache = await openCache();
  let verified = 0;
  let verifiedBytes = 0;
  for (let index = 0; index < MODEL_ARTIFACTS.length; index++) {
    const artifact = MODEL_ARTIFACTS[index];
    const valid = await verifyCachedArtifact(cache, artifact);
    if (!valid) return { installed: false, totalBytes: verifiedBytes, verifiedArtifacts: verified };
    verified++;
    verifiedBytes += artifact.bytes;
    if (progress) {
      postProgress(id, {
        stage: 'verifying',
        downloadedBytes: verifiedBytes,
        totalBytes: MODEL_TOTAL_BYTES,
        artifactIndex: index + 1,
        artifactCount: MODEL_ARTIFACTS.length,
      });
    }
  }
  return { installed: true, totalBytes: MODEL_TOTAL_BYTES, verifiedArtifacts: verified };
}

async function installModel(id) {
  const cache = await openCache();
  activeAbortController = new AbortController();
  currentRequestId = id;
  let completedBytes = 0;
  try {
    for (let index = 0; index < MODEL_ARTIFACTS.length; index++) {
      const artifact = MODEL_ARTIFACTS[index];
      if (await verifyCachedArtifact(cache, artifact)) {
        completedBytes += artifact.bytes;
        postProgress(id, {
          stage: 'verifying',
          downloadedBytes: completedBytes,
          totalBytes: MODEL_TOTAL_BYTES,
          artifactIndex: index + 1,
          artifactCount: MODEL_ARTIFACTS.length,
        });
        continue;
      }

      const url = modelArtifactUrl(artifact.path);
      const response = await nativeFetch(url, {
        signal: activeAbortController.signal,
        credentials: 'omit',
        cache: 'no-store',
      });
      if (response.status !== 200) {
        throw new Error(`No se pudo descargar ${artifact.path} (HTTP ${response.status}).`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        const bytes = await readVerifiedResponse(response, artifact);
        completedBytes += bytes.byteLength;
        await cache.put(url, new Response(bytes, { headers: { 'content-length': String(bytes.byteLength) } }));
        postProgress(id, {
          stage: 'downloading', downloadedBytes: completedBytes, totalBytes: MODEL_TOTAL_BYTES,
          artifactIndex: index + 1, artifactCount: MODEL_ARTIFACTS.length,
        });
        continue;
      }

      const chunks = [];
      let receivedBytes = 0;
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        if (!value) continue;
        receivedBytes += value.byteLength;
        if (receivedBytes > artifact.bytes) throw new Error(`El archivo ${artifact.path} excedió el tamaño fijado.`);
        chunks.push(value);
        postProgress(id, {
          stage: 'downloading',
          downloadedBytes: completedBytes + receivedBytes,
          totalBytes: MODEL_TOTAL_BYTES,
          artifactIndex: index + 1,
          artifactCount: MODEL_ARTIFACTS.length,
        });
      }
      const bytes = new Uint8Array(receivedBytes);
      let offset = 0;
      for (const chunk of chunks) {
        bytes.set(chunk, offset);
        offset += chunk.byteLength;
      }
      if (!(await verifyArtifactBytes(artifact, bytes))) {
        throw new Error(`El archivo ${artifact.path} no coincide con tamaño o SHA-256 de la revisión fijada.`);
      }
      await cache.put(url, new Response(bytes, { headers: { 'content-length': String(bytes.byteLength) } }));
      completedBytes += bytes.byteLength;
    }

    const status = await verifyAllCachedArtifacts(id, true);
    if (!status.installed) throw new Error('La instalación local quedó incompleta o perdió integridad.');
    postResult(id, status);
  } finally {
    activeAbortController = null;
    currentRequestId = null;
  }
}

async function deleteModel(id) {
  const cache = await openCache();
  for (const artifact of MODEL_ARTIFACTS) await cache.delete(modelArtifactUrl(artifact.path));
  processor = null;
  model = null;
  activeDevice = null;
  postResult(id, { installed: false, totalBytes: 0 });
}

async function configureTransformers() {
  const wasm = env.backends.onnx?.wasm;
  if (!wasm) throw new Error('Transformers.js no expuso el backend ONNX WASM.');
  wasm.numThreads = 1;
  wasm.proxy = false;
  wasm.wasmPaths = {
    mjs: new URL('./ort-wasm-simd-threaded.asyncify.mjs', import.meta.url).href,
    wasm: new URL('./ort-wasm-simd-threaded.asyncify.wasm', import.meta.url).href,
  };
  env.allowLocalModels = false;
  env.allowRemoteModels = true;
  // Transformers.js performs some metadata probes without forwarding the revision
  // option. Pin its default path too, so every lookup resolves to the verified cache.
  env.remotePathTemplate = MODEL_REMOTE_PATH_TEMPLATE;
  env.useBrowserCache = false;
  env.useWasmCache = false;
  env.useFSCache = false;
  env.useCustomCache = true;
  env.cacheKey = MODEL_CACHE_NAME;
  env.logLevel = 40;
  env.fetch = async () => {
    throw new Error('Se bloqueó la descarga automática: instala primero el modelo con acción explícita.');
  };
  env.customCache = {
    async match(request) {
      const artifact = artifactForUrl(request);
      if (!artifact) return undefined;
      const cache = await openCache();
      const url = modelArtifactUrl(artifact.path);
      const response = await cache.match(url);
      if (!response) return undefined;
      const bytes = new Uint8Array(await response.arrayBuffer());
      if (!(await verifyArtifactBytes(artifact, bytes))) {
        await cache.delete(url);
        return undefined;
      }
      return new Response(bytes, {
        headers: { 'content-length': String(bytes.byteLength), 'content-type': 'application/octet-stream' },
      });
    },
    async put(request) {
      throw new Error(`No se permite guardar ${String(request)} fuera del instalador verificado.`);
    },
    async delete(request) {
      const artifact = artifactForUrl(request);
      if (!artifact) return false;
      const cache = await openCache();
      return await cache.delete(modelArtifactUrl(artifact.path));
    },
  };
}

async function ensureModel(id) {
  if (processor && model) return;
  await sessionDiagnosticsReady;
  // The orchestrator has just checked the complete manifest. The custom cache below
  // hashes every file again as Transformers.js reads it, so a changed cache entry
  // cannot reach ONNX Runtime and can never fall back to a network download.
  await configureTransformers();
  postProgress(id, { stage: 'processor' });
  const startedProcessor = performance.now();
  processor = await AutoProcessor.from_pretrained(MODEL_ID, { revision: MODEL_REVISION });
  const processorMs = Math.round(performance.now() - startedProcessor);
  postProgress(id, { stage: 'model', processorMs });

  const gpu = globalThis.navigator?.gpu;
  let adapter = null;
  if (gpu) {
    try {
      adapter = await gpu.requestAdapter();
    } catch {}
  }
  const useWebGpu = Boolean(adapter && adapter.features?.has('shader-f16'));
  const device = useWebGpu ? 'webgpu' : 'wasm';
  const startedModel = performance.now();
  let lastLoadProgressAt = 0;
  const loadedModel = await loadModelWithFallback(
    device,
    selectedDevice => AutoModelForVision2Seq.from_pretrained(MODEL_ID, {
      revision: MODEL_REVISION,
      device: selectedDevice,
      dtype: { embed_tokens: 'quantized', vision_encoder: 'q4f16', decoder_model_merged: 'q4f16' },
      progress_callback: () => {
        const now = performance.now();
        if (now - lastLoadProgressAt >= 1_000) {
          lastLoadProgressAt = now;
          postProgress(id, { stage: 'loading-model', device: selectedDevice });
        }
      },
    }),
    () => postProgress(id, { stage: 'model-fallback', device: 'wasm' }),
  );
  model = loadedModel.model;
  activeDevice = loadedModel.device;
  postProgress(id, {
    stage: 'ready', processorMs, modelLoadMs: Math.round(performance.now() - startedModel), device: activeDevice,
  });
}

async function renderPdfPage(id, request) {
  const pdfBytes = fromBase64(request.bytesBase64);
  if (pdfBytes.byteLength < 5 || pdfBytes.byteLength > MAX_DOCUMENT_BYTES ||
      String.fromCharCode(...pdfBytes.subarray(0, 5)) !== '%PDF-') {
    throw new Error('El PDF está vacío, supera 25 MiB o no tiene una cabecera PDF válida.');
  }
  const loadingTask = pdfjs.getDocument({
    data: pdfBytes,
    isEvalSupported: false,
    disableAutoFetch: true,
    disableStream: true,
    useWorkerFetch: false,
    useSystemFonts: false,
  });
  let document;
  try {
    document = await loadingTask.promise;
    if (document.numPages < 1) throw new Error('El PDF no contiene páginas.');
    const page = await document.getPage(1);
    const viewport = page.getViewport({ scale: 150 / 72 });
    const width = Math.ceil(viewport.width);
    const height = Math.ceil(viewport.height);
    assertImageDimensions(width, height);
    const canvas = new OffscreenCanvas(width, height);
    const context = canvas.getContext('2d', { alpha: false });
    if (!context) throw new Error('El navegador no pudo crear el raster de la página PDF.');
    await page.render({ canvasContext: context, viewport, intent: 'display' }).promise;
    const blob = await canvas.convertToBlob({ type: 'image/png' });
    if (blob.size > MAX_RASTER_BYTES) {
      throw new Error('El raster de la página supera 25 MiB; reduce la resolución o usa una imagen PNG/JPEG más pequeña.');
    }
    const rasterBytes = new Uint8Array(await blob.arrayBuffer());
    postResult(id, {
      pageIndex: 1,
      width,
      height,
      bytesBase64: toBase64(rasterBytes),
    });
  } finally {
    await document?.destroy();
    await loadingTask.destroy();
  }
}

async function readRasterImage(request) {
  if (request.format !== 'PNG' && request.format !== 'JPEG') {
    throw new Error('Selecciona una imagen PNG o JPEG válida.');
  }
  const bytes = fromBase64(request.bytesBase64);
  if (bytes.byteLength < 4 || bytes.byteLength > MAX_DOCUMENT_BYTES) {
    throw new Error('La imagen está vacía o supera 25 MiB.');
  }
  const mimeType = request.format === 'PNG' ? 'image/png' : 'image/jpeg';
  const bitmap = await createImageBitmap(new Blob([bytes], { type: mimeType }));
  try {
    assertImageDimensions(bitmap.width, bitmap.height);
    const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
    const context = canvas.getContext('2d', { alpha: false, willReadFrequently: true });
    if (!context) throw new Error('El navegador no pudo decodificar la imagen seleccionada.');
    context.drawImage(bitmap, 0, 0);
    return await RawImage.read(canvas);
  } finally {
    bitmap.close();
  }
}

async function analyzeImage(id, request) {
  const startedAt = performance.now();
  await ensureModel(id);
  const image = await readRasterImage(request);
  const messages = [{
    role: 'user',
    content: [{ type: 'image' }, { type: 'text', text: 'Convert this page to docling.' }],
  }];
  const prompt = processor.apply_chat_template(messages, { add_generation_prompt: true });
  const inputs = await processor(prompt, [image], { do_image_splitting: true });
  const inputLength = inputs.input_ids.dims.at(-1);
  postProgress(id, { stage: 'generating', promptTokens: inputLength, device: activeDevice });

  let generatedTokens = 0;
  const streamer = new TextStreamer(processor.tokenizer, {
    skip_prompt: true,
    skip_special_tokens: false,
    token_callback_function(tokens) {
      generatedTokens += tokens.length;
      if (generatedTokens % 8 === 0) {
        postProgress(id, { stage: 'generating', generatedTokens, maxGenerationTokens: MAX_GENERATION_TOKENS });
      }
    },
    callback_function() {},
  });
  const generated = await model.generate({ ...inputs, max_new_tokens: MAX_GENERATION_TOKENS, streamer });
  const suffix = generated.slice(null, [inputLength, null]);
  const [docTagsWithSpecialTokens] = processor.batch_decode(suffix, { skip_special_tokens: false });
  const [textWithoutSpecialTokens] = processor.batch_decode(suffix, { skip_special_tokens: true });
  const outputTokens = generated.dims.at(-1) - inputLength;
  postResult(id, {
    docTags: docTagsWithSpecialTokens,
    textWithoutSpecialTokens,
    device: activeDevice,
    promptTokens: inputLength,
    outputTokens,
    maxGenerationTokens: MAX_GENERATION_TOKENS,
    stoppedAtTokenLimit: outputTokens >= MAX_GENERATION_TOKENS,
    processorAndInferenceMs: Math.round(performance.now() - startedAt),
  });
}

self.onmessage = async event => {
  const request = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
  const { id, type } = request ?? {};
  if (type === 'cancel') {
    if (currentRequestId === request.cancelId) activeAbortController?.abort();
    return;
  }
  if (!id) return;
  if (diagnosticsEnabled) diagnosticRequestId = id;
  try {
    if (type === 'status') {
      postResult(id, await verifyAllCachedArtifacts(id, true));
    } else if (type === 'install') {
      await installModel(id);
    } else if (type === 'delete') {
      await deleteModel(id);
    } else if (type === 'rasterize-pdf') {
      await renderPdfPage(id, request);
    } else if (type === 'analyze-image') {
      await analyzeImage(id, request);
    } else {
      throw new Error('Acción del Worker no reconocida.');
    }
  } catch (error) {
    postError(id, error);
  } finally {
    if (diagnosticRequestId === id) diagnosticRequestId = null;
  }
};
