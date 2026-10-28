import { createHash, randomUUID } from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import { once } from 'node:events';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { artifactManifest } from './verify-web.mjs';
import { summarizeVerification } from './verification-status.mjs';
import {
  MODEL_ID,
  MODEL_ORIGIN,
  MODEL_REVISION,
  MODEL_TOTAL_BYTES,
  MODEL_ARTIFACTS,
  artifactForUrl,
  modelArtifactUrl,
} from '../kmp/composeApp/docling-worker/src/model-artifacts.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const distRoot = path.join(root, 'kmp', 'composeApp', 'build', 'dist', 'wasmJs', 'productionExecutable');
const fixtureRoot = path.join(root, 'kmp', 'composeApp', 'browser-tests', 'fixtures', 'docling-synthetic');
const reportRoot = path.join(os.homedir(), '.finsim', 'verification', 'docling');
const profileRoot = path.join(os.tmpdir(), 'finsim-docling-profile');
const runId = `${new Date().toISOString().replaceAll(':', '-')}-${randomUUID()}`;
const reportDirectory = path.join(reportRoot, runId);
const diagnosticMode = process.argv.includes('--diagnostic');
const diagnosticInstall = process.argv.includes('--install-model');
const diagnosticReferenceOnly = process.argv.includes('--reference-only');
const diagnosticPortArg = process.argv.find(argument => /^--port=\d+$/.test(argument));
const diagnosticPort = diagnosticPortArg ? Number(diagnosticPortArg.slice('--port='.length)) : null;
const officialReference = {
  url: 'https://huggingface.co/ibm-granite/granite-docling-258M/resolve/982fe3b40f2fa73c365bdb1bcacf6c81b7184bfe/assets/new_arxiv.png',
  revision: '982fe3b40f2fa73c365bdb1bcacf6c81b7184bfe',
  bytes: 523477,
  sha256: '15e72aca956d9e796788eaa4b0debb9ae988ca7a9637ae3b6df1e6ce671d73d0',
  expectedText: 'Energy Budget of WASP-121 b from JWST/NIRISS Phase Curve',
};
const formats = ['PDF', 'PNG', 'JPEG'];
const criteria = ['DOC-01', 'DOC-02', 'DOC-03', 'DOC-04'];
const report = {
  profile: diagnosticMode ? 'Docling Worker diagnostic; not acceptance' : 'Docling Web real inference',
  command: diagnosticMode
    ? `npm run verify:docling -- --diagnostic${diagnosticInstall ? ' --install-model' : ''}${diagnosticReferenceOnly ? ' --reference-only' : ''}${diagnosticPortArg ? ` ${diagnosticPortArg}` : ''}`
    : 'npm run verify:docling',
  runId,
  startedAt: new Date().toISOString(),
  source: {},
  platform: 'Chrome WebAssembly',
  requirements: ['DOC-01', 'DOC-02', 'DOC-03', 'DOC-04', 'SEC-01', 'VERIFY-01', 'VERIFY-02', 'VERIFY-03'],
  coverage: {
    formats: Object.fromEntries(formats.map(format => [format, 'NOT_RUN'])),
    criteria: Object.fromEntries(criteria.map(id => [id, Object.fromEntries(formats.map(format => [format, 'NOT_RUN']))])),
    ambiguity: 'NOT_RUN',
  },
  fixture: {},
  model: { id: MODEL_ID, revision: MODEL_REVISION, bytes: MODEL_TOTAL_BYTES, artifacts: MODEL_ARTIFACTS.length },
  artifact: {},
  steps: [],
  inference: { status: 'NOT_RUN', progress: [], result: null, error: null },
  diagnostic: null,
  extraction: { status: 'NOT_RUN', expected: {}, actual: {}, mismatches: [] },
  privacy: {
    status: 'NOT_RUN',
    observedChecksStatus: 'NOT_RUN',
    scope: 'Solo observa las solicitudes de esta corrida y almacenes accesibles del perfil aislado; no acredita privacidad global.',
    inspectedStores: ['localStorage', 'sessionStorage', 'Cache Storage (URLs y cuerpos ajenos a artefactos fijados del modelo)', 'IndexedDB', 'cookies del contexto'],
    notInspected: ['cuerpos de artefactos fijados del modelo', 'OPFS', 'memoria de la página', 'otros perfiles o almacenes del navegador'],
    externalRequests: [],
    blockedRequests: [],
    storage: {},
    persistedDocumentMarkers: [],
  },
  scenarioApplication: {
    status: 'NOT_RUN',
    reason: 'El perfil no observa el estado del escenario antes y después de la extracción.',
  },
};

let previewProcess;
let context;

function hash(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

async function loadOfficialReference() {
  const response = await fetch(officialReference.url, { signal: AbortSignal.timeout(30_000) });
  if (!response.ok) throw new Error(`Referencia oficial inaccesible (HTTP ${response.status}).`);
  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length !== officialReference.bytes || hash(bytes) !== officialReference.sha256) {
    throw new Error('La imagen de referencia oficial no coincide con el tamaño y SHA-256 fijados.');
  }
  return bytes;
}

function summarizeDiagnosticResult(result) {
  if (!result) return null;
  const docTags = typeof result.docTags === 'string' ? result.docTags.trim() : '';
  return {
    device: result.device ?? null,
    outputTokens: result.outputTokens ?? null,
    maxGenerationTokens: result.maxGenerationTokens ?? null,
    stoppedAtTokenLimit: result.stoppedAtTokenLimit === true,
    docTagsLength: docTags.length,
    startsWithDocTags: docTags.startsWith('<doctag>'),
    firstDocTag: docTags.match(/^<doctag><([^>]+)>/)?.[1] ?? null,
    punctuationOnly: docTags.length > 0 && /^[!\s]+$/.test(docTags),
    containsOfficialText: docTags.includes(officialReference.expectedText),
    processorAndInferenceMs: result.processorAndInferenceMs ?? null,
  };
}

function record(id, status, actual, reason) {
  const step = { id, status, ...(actual !== undefined && actual !== null ? { actual } : {}), ...(reason ? { reason } : {}) };
  const index = report.steps.findIndex(existing => existing.id === id);
  if (index === -1) report.steps.push(step);
  else report.steps[index] = step;
  const match = id.match(/^(DOC-0[1-4])-(PDF|PNG|JPEG)$/);
  if (match) {
    report.coverage.criteria[match[1]][match[2]] = status;
    if (match[2] === 'PDF') {
      const pdfStatuses = criteria.map(criterion => report.coverage.criteria[criterion].PDF);
      report.coverage.formats.PDF = pdfStatuses.includes('FAIL') ? 'FAIL' : pdfStatuses.every(value => value === 'PASS') ? 'PASS' : 'NOT_RUN';
    }
  }
}

for (const criterion of criteria) {
  for (const format of formats) {
    record(`${criterion}-${format}`, 'NOT_RUN', null,
      format === 'PDF' ? 'Pendiente de evidencia de este perfil.' : 'Este perfil solo selecciona y analiza PDF.');
  }
}
record('DOC-AMBIGUITY', 'NOT_RUN', null, 'El PDF sintético no prueba filas ambiguas ni conflictos de interpretación.');
record('SEC-01', 'NOT_RUN', null, 'La privacidad global no se acredita con una corrida ni con un subconjunto de almacenes.');
record('SEC-01-SCOPED', 'NOT_RUN', null, 'Pendiente de revisar solicitudes y almacenes del perfil aislado.');

function git(args) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8' }).trim();
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function freePort() {
  const server = net.createServer();
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const port = server.address().port;
  await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  return port;
}

async function startPreview() {
  const port = diagnosticPort ?? await freePort();
  const url = `http://127.0.0.1:${port}/`;
  const viteCli = path.join(root, 'node_modules', 'vite', 'bin', 'vite.js');
  previewProcess = spawn(process.execPath, [
    viteCli, 'preview', '--host', '127.0.0.1', '--port', String(port), '--strictPort', '--outDir', distRoot,
  ], { cwd: root, windowsHide: true, stdio: 'ignore' });
  for (let attempt = 0; attempt < 60; attempt += 1) {
    if (previewProcess.exitCode !== null) throw new Error('Vite preview terminó antes de responder.');
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(1_000) });
      if (response.ok) return url;
    } catch {}
    await delay(500);
  }
  throw new Error('Vite preview no respondió dentro de 30 segundos.');
}

async function verifyServedArtifact(manifest, baseUrl) {
  const mismatches = [];
  for (const entry of manifest.entries) {
    const relativeUrl = entry.path.split('/').map(encodeURIComponent).join('/');
    try {
      const response = await fetch(new URL(relativeUrl, baseUrl), { signal: AbortSignal.timeout(15_000) });
      if (!response.ok) {
        mismatches.push(`${entry.path}: HTTP ${response.status}`);
        continue;
      }
      const actual = hash(Buffer.from(await response.arrayBuffer()));
      if (actual !== entry.sha256) mismatches.push(`${entry.path}: hash servido distinto al build.`);
    } catch (error) {
      mismatches.push(`${entry.path}: ${error instanceof Error ? error.message : String(error)}`);
    }
  }
  return mismatches;
}

async function clickComposeControl(page, locator) {
  await locator.waitFor({ state: 'visible', timeout: 30_000 });
  let box = null;
  for (let sample = 0; sample < 5; sample += 1) {
    const first = await locator.boundingBox();
    await page.waitForTimeout(100);
    const second = await locator.boundingBox();
    if (first && second && Math.abs(first.x - second.x) <= 2 && Math.abs(first.y - second.y) <= 2 &&
        Math.abs(first.width - second.width) <= 2 && Math.abs(first.height - second.height) <= 2) {
      box = second;
      break;
    }
  }
  if (!box) throw new Error('El control de Compose no tuvo límites estables para la interacción.');
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(250);
}

function valueOnLine(text, label) {
  return text.split(/\r?\n/).map(line => line.trim()).find(line => line.startsWith(label))?.slice(label.length).trim() ?? null;
}

export function assessDoc01Evidence(result, reviewVisible) {
  const inferenceStatus = result?.docTags?.trim() && !result.stoppedAtTokenLimit ? 'PASS' : 'FAIL';
  const reviewStatus = result ? (reviewVisible ? 'PASS' : 'FAIL') : 'NOT_RUN';
  const criterionStatus = inferenceStatus === 'FAIL' || reviewStatus === 'FAIL'
    ? 'FAIL'
    : inferenceStatus === 'PASS' && reviewStatus === 'PASS' ? 'PASS' : 'NOT_RUN';
  return { inferenceStatus, reviewStatus, criterionStatus };
}

export function assessDoc04Evidence({ reviewVisible, inferenceComplete, scenarioApplication } = {}) {
  const reviewStatus = reviewVisible ? 'PASS' : inferenceComplete ? 'FAIL' : 'NOT_RUN';
  const applicationStatus = scenarioApplication === 'UNCHANGED' ? 'PASS'
    : scenarioApplication === 'APPLIED' ? 'FAIL' : 'NOT_RUN';
  const criterionStatus = reviewStatus === 'FAIL' || applicationStatus === 'FAIL' ? 'FAIL'
    : reviewStatus === 'PASS' && applicationStatus === 'PASS' ? 'PASS' : 'NOT_RUN';
  return { reviewStatus, applicationStatus, criterionStatus };
}

// Compose exposes page text in an open shadow root, which body.innerText omits.
export async function readAccessiblePageText(page) {
  return [...new Set((await page.locator('body *').allInnerTexts()).filter(Boolean))].join('\n');
}

async function waitForAccessiblePageText(page, predicate, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const text = await readAccessiblePageText(page);
    if (predicate(text)) return text;
    await page.waitForTimeout(Math.min(250, deadline - Date.now()));
  }
  throw new Error(`No se encontró el texto accesible esperado en ${timeoutMs} ms.`);
}

const artifactPaths = new Set(MODEL_ARTIFACTS.map(artifact => artifact.path));
// shortcut: permite el CDN us observado; ampliar la allowlist si Hugging Face cambia la región de descarga.
const modelRedirectOrigins = new Set([MODEL_ORIGIN, 'https://us.aws.cdn.hf.co']);

function knownModelRedirect(url) {
  if (!modelRedirectOrigins.has(url.origin)) return false;
  if (url.origin === MODEL_ORIGIN) {
    const prefix = `/api/resolve-cache/models/${MODEL_ID}/${MODEL_REVISION}/`;
    return artifactPaths.has(url.pathname.slice(prefix.length));
  }
  return /^\/xet-bridge-us\/[^/]+\/[a-f0-9]+$/i.test(url.pathname);
}

export function isAllowedNetworkRequest({ url: input, method, hasBody = false, redirectedFrom = [] }, appOrigin) {
  let url;
  try { url = new URL(input); } catch { return false; }
  if (url.origin === appOrigin) return true;
  if (!['GET', 'HEAD'].includes(method) || hasBody) return false;
  if (artifactForUrl(url) && !url.search && !url.hash) return true;
  return redirectedFrom.some(source => {
    try { return Boolean(artifactForUrl(new URL(source))); } catch { return false; }
  }) && knownModelRedirect(url);
}

export async function inspectBrowserStorage({ markers, pdfBytes, cookieValues, modelArtifactUrls = [] }) {
  const encoder = new TextEncoder();
  const modelArtifactUrlSet = new Set(modelArtifactUrls);
  const markerBytes = markers.map(marker => ({ id: marker.id, bytes: encoder.encode(marker.value) }));
  markerBytes.push({ id: 'pdf-bytes', bytes: Uint8Array.from(pdfBytes) });
  const maxMarkerLength = Math.max(...markerBytes.map(marker => marker.bytes.length));
  const found = new Set();
  const locations = new Map();
  const storage = {};
  const errors = [];

  const scanBytes = (bytes, location) => {
    const hits = [];
    for (const marker of markerBytes) {
      if (found.has(marker.id)) continue;
      let offset = bytes.indexOf(marker.bytes[0]);
      while (offset !== -1) {
        let index = 1;
        while (index < marker.bytes.length && bytes[offset + index] === marker.bytes[index]) index += 1;
        if (index === marker.bytes.length) {
          found.add(marker.id);
          locations.set(marker.id, location);
          hits.push(marker.id);
          break;
        }
        offset = bytes.indexOf(marker.bytes[0], offset + 1);
      }
    }
    return hits;
  };
  const scanText = (value, location) => scanBytes(encoder.encode(String(value)), location);
  const scanValue = async (value, location, seen = new WeakSet()) => {
    if (typeof value === 'string') { scanText(value, location); return; }
    if (value instanceof Blob) { await scanValue(new Uint8Array(await value.arrayBuffer()), location, seen); return; }
    if (value instanceof ArrayBuffer) { scanBytes(new Uint8Array(value), location); return; }
    if (ArrayBuffer.isView(value)) { scanBytes(new Uint8Array(value.buffer, value.byteOffset, value.byteLength), location); return; }
    if (!value || typeof value !== 'object' || seen.has(value)) return;
    seen.add(value);
    for (const [key, child] of Object.entries(value)) {
      scanText(key, location);
      await scanValue(child, location, seen);
    }
  };
  const scanResponse = async (response, location) => {
    if (!response) return false;
    if (response.type === 'opaque') return false;
    if (!response.body) return [204, 205, 304].includes(response.status);
    const reader = response.body.getReader();
    let tail = new Uint8Array();
    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        const combined = new Uint8Array(tail.length + value.length);
        combined.set(tail);
        combined.set(value, tail.length);
        scanBytes(combined, location);
        if (found.size === markerBytes.length) {
          await reader.cancel();
          return true;
        }
        tail = combined.slice(Math.max(0, combined.length - maxMarkerLength + 1));
      }
      scanBytes(tail, location);
      return true;
    } finally {
      reader.releaseLock();
    }
  };

  for (const [storeName, storageName] of [['localStorage', 'localStorage'], ['sessionStorage', 'sessionStorage']]) {
    try {
      const store = globalThis[storageName];
      for (let index = 0; index < store.length; index += 1) {
        const key = store.key(index);
        scanText(key, storeName);
        scanText(store.getItem(key), storeName);
      }
      storage[storeName] = 'INSPECTED';
    } catch (error) {
      storage[storeName] = 'UNAVAILABLE';
      errors.push(`${storeName}: ${error instanceof Error ? error.message : String(error)}`);
    }
  }

  try {
    let entries = 0;
    let modelArtifactBodiesSkipped = 0;
    for (const cacheName of await caches.keys()) {
      scanText(cacheName, 'Cache Storage cache name');
      const cache = await caches.open(cacheName);
      for (const request of await cache.keys()) {
        entries += 1;
        scanText(request.url, 'Cache Storage request URL');
        if (modelArtifactUrlSet.has(request.url)) {
          modelArtifactBodiesSkipped += 1;
          continue;
        }
        if (!(await scanResponse(await cache.match(request), 'Cache Storage response body'))) {
          throw new Error('Un cuerpo de Cache Storage no fue legible.');
        }
      }
    }
    storage.cacheStorage = { status: 'INSPECTED', entries, modelArtifactBodiesSkipped };
  } catch (error) {
    storage.cacheStorage = { status: 'UNAVAILABLE' };
    errors.push(`Cache Storage: ${error instanceof Error ? error.message : String(error)}`);
  }

  try {
    if (typeof indexedDB.databases !== 'function') throw new Error('El navegador no expone indexedDB.databases().');
    const databases = await indexedDB.databases();
    let records = 0;
    for (const entry of databases) {
      if (!entry.name) continue;
      scanText(entry.name, 'IndexedDB database name');
      const database = await new Promise((resolve, reject) => {
        const request = indexedDB.open(entry.name);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error ?? new Error('No se pudo abrir IndexedDB.'));
      });
      try {
        for (const storeName of Array.from(database.objectStoreNames)) {
          scanText(storeName, 'IndexedDB object store name');
          await new Promise((resolve, reject) => {
            const request = database.transaction(storeName, 'readonly').objectStore(storeName).openCursor();
            const scans = [];
            request.onsuccess = async () => {
              try {
                const cursor = request.result;
                if (!cursor) { await Promise.all(scans); resolve(); return; }
                records += 1;
                scans.push(scanValue(cursor.key, 'IndexedDB'), scanValue(cursor.value, 'IndexedDB'));
                cursor.continue();
              } catch (error) { reject(error); }
            };
            request.onerror = () => reject(request.error ?? new Error('No se pudo leer IndexedDB.'));
          });
        }
      } finally { database.close(); }
    }
    storage.indexedDB = { status: 'INSPECTED', databases: databases.length, records };
  } catch (error) {
    storage.indexedDB = { status: 'UNAVAILABLE' };
    errors.push(`IndexedDB: ${error instanceof Error ? error.message : String(error)}`);
  }

  try {
    for (const value of cookieValues) scanText(value, 'cookies');
    storage.cookies = 'INSPECTED';
  } catch (error) {
    storage.cookies = 'UNAVAILABLE';
    errors.push(`Cookies: ${error instanceof Error ? error.message : String(error)}`);
  }

  return {
    storage,
    errors,
    markerIds: [...found],
    markerLocations: [...locations].map(([id, store]) => ({ id, store })),
  };
}

async function inspectStorage(page, markers, pdfBytes, cookies) {
  return page.evaluate(inspectBrowserStorage, {
    markers,
    pdfBytes: [...pdfBytes],
    cookieValues: cookies.flatMap(cookie => [cookie.name, cookie.value]),
    modelArtifactUrls: MODEL_ARTIFACTS.map(artifact => modelArtifactUrl(artifact.path)),
  });
}

async function run() {
  await mkdir(reportDirectory, { recursive: true });
  try {
    report.source = {
      branch: git(['branch', '--show-current']) || 'DETACHED',
      commit: git(['rev-parse', 'HEAD']),
      workingTree: git(['status', '--porcelain']),
    };

    const expected = JSON.parse(await readFile(path.join(fixtureRoot, 'expected.json'), 'utf8'));
    const pdfPath = path.join(fixtureRoot, 'synthetic-loan-cop-01.pdf');
    const pdfBytes = await readFile(pdfPath);
    const pdfSha256 = hash(pdfBytes);
    report.fixture = { name: path.basename(pdfPath), synthetic: expected.synthetic, pdfSha256 };
    report.extraction.expected = expected.expectedBeforeCorrection;
    if (expected.synthetic !== true || expected.humanPrivateReference !== false || pdfSha256 !== expected.pdfSha256) {
      record('DOC-FIXTURE', 'FAIL', null, 'La fixture no pasó su identidad sintética independiente.');
      return;
    }
    record('DOC-FIXTURE', 'PASS', 'Se usó únicamente el PDF sintético del repositorio; hash independiente coincidente.');

    const referenceBytes = diagnosticMode ? await loadOfficialReference() : null;

    try {
      const distIndex = await stat(path.join(distRoot, 'index.html'));
      if (!distIndex.isFile()) throw new Error('No existe index.html de producción.');
    } catch {
      record('DOC-DISTRIBUTION', 'BLOCKED', null, 'Primero genera la distribución con `npm run build`.');
      return;
    }
    const manifest = await artifactManifest(distRoot);
    report.artifact = { sha256: manifest.sha256, files: manifest.files, rebuiltByThisProfile: false };
    const url = await startPreview();
    report.artifact.url = url;
    const mismatches = await verifyServedArtifact(manifest, url);
    if (mismatches.length) {
      record('DOC-ARTIFACT', 'FAIL', null, mismatches.slice(0, 10).join(' '));
      return;
    }
    record('DOC-ARTIFACT', 'PASS', `${manifest.files} archivos servidos coinciden con el árbol SHA-256 ${manifest.sha256}.`);

    const { chromium } = await import('@playwright/test');
    try {
      context = await chromium.launchPersistentContext(profileRoot, {
        channel: 'chrome', headless: true, viewport: { width: 1280, height: 1000 }, serviceWorkers: 'block',
        ...(diagnosticMode ? { args: ['--disable-gpu'] } : {}),
      });
    } catch (error) {
      record('DOC-CHROME', 'BLOCKED', null, error instanceof Error ? error.message : String(error));
      return;
    }
    report.browser = { version: context.browser()?.version() ?? null };
    const page = context.pages()[0] ?? await context.newPage();
    const origin = new URL(url).origin;
    page.on('pageerror', error => { report.pageErrors ??= []; report.pageErrors.push(error.message); });
    await context.route('**/*', async route => {
      const request = route.request();
      const url = request.url();
      const redirectedFrom = [];
      for (let previous = request.redirectedFrom(); previous; previous = previous.redirectedFrom()) {
        redirectedFrom.push(previous.url());
      }
      const body = request.postDataBuffer?.() ?? request.postData?.();
      const hasBody = body !== null && body !== undefined && body.length !== 0;
      const allowed = isAllowedNetworkRequest({ url, method: request.method(), hasBody, redirectedFrom }, origin);
      let observation;
      try {
        const parsed = new URL(url);
        observation = { origin: parsed.origin, method: request.method(), hasBody, hasQuery: parsed.search.length > 0, allowed };
        if (parsed.origin !== origin) report.privacy.externalRequests.push(observation);
      } catch {
        observation = { origin: 'invalid-url', method: request.method(), hasBody, hasQuery: url.includes('?'), allowed: false };
        report.privacy.externalRequests.push(observation);
      }
      if (!allowed) {
        report.privacy.blockedRequests.push(observation);
        await route.abort('blockedbyclient');
      } else {
        await route.continue();
      }
    });
    await page.addInitScript(diagnostic => {
      window.__doclingProfile = { progress: [], result: null, results: {}, errors: {}, onnx: [], modelStatus: null, worker: null };
      const OriginalWorker = window.Worker;
      window.Worker = new Proxy(OriginalWorker, {
        construct(target, args, newTarget) {
          const workerArgs = [...args];
          const isDoclingWorker = String(workerArgs[0]).includes('docling/worker.mjs');
          if (diagnostic && isDoclingWorker) {
            const workerUrl = new URL(String(workerArgs[0]), document.baseURI);
            workerUrl.searchParams.set('onnxDiagnostics', '1');
            workerArgs[0] = workerUrl.href;
          }
          const worker = Reflect.construct(target, workerArgs, newTarget);
          if (isDoclingWorker) {
            window.__doclingProfile.worker = worker;
            worker.addEventListener('message', event => {
              let message;
              try { message = typeof event.data === 'string' ? JSON.parse(event.data) : event.data; } catch { return; }
              if (message?.kind === 'progress') {
                const progress = { stage: message.stage ?? null, device: message.device ?? null };
                const previous = window.__doclingProfile.progress.at(-1);
                if (!diagnostic || previous?.stage !== progress.stage || previous?.device !== progress.device) {
                  window.__doclingProfile.progress.push(progress);
                }
                if (String(message.stage).startsWith('onnx-')) {
                  const { id, stage, sessionIndex, providers, component, runIndex, durationMs, errorName } = message;
                  window.__doclingProfile.onnx.push({ id, stage, sessionIndex, providers, component, runIndex, durationMs, errorName });
                }
              }
              if (message?.kind === 'result') {
                if (message.installed !== undefined) {
                  window.__doclingProfile.modelStatus = {
                    installed: message.installed === true,
                    verifiedArtifacts: message.verifiedArtifacts ?? null,
                    totalBytes: message.totalBytes ?? null,
                  };
                }
                if (message.docTags) {
                  const result = {
                    id: message.id,
                    docTags: message.docTags,
                    device: message.device ?? null,
                    outputTokens: message.outputTokens ?? null,
                    maxGenerationTokens: message.maxGenerationTokens ?? null,
                    stoppedAtTokenLimit: message.stoppedAtTokenLimit === true,
                    processorMs: message.processorMs ?? null,
                    modelLoadMs: message.modelLoadMs ?? null,
                    processorAndInferenceMs: message.processorAndInferenceMs ?? null,
                  };
                  window.__doclingProfile.results[message.id] = result;
                  if (message.id !== 'official-reference-control') window.__doclingProfile.result = result;
                }
              }
              if (message?.kind === 'error') {
                const error = { id: message.id, message: String(message.message ?? 'Worker error').slice(0, 240) };
                window.__doclingProfile.errors[message.id] = error.message;
                window.__doclingProfile.lastError = error;
              }
            });
          }
          return worker;
        },
      });
    }, diagnosticMode);
    await page.goto(url);
    await clickComposeControl(page, page.getByRole('button', { name: 'Importar extracto', exact: true }).first());
    let bodyText = await waitForAccessiblePageText(page, text =>
      text.includes('Instalar modelo local') || text.includes('✓ Instalado y verificado') || text.includes('No se pudo verificar Granite-Docling'),
    diagnosticMode ? 30_000 : 180_000);

    if (!bodyText.includes('✓ Instalado y verificado')) {
      if (diagnosticMode && !diagnosticInstall) {
        report.diagnostic = { verdict: 'BLOCKED_MODEL_NOT_INSTALLED', modelStatus: null };
        record('DOC-MODEL-DIAGNOSTIC', 'BLOCKED', null, 'La sonda requiere artefactos ya instalados y verificados; usa --install-model para ejecutar la acción explícita de descarga.');
        return;
      }
      if (!bodyText.includes('Instalar modelo local')) {
        record('DOC-MODEL', 'BLOCKED', null, bodyText.match(/No se pudo verificar Granite-Docling[^\n]*/) ?? 'No se pudo establecer estado del modelo.');
        return;
      }
      report.model.installStarted = true;
      await clickComposeControl(page, page.getByRole('button', { name: /Instalar modelo local/ }));
      try {
        bodyText = await waitForAccessiblePageText(page, text =>
          text.includes('✓ Instalado y verificado') || text.includes('No se pudo verificar Granite-Docling'),
        15 * 60_000);
      } catch (error) {
        record('DOC-MODEL-INSTALL', 'BLOCKED', null, error instanceof Error ? error.message : String(error));
        return;
      }
      if (!bodyText.includes('✓ Instalado y verificado')) {
        record('DOC-MODEL-INSTALL', 'BLOCKED', null, bodyText.match(/No se pudo verificar Granite-Docling[^\n]*/) ?? 'No se completó la instalación verificada.');
        return;
      }
      record('DOC-MODEL-INSTALL', 'PASS', `${MODEL_ARTIFACTS.length} archivos y ${MODEL_TOTAL_BYTES} bytes verificados por el Worker.`);
    } else {
      record('DOC-MODEL', 'PASS', `${MODEL_ARTIFACTS.length} archivos y ${MODEL_TOTAL_BYTES} bytes ya verificados por el Worker.`);
    }

    if (diagnosticMode && diagnosticReferenceOnly) {
      const referenceId = 'official-reference-control';
      await page.evaluate(({ id, bytesBase64 }) => {
        window.__doclingProfile.worker.postMessage({ id, type: 'analyze-image', format: 'PNG', bytesBase64 });
      }, { id: referenceId, bytesBase64: referenceBytes.toString('base64') });
      try {
        await page.waitForFunction(id =>
          Boolean(window.__doclingProfile.results[id] || window.__doclingProfile.errors[id]),
        referenceId, { timeout: 60_000 });
      } catch (error) {
        report.inference.error = error instanceof Error ? error.message : String(error);
      }
      const referenceEvidence = await page.evaluate(id => ({
        result: window.__doclingProfile.results[id] ?? null,
        error: window.__doclingProfile.errors[id] ?? null,
        modelStatus: window.__doclingProfile.modelStatus,
        stages: window.__doclingProfile.progress,
        onnxEvents: window.__doclingProfile.onnx,
      }), referenceId);
      report.diagnostic = {
        mode: '32-token WASM official-reference control; not acceptance evidence',
        input: 'pinned public model-card image; direct PNG to the production Worker',
        modelStatus: referenceEvidence.modelStatus,
        stages: referenceEvidence.stages,
        onnxEvents: referenceEvidence.onnxEvents,
        synthetic: { status: 'NOT_RUN' },
        officialReference: {
          source: 'onnx-community model card example; original IBM Granite image pinned to its repository revision',
          revision: officialReference.revision,
          bytes: referenceBytes.byteLength,
          sha256: hash(referenceBytes),
          expectedText: officialReference.expectedText,
          result: summarizeDiagnosticResult(referenceEvidence.result),
          error: referenceEvidence.error ?? report.inference.error ?? null,
          status: referenceEvidence.result?.docTags?.includes(officialReference.expectedText)
            ? 'MATCHED_PUBLISHED_EXAMPLE'
            : referenceEvidence.result?.stoppedAtTokenLimit ? 'PREFIX_DID_NOT_REACH_PUBLISHED_TEXT'
              : referenceEvidence.result ? 'MISMATCH' : 'INCONCLUSIVE_AT_CUTOFF',
        },
      };
      report.diagnostic.verdict = report.diagnostic.officialReference.status === 'MATCHED_PUBLISHED_EXAMPLE'
        ? 'OFFICIAL_CONTROL_MATCHED'
        : report.diagnostic.officialReference.status === 'INCONCLUSIVE_AT_CUTOFF'
          ? 'INCONCLUSIVE_OFFICIAL_NO_RESULT_AT_CUTOFF'
          : report.diagnostic.officialReference.status === 'PREFIX_DID_NOT_REACH_PUBLISHED_TEXT'
            ? 'INCONCLUSIVE_OFFICIAL_PREFIX' : 'OFFICIAL_CONTROL_DID_NOT_MATCH';
      return;
    }

    const chooserPromise = page.waitForEvent('filechooser', { timeout: 30_000 });
    await clickComposeControl(page, page.getByRole('button', { name: 'Seleccionar PDF, PNG o JPEG', exact: true }));
    const chooser = await chooserPromise;
    await chooser.setFiles({ name: report.fixture.name, mimeType: 'application/pdf', buffer: pdfBytes });
    await clickComposeControl(page, page.getByRole('button', { name: 'Analizar página 1 con Granite-Docling', exact: true }));
    try {
      bodyText = await waitForAccessiblePageText(page, text =>
        text.includes('Revisión de extracción') || text.includes('Error al analizar el documento visual'),
      diagnosticMode ? 60_000 : 240_000);
    } catch (error) {
      report.inference.error = error instanceof Error ? error.message : String(error);
    }

    const workerEvidence = await page.evaluate(() => {
      const { worker, ...evidence } = window.__doclingProfile;
      return evidence;
    });
    const hasProfileWorker = diagnosticMode ? await page.evaluate(() => Boolean(window.__doclingProfile.worker)) : false;
    if (diagnosticMode) {
      const syntheticResult = workerEvidence?.result ?? null;
      report.diagnostic = {
        mode: '32-token WASM probe; not acceptance evidence',
        input: 'synthetic PDF, first page rasterized by the production Worker',
        modelStatus: workerEvidence?.modelStatus ?? null,
        stages: workerEvidence?.progress ?? [],
        onnxEvents: workerEvidence?.onnx ?? [],
        synthetic: {
          result: summarizeDiagnosticResult(syntheticResult),
          error: workerEvidence?.lastError?.message ?? report.inference.error ?? null,
        },
        officialReference: {
          source: 'onnx-community model card example; original IBM Granite image pinned to its repository revision',
          revision: officialReference.revision,
          bytes: referenceBytes.byteLength,
          sha256: hash(referenceBytes),
          expectedText: officialReference.expectedText,
          status: 'NOT_RUN',
        },
      };

      if (syntheticResult && hasProfileWorker) {
        const referenceId = 'official-reference-control';
        await page.evaluate(({ id, bytesBase64 }) => {
          window.__doclingProfile.worker.postMessage({ id, type: 'analyze-image', format: 'PNG', bytesBase64 });
        }, { id: referenceId, bytesBase64: referenceBytes.toString('base64') });
        try {
          await page.waitForFunction(id =>
            Boolean(window.__doclingProfile.results[id] || window.__doclingProfile.errors[id]),
          referenceId, { timeout: 60_000 });
          const referenceResult = await page.evaluate(id => window.__doclingProfile.results[id] ?? null, referenceId);
          report.diagnostic.officialReference.result = summarizeDiagnosticResult(referenceResult);
          report.diagnostic.officialReference.status = report.diagnostic.officialReference.result?.containsOfficialText
            ? 'MATCHED_PUBLISHED_EXAMPLE' : referenceResult?.stoppedAtTokenLimit ? 'PREFIX_DID_NOT_REACH_PUBLISHED_TEXT' : 'MISMATCH';
          report.diagnostic.officialReference.error = await page.evaluate(id => window.__doclingProfile.errors[id] ?? null, referenceId);
          report.diagnostic.onnxEvents = await page.evaluate(() => window.__doclingProfile.onnx);
        } catch (error) {
          report.diagnostic.officialReference.status = 'INCONCLUSIVE_AT_CUTOFF';
          report.diagnostic.officialReference.error = error instanceof Error ? error.message : String(error);
        }
      }

      const synthetic = report.diagnostic.synthetic.result;
      report.diagnostic.verdict = !synthetic
        ? 'INCONCLUSIVE_NO_RESULT_AT_CUTOFF'
        : synthetic.punctuationOnly
          ? report.diagnostic.officialReference.status === 'MATCHED_PUBLISHED_EXAMPLE'
            ? 'REPRODUCED_SYNTHETIC_ONLY_OFFICIAL_CONTROL_MATCHED'
            : 'REPRODUCED_PUNCTUATION_OUTPUT'
          : 'PUNCTUATION_SYMPTOM_NOT_REPRODUCED_IN_32_TOKEN_PROBE';
      return;
    }
    report.inference.progress = workerEvidence?.progress ?? [];
    report.inference.result = workerEvidence?.result ?? null;
    bodyText = await readAccessiblePageText(page);
    const reviewVisible = bodyText.includes('Revisión de extracción');
    const doc01Evidence = assessDoc01Evidence(report.inference.result, reviewVisible);
    report.inference.status = doc01Evidence.inferenceStatus;
    if (!report.inference.result) {
      report.inference.error ??= bodyText.match(/Error al analizar el documento visual[^\n]*/) ?? 'No llegó una respuesta real del Worker.';
    } else if (report.inference.result.stoppedAtTokenLimit) {
      report.inference.error = 'El modelo real llegó al límite de tokens; la salida se conserva como incompleta.';
    }
    const doc01Reason = doc01Evidence.inferenceStatus !== 'PASS' ? report.inference.error ?? 'No se recibió una salida DocTags completa.'
      : doc01Evidence.reviewStatus !== 'PASS' ? 'DocTags recibidos, pero no se observó la pantalla de revisión.' : null;
    record('DOC-01-INFERENCE-PDF', doc01Evidence.inferenceStatus,
      doc01Evidence.inferenceStatus === 'PASS' ? `DocTags reales recibidos desde Granite-Docling (${report.inference.result.device ?? 'dispositivo no informado'}).` : null,
      doc01Evidence.inferenceStatus === 'PASS' ? null : doc01Reason);
    record('DOC-01-REVIEW-PDF', doc01Evidence.reviewStatus,
      doc01Evidence.reviewStatus === 'PASS' ? 'La pantalla de revisión de extracción fue observada.' : null,
      doc01Evidence.reviewStatus === 'FAIL' ? 'No se observó la pantalla de revisión tras recibir DocTags.' : undefined);
    record('DOC-01-PDF', doc01Evidence.criterionStatus,
      doc01Evidence.criterionStatus === 'PASS' ? 'Inferencia completa y pantalla de revisión observadas.' : null,
      doc01Reason);

    const doc04Evidence = assessDoc04Evidence({ reviewVisible, inferenceComplete: doc01Evidence.inferenceStatus === 'PASS' });
    report.scenarioApplication = {
      status: doc04Evidence.applicationStatus,
      reason: 'El perfil no compara un estado independiente del escenario antes y después de la extracción.',
    };
    record('DOC-04-REVIEW-PDF', doc04Evidence.reviewStatus,
      reviewVisible ? 'La pantalla de revisión de extracción fue observada.' : null,
      reviewVisible ? null : 'No se observó la pantalla de revisión.');
    record('DOC-04-APPLICATION-PDF', doc04Evidence.applicationStatus, null, report.scenarioApplication.reason);
    record('DOC-04-PDF', doc04Evidence.criterionStatus, null,
      doc04Evidence.criterionStatus === 'NOT_RUN'
        ? 'Se observó la revisión, pero falta evidencia independiente del estado del escenario.'
        : doc04Evidence.criterionStatus === 'FAIL' ? 'No se confirmó la secuencia de revisión previa a la aplicación.' : undefined);

    if (reviewVisible && report.inference.result) {
      const expectedFields = [
        ['originalPrincipal', 'Capital original:', expected.expectedBeforeCorrection.originalPrincipal],
        ['capitalBalance', 'Saldo de capital:', expected.expectedBeforeCorrection.capitalBalance],
        ['originalTermMonths', 'Plazo original:', `${expected.expectedBeforeCorrection.originalTermMonths} meses`],
        ['remainingTermMonths', 'Plazo restante:', `${expected.expectedBeforeCorrection.remainingTermMonths} meses`],
        ['basePayment', 'Cuota base:', expected.expectedBeforeCorrection.basePayment],
        ['totalPayment', 'Cuota total:', expected.expectedBeforeCorrection.totalPayment],
        ['rate', 'Tasa:', `${expected.expectedBeforeCorrection.ratePercentage}% · ${expected.expectedBeforeCorrection.rateKind}`],
        ['totalInsurance', 'Seguro cobrado:', expected.expectedBeforeCorrection.insurancePaid],
      ];
      const extractedFields = Object.fromEntries(expectedFields.map(([key, label]) => [key, valueOnLine(bodyText, label)]));
      report.extraction.actual = extractedFields;
      report.extraction.mismatches = expectedFields
        .filter(([key, , value]) => extractedFields[key] !== value)
        .map(([key, , value]) => ({ field: key, expected: value, actual: extractedFields[key] }));
      const insuranceMeaning = bodyText.includes('Seguro de vida') && bodyText.includes('periodicidad mensual');
      const fieldsStatus = report.extraction.mismatches.length === 0 ? 'PASS' : 'FAIL';
      const insuranceStatus = insuranceMeaning ? 'PASS' : 'FAIL';
      record(
        'DOC-02-PDF', fieldsStatus,
        `${expectedFields.length - report.extraction.mismatches.length}/${expectedFields.length} campos del PDF sintético coinciden.`,
        report.extraction.mismatches.length ? JSON.stringify(report.extraction.mismatches) : null
      );
      record('DOC-03-PDF', insuranceStatus, insuranceMeaning ? 'Significado y periodicidad mensual del seguro visibles.' : null,
        insuranceMeaning ? null : 'Faltó confirmar significado y periodicidad mensual del seguro.');
      report.extraction.status = [fieldsStatus, insuranceStatus].includes('FAIL') ? 'FAIL' : 'PASS';
    }

    const pdfBase64Marker = pdfBytes.toString('base64').slice(0, 128);
    const storageScan = await inspectStorage(page, [
      { id: 'fixture-name', value: report.fixture.name },
      { id: 'fixture-sha256', value: report.fixture.pdfSha256 },
      { id: 'fixture-base64-prefix', value: pdfBase64Marker },
      { id: 'fixture-title', value: 'Finsim - extracto sintetico COP 01' },
      { id: 'fixture-id', value: expected.id },
      { id: 'original-principal', value: expected.expectedBeforeCorrection.originalPrincipal },
      { id: 'capital-balance', value: expected.expectedBeforeCorrection.capitalBalance },
      { id: 'base-payment', value: expected.expectedBeforeCorrection.basePayment },
      { id: 'total-payment', value: expected.expectedBeforeCorrection.totalPayment },
      { id: 'insurance-name', value: expected.expectedInsurance.name },
    ], pdfBytes, await context.cookies());
    report.privacy.storage = storageScan.storage;
    report.privacy.storageInspectionErrors = storageScan.errors;
    report.privacy.persistedDocumentMarkers = storageScan.markerIds;
    report.privacy.markerLocations = storageScan.markerLocations;
    const storageStatuses = Object.values(storageScan.storage).map(value => typeof value === 'string' ? value : value.status);
    const storageInspectionComplete = storageStatuses.every(status => status === 'INSPECTED');
    const networkViolations = report.privacy.blockedRequests.length;
    report.privacy.observedChecksStatus = networkViolations || storageScan.markerIds.length
      ? 'FAIL'
      : storageInspectionComplete ? 'PASS' : 'NOT_RUN';
    report.privacy.status = report.privacy.observedChecksStatus === 'NOT_RUN' ? 'NOT_RUN' : 'PARTIAL';
    record(
      'SEC-01-SCOPED',
      report.privacy.observedChecksStatus,
      `${report.privacy.externalRequests.length} solicitudes externas; ${networkViolations} bloqueadas; almacenes inspeccionados: ${storageStatuses.filter(status => status === 'INSPECTED').length}/${storageStatuses.length}.`,
      report.privacy.observedChecksStatus === 'FAIL'
        ? `Solicitudes bloqueadas: ${networkViolations}; marcadores sintéticos encontrados: ${storageScan.markerIds.join(', ')}`
        : storageInspectionComplete ? null : `Inspección incompleta: ${storageScan.errors.join('; ')}`
    );
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    if (!report.steps.some(step => step.id === 'DOC-FIXTURE')) record('DOC-RUNNER', 'BLOCKED', null, reason);
    else record('DOC-RUNNER', 'FAIL', null, reason);
  } finally {
    await context?.close().catch(() => {});
    if (previewProcess && previewProcess.exitCode === null) previewProcess.kill();
    const result = diagnosticMode
      ? {
        complete: false,
        exitCode: report.diagnostic?.verdict === 'PUNCTUATION_SYMPTOM_NOT_REPRODUCED_IN_32_TOKEN_PROBE' ||
          report.diagnostic?.verdict === 'OFFICIAL_CONTROL_MATCHED' ? 0
          : report.diagnostic?.verdict?.startsWith('REPRODUCED_') || report.diagnostic?.verdict === 'OFFICIAL_CONTROL_DID_NOT_MATCH' ? 1 : 2,
        status: report.diagnostic?.verdict ?? 'INCOMPLETE',
      }
      : summarizeVerification(report.steps);
    report.finishedAt = new Date().toISOString();
    report.result = result;
    const jsonPath = path.join(reportDirectory, 'report.json');
    const markdownPath = path.join(reportDirectory, 'report.md');
    const formatSummary = formats.map(format => `${format} ${report.coverage.formats[format]}`).join('; ');
    await writeFile(jsonPath, `${JSON.stringify(report, null, 2)}\n`);
    await writeFile(markdownPath, diagnosticMode
      ? `# Docling Worker diagnostic\n\n**Estado:** ${result.status}\n\n**Alcance:** sonda diagnóstica limitada; no es aceptación #33.\n\n**Interpretación:** el corte de 60 s solo limita la observación; no distingue cálculo lento de bloqueo.\n\n**Ejecución:** ${runId}\n\n**Fuente:** ${report.source.branch ?? 'desconocida'}@${report.source.commit ?? 'desconocido'}\n\n**Distribución:** ${report.artifact.sha256 ?? 'no identificada'}\n\n**Pesos verificados:** ${report.diagnostic?.modelStatus?.verifiedArtifacts ?? 'sin evidencia'}/${MODEL_ARTIFACTS.length}\n\n**Último evento ONNX:** ${JSON.stringify(report.diagnostic?.onnxEvents?.at(-1) ?? null)}\n\n**Referencia oficial:** ${report.diagnostic?.officialReference?.status ?? 'NOT_RUN'}\n\n**Evidencia:** ${jsonPath}\n`
      : `# Docling Web real inference\n\n**Estado:** ${result.status}\n\n**Ejecución:** ${runId}\n\n**Fuente:** ${report.source.branch ?? 'desconocida'}@${report.source.commit ?? 'desconocido'}\n\n**Distribución:** ${report.artifact.sha256 ?? 'no identificada'}\n\n**Resultado de inferencia:** ${report.inference.status}${report.inference.error ? ` · ${report.inference.error}` : ''}\n\n**Cobertura por formato:** ${formatSummary}. Ambigüedad: ${report.coverage.ambiguity}.\n\n**Privacidad:** estado global SEC-01 ${report.steps.find(step => step.id === 'SEC-01')?.status ?? 'NOT_RUN'}; comprobaciones acotadas ${report.privacy.observedChecksStatus}.\n\n**Aplicación al escenario:** ${report.scenarioApplication.status} · ${report.scenarioApplication.reason}\n\n**Evidencia:** ${jsonPath}\n`);
    console.log(`${diagnosticMode ? 'Docling diagnóstico' : 'Docling Web'} ${result.status}. Informe: ${markdownPath}`);
    process.exitCode = result.exitCode;
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) run();

export { report };
