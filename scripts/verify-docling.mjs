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
  MODEL_REVISION,
  MODEL_TOTAL_BYTES,
  MODEL_ARTIFACTS,
} from '../kmp/composeApp/docling-worker/src/model-artifacts.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const distRoot = path.join(root, 'kmp', 'composeApp', 'build', 'dist', 'wasmJs', 'productionExecutable');
const fixtureRoot = path.join(root, 'kmp', 'composeApp', 'browser-tests', 'fixtures', 'docling-synthetic');
const reportRoot = path.join(os.homedir(), '.finsim', 'verification', 'docling');
const profileRoot = path.join(os.tmpdir(), 'finsim-docling-profile');
const runId = `${new Date().toISOString().replaceAll(':', '-')}-${randomUUID()}`;
const reportDirectory = path.join(reportRoot, runId);
const report = {
  profile: 'Docling Web real inference',
  command: 'npm run verify:docling',
  runId,
  startedAt: new Date().toISOString(),
  source: {},
  platform: 'Chrome WebAssembly',
  requirements: ['DOC-01', 'DOC-02', 'DOC-03', 'DOC-04', 'SEC-01', 'VERIFY-01', 'VERIFY-02', 'VERIFY-03'],
  fixture: {},
  model: { id: MODEL_ID, revision: MODEL_REVISION, bytes: MODEL_TOTAL_BYTES, artifacts: MODEL_ARTIFACTS.length },
  artifact: {},
  steps: [],
  inference: { status: 'NOT_RUN', progress: [], result: null, error: null },
  extraction: { status: 'NOT_RUN', expected: {}, actual: {}, mismatches: [] },
  privacy: { status: 'NOT_RUN', externalRequests: [], persistedDocumentMarkers: [] },
  appliedToScenario: false,
};

let previewProcess;
let context;

function hash(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

function record(id, status, actual, reason) {
  report.steps.push({ id, status, ...(actual ? { actual } : {}), ...(reason ? { reason } : {}) });
}

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
  const port = await freePort();
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

async function inspectStorage(page, markers) {
  const storage = await page.evaluate(async () => {
    const localValues = Object.entries(localStorage).flatMap(([key, value]) => [key, value]);
    const cacheUrls = [];
    for (const cacheName of await caches.keys()) {
      const cache = await caches.open(cacheName);
      for (const request of await cache.keys()) cacheUrls.push(request.url);
    }
    return { localValues, cacheUrls };
  });
  return markers.filter(marker =>
    storage.localValues.some(value => value.includes(marker)) ||
    storage.cacheUrls.some(value => value.includes(marker))
  );
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
        channel: 'chrome', headless: true, viewport: { width: 1280, height: 1000 },
      });
    } catch (error) {
      record('DOC-CHROME', 'BLOCKED', null, error instanceof Error ? error.message : String(error));
      return;
    }
    report.browser = { version: context.browser()?.version() ?? null };
    const page = context.pages()[0] ?? await context.newPage();
    const origin = new URL(url).origin;
    page.on('pageerror', error => { report.pageErrors ??= []; report.pageErrors.push(error.message); });
    context.on('request', request => {
      const requestUrl = new URL(request.url());
      if (requestUrl.origin !== origin) {
        report.privacy.externalRequests.push({ origin: requestUrl.origin, method: request.method(), hasBody: request.postData() !== null });
      }
    });
    await page.addInitScript(() => {
      window.__doclingProfile = { progress: [], result: null };
      const OriginalWorker = window.Worker;
      window.Worker = new Proxy(OriginalWorker, {
        construct(target, args, newTarget) {
          const worker = Reflect.construct(target, args, newTarget);
          if (String(args[0]).includes('docling/worker.mjs')) {
            worker.addEventListener('message', event => {
              let message;
              try { message = typeof event.data === 'string' ? JSON.parse(event.data) : event.data; } catch { return; }
              if (message?.kind === 'progress') {
                window.__doclingProfile.progress.push({ stage: message.stage ?? null, device: message.device ?? null });
              }
              if (message?.kind === 'result' && message.docTags) {
                window.__doclingProfile.result = {
                  docTags: message.docTags,
                  device: message.device ?? null,
                  outputTokens: message.outputTokens ?? null,
                  maxGenerationTokens: message.maxGenerationTokens ?? null,
                  stoppedAtTokenLimit: message.stoppedAtTokenLimit === true,
                  processorMs: message.processorMs ?? null,
                  modelLoadMs: message.modelLoadMs ?? null,
                  processorAndInferenceMs: message.processorAndInferenceMs ?? null,
                };
              }
            });
          }
          return worker;
        },
      });
    });
    await page.goto(url);
    await clickComposeControl(page, page.getByRole('button', { name: 'Importar extracto', exact: true }).first());
    await page.waitForFunction(() => {
      const text = document.body?.innerText ?? '';
      return text.includes('Instalar modelo local') || text.includes('✓ Instalado y verificado') || text.includes('No se pudo verificar Granite-Docling');
    }, null, { timeout: 180_000 });

    let bodyText = await page.locator('body').innerText();
    if (!bodyText.includes('✓ Instalado y verificado')) {
      if (!bodyText.includes('Instalar modelo local')) {
        record('DOC-MODEL', 'BLOCKED', null, bodyText.match(/No se pudo verificar Granite-Docling[^\n]*/) ?? 'No se pudo establecer estado del modelo.');
        return;
      }
      report.model.installStarted = true;
      await clickComposeControl(page, page.getByRole('button', { name: /Instalar modelo local/ }));
      try {
        await page.waitForFunction(() => {
          const text = document.body?.innerText ?? '';
          return text.includes('✓ Instalado y verificado') || text.includes('No se pudo verificar Granite-Docling');
        }, null, { timeout: 15 * 60_000 });
      } catch (error) {
        record('DOC-MODEL-INSTALL', 'BLOCKED', null, error instanceof Error ? error.message : String(error));
        return;
      }
      bodyText = await page.locator('body').innerText();
      if (!bodyText.includes('✓ Instalado y verificado')) {
        record('DOC-MODEL-INSTALL', 'BLOCKED', null, bodyText.match(/No se pudo verificar Granite-Docling[^\n]*/) ?? 'No se completó la instalación verificada.');
        return;
      }
      record('DOC-MODEL-INSTALL', 'PASS', `${MODEL_ARTIFACTS.length} archivos y ${MODEL_TOTAL_BYTES} bytes verificados por el Worker.`);
    } else {
      record('DOC-MODEL', 'PASS', `${MODEL_ARTIFACTS.length} archivos y ${MODEL_TOTAL_BYTES} bytes ya verificados por el Worker.`);
    }

    const chooserPromise = page.waitForEvent('filechooser', { timeout: 30_000 });
    await clickComposeControl(page, page.getByRole('button', { name: 'Seleccionar PDF, PNG o JPEG', exact: true }));
    const chooser = await chooserPromise;
    await chooser.setFiles({ name: report.fixture.name, mimeType: 'application/pdf', buffer: pdfBytes });
    await clickComposeControl(page, page.getByRole('button', { name: 'Analizar página 1 con Granite-Docling', exact: true }));
    try {
      await page.waitForFunction(() => {
        const text = document.body?.innerText ?? '';
        return text.includes('Revisión de extracción') || text.includes('Error al analizar el documento visual');
      }, null, { timeout: 240_000 });
    } catch (error) {
      report.inference.error = error instanceof Error ? error.message : String(error);
    }

    const workerEvidence = await page.evaluate(() => window.__doclingProfile);
    report.inference.progress = workerEvidence?.progress ?? [];
    report.inference.result = workerEvidence?.result ?? null;
    bodyText = await page.locator('body').innerText();
    if (!report.inference.result) {
      report.inference.status = 'FAIL';
      report.inference.error ??= bodyText.match(/Error al analizar el documento visual[^\n]*/) ?? 'No llegó una respuesta real del Worker.';
      record('DOC-01', 'FAIL', null, report.inference.error);
    } else if (report.inference.result.stoppedAtTokenLimit) {
      report.inference.status = 'FAIL';
      record('DOC-01', 'FAIL', null, 'El modelo real llegó al límite de tokens; la salida se conserva como incompleta.');
    } else {
      report.inference.status = 'PASS';
      record('DOC-01', 'PASS', `DocTags reales recibidos desde Granite-Docling (${report.inference.result.device ?? 'dispositivo no informado'}).`);
    }

    if (bodyText.includes('Revisión de extracción') && report.inference.result) {
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
      const reviewBeforeApply = bodyText.includes('Revisión de extracción') && !report.appliedToScenario;
      const insuranceMeaning = bodyText.includes('Seguro de vida') && bodyText.includes('periodicidad mensual');
      report.extraction.status = report.extraction.mismatches.length === 0 && reviewBeforeApply && insuranceMeaning ? 'PASS' : 'FAIL';
      record(
        'DOC-02/03/04',
        report.extraction.status,
        `${expectedFields.length - report.extraction.mismatches.length}/${expectedFields.length} campos originales coinciden; extracción revisada antes de aplicar.`,
        report.extraction.mismatches.length ? JSON.stringify(report.extraction.mismatches) : insuranceMeaning ? null : 'Faltó confirmar significado/periodicidad mensual del seguro.'
      );
    } else {
      record('DOC-02/03/04', 'NOT_RUN', null, 'No se llegó a la hoja de revisión con salida real del modelo.');
    }

    const pdfBase64Marker = pdfBytes.toString('base64').slice(0, 128);
    report.privacy.persistedDocumentMarkers = await inspectStorage(page, [report.fixture.name, report.fixture.pdfSha256, pdfBase64Marker]);
    const externalViolations = report.privacy.externalRequests.filter(request => !['GET', 'HEAD'].includes(request.method) || request.hasBody);
    report.privacy.status = report.privacy.persistedDocumentMarkers.length === 0 && externalViolations.length === 0 ? 'PASS' : 'FAIL';
    record(
      'SEC-01',
      report.privacy.status,
      `${report.privacy.externalRequests.length} solicitudes externas observadas; no se detectó el fixture en localStorage/Cache Storage.`,
      externalViolations.length || report.privacy.persistedDocumentMarkers.length
        ? `Solicitudes incompatibles: ${JSON.stringify(externalViolations)}; marcadores persistidos: ${report.privacy.persistedDocumentMarkers.join(', ')}`
        : null
    );
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    if (!report.steps.length) record('DOC-RUNNER', 'BLOCKED', null, reason);
    else record('DOC-RUNNER', 'FAIL', null, reason);
  } finally {
    await context?.close().catch(() => {});
    if (previewProcess && previewProcess.exitCode === null) previewProcess.kill();
    const result = summarizeVerification(report.steps);
    report.finishedAt = new Date().toISOString();
    report.result = result;
    const jsonPath = path.join(reportDirectory, 'report.json');
    const markdownPath = path.join(reportDirectory, 'report.md');
    await writeFile(jsonPath, `${JSON.stringify(report, null, 2)}\n`);
    await writeFile(markdownPath, `# Docling Web real inference\n\n**Estado:** ${result.status}\n\n**Ejecución:** ${runId}\n\n**Fuente:** ${report.source.branch ?? 'desconocida'}@${report.source.commit ?? 'desconocido'}\n\n**Distribución:** ${report.artifact.sha256 ?? 'no identificada'}\n\n**Resultado de inferencia:** ${report.inference.status}${report.inference.error ? ` · ${report.inference.error}` : ''}\n\n**Aplicado al escenario:** no\n\n**Evidencia:** ${jsonPath}\n`);
    console.log(`Docling Web ${result.status}. Informe: ${markdownPath}`);
    process.exitCode = result.exitCode;
  }
}

run();
