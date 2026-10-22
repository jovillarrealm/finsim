import { createHash, randomUUID } from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import { createReadStream, createWriteStream } from 'node:fs';
import { lstat, mkdir, readFile, readdir, readlink, rename, stat, writeFile } from 'node:fs/promises';
import { once } from 'node:events';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { summarizeVerification } from './verification-status.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const distRoot = path.join(root, 'kmp', 'composeApp', 'build', 'dist', 'wasmJs', 'productionExecutable');
const catalogRoot = path.join(root, 'scripts', 'web-catalog');
const reportRoot = path.join(os.homedir(), '.finsim', 'verification', 'web');
const expectedGradle = '9.3.1';
const preferredJdk = 'C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot';

function git(args, options = {}) {
  return execFileSync('git', args, { cwd: root, ...options });
}

async function workingTreeIdentity() {
  const diff = git(['diff', '--binary', 'HEAD', '--']);
  const untracked = git(['ls-files', '--others', '--exclude-standard', '-z']).toString('utf8').split('\0').filter(Boolean).sort();
  const hash = createHash('sha256').update(diff).update('\0');
  for (const relativePath of untracked) {
    const filePath = path.resolve(root, relativePath);
    const relative = path.relative(root, filePath);
    if (relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) throw new Error('Untracked file resolved outside the repository.');
    hash.update(relativePath).update('\0');
    const metadata = await lstat(filePath);
    if (metadata.isSymbolicLink()) hash.update('symlink:').update(await readlink(filePath));
    else if (metadata.isFile()) {
      const stream = createReadStream(filePath);
      for await (const chunk of stream) hash.update(chunk);
    }
    hash.update('\0');
  }
  const status = git(['status', '--porcelain', '--untracked-files=no'], { encoding: 'utf8' }).trim();
  return {
    hasLocalChanges: diff.length > 0 || untracked.length > 0 || status.length > 0,
    sha256: hash.digest('hex'),
    trackedFilesChanged: status ? status.split(/\r?\n/).length : 0,
    untrackedFiles: untracked.length,
  };
}

async function sourceIdentity() {
  return {
    repository: git(['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim(),
    branch: git(['branch', '--show-current'], { encoding: 'utf8' }).trim() || 'DETACHED',
    commit: git(['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
    workingTree: await workingTreeIdentity(),
  };
}

export function parseTapResults(output) {
  const results = [];
  for (const line of output.split(/\r?\n/)) {
    const match = line.match(/^\s*(not ok|ok)\s+\d+\s+-\s+(.+?)\s*$/);
    if (!match) continue;
    const titleAndDirective = match[2];
    const directive = titleAndDirective.match(/\s+#\s+(SKIP|TODO)(?:\s+.*)?$/i)?.[1]?.toUpperCase();
    const title = titleAndDirective.replace(/\s+#\s+(?:SKIP|TODO)(?:\s+.*)?$/i, '').trim();
    results.push({
      title,
      status: match[1] === 'not ok' ? 'FAIL' : directive ? 'NOT_RUN' : 'PASS',
    });
  }
  return results;
}

export function evaluateWebCases(catalog, observed) {
  return catalog.map(item => {
    const matches = observed.filter(result => result.title === item.title);
    if (matches.length > 1) {
      return { ...item, status: 'FAIL', actual: `TAP informó ${matches.length} resultados con el mismo título; la asociación no es inequívoca.` };
    }
    const result = matches[0];
    if (!result) return { ...item, status: 'NOT_RUN', actual: 'No se encontró este título exacto en la salida TAP nueva de la ejecución.' };
    return {
      ...item,
      status: result.status,
      actual: `TAP informó el título exacto con estado ${result.status}.`,
      ...(result.status === 'NOT_RUN' ? { reason: 'Node marcó el caso omitido o pendiente.' } : {}),
    };
  });
}

export function validateWebCatalog(catalog) {
  const errors = [];
  if (!Array.isArray(catalog) || catalog.length === 0) return ['El catálogo web debe contener al menos un caso.'];
  const ids = new Set();
  const titles = new Set();
  for (const [index, item] of catalog.entries()) {
    for (const field of ['id', 'title', 'requirement', 'expectation', 'expectationSource', 'platform', 'source']) {
      if (typeof item?.[field] !== 'string' || !item[field].trim()) errors.push(`Caso ${index + 1}: falta ${field}.`);
    }
    if (item?.id && ids.has(item.id)) errors.push(`ID duplicado: ${item.id}.`);
    if (item?.title && titles.has(item.title)) errors.push(`Título duplicado: ${item.title}.`);
    if (item?.id) ids.add(item.id);
    if (item?.title) titles.add(item.title);
  }
  return errors;
}

export function validateReusableBuild({ previous, current, changedSinceBuild, buildInputChangesSince = [], artifactHash }) {
  const errors = [];
  if (previous?.build?.status !== 'PASS' || previous.build.exitCode !== 0) errors.push('El reporte no contiene un build Wasm exitoso.');
  if (!previous?.steps?.some(step => step.id === 'WEB-TOOLCHAIN' && step.status === 'PASS')) errors.push('El reporte no confirma un toolchain Gradle/JDK compatible.');
  if (!previous?.steps?.some(step => step.id === 'WEB-BUILD' && step.status === 'PASS')) errors.push('El reporte no confirma la distribución Wasm generada.');
  if (!previous?.source?.commit || previous.source.commit !== current?.commit) errors.push('El commit del build no coincide con la fuente actual.');
  if (previous?.source?.workingTree?.trackedFilesChanged !== current?.workingTree?.trackedFilesChanged ||
      previous?.source?.workingTree?.untrackedFiles !== current?.workingTree?.untrackedFiles) {
    errors.push('Cambió el inventario de archivos locales desde el build.');
  }
  if (!previous?.artifact?.sha256 || previous.artifact.sha256 !== artifactHash) errors.push('El hash del artefacto no coincide con el build.');
  if (buildInputChangesSince.length) errors.push(`Cambió una entrada de producto, compilación o dependencias después del build: ${buildInputChangesSince.join(', ')}.`);

  const workspaceMatches = previous?.source?.workingTree?.sha256 === current?.workingTree?.sha256;
  const verifierOnlyChanges = new Set([
    'scripts/verify-web.mjs',
    'scripts/verify-web.test.mjs',
    'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs',
  ]);
  if (!workspaceMatches && (changedSinceBuild.length === 0 || changedSinceBuild.some(file => !verifierOnlyChanges.has(file)))) {
    errors.push('El hash completo del árbol difiere y contiene cambios posteriores fuera del verificador web, su selfcheck y el caso browser #40.');
  }
  if (changedSinceBuild.some(file => !verifierOnlyChanges.has(file))) {
    errors.push(`Hay archivos modificados después del build fuera del verificador/selfcheck: ${changedSinceBuild.filter(file => !verifierOnlyChanges.has(file)).join(', ')}.`);
  }
  return errors;
}

export async function loadWebCatalogs(directory = catalogRoot) {
  const files = (await readdir(directory)).filter(file => /^issue-\d+\.mjs$/.test(file)).sort();
  const catalogs = [];
  for (const file of files) {
    const module = await import(pathToFileURL(path.join(directory, file)).href);
    if (!Array.isArray(module.default)) throw new Error(`${file} debe exportar una lista por defecto.`);
    catalogs.push(...module.default);
  }
  return { files, cases: catalogs };
}

function runProcess(command, args, logPath, { env = process.env, echo = true, onOutput } = {}) {
  return new Promise(async resolve => {
    const log = createWriteStream(logPath, { flags: 'wx' });
    try {
      await once(log, 'open');
    } catch (error) {
      resolve({ exitCode: null, signal: null, error, output: '' });
      return;
    }
    let output = '';
    let processError = null;
    let settled = false;
    const finish = (exitCode, signal = null) => {
      if (settled) return;
      settled = true;
      log.end(() => resolve({ exitCode, signal, error: processError, output }));
    };
    try {
      const child = spawn(command, args, {
        cwd: root,
        env,
        shell: process.platform === 'win32' && (/\.(?:bat|cmd)$/i.test(command) || command === 'npm'),
        windowsHide: true,
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      const consume = chunk => {
        log.write(chunk);
        if (echo) process.stdout.write(chunk);
        output = `${output}${chunk.toString('utf8')}`.slice(-200_000);
        onOutput?.(output);
      };
      child.stdout.on('data', consume);
      child.stderr.on('data', consume);
      child.on('error', error => { processError = error; finish(null); });
      child.on('close', (code, signal) => finish(code, signal));
    } catch (error) {
      processError = error;
      finish(null);
    }
  });
}

function commandText(command, args) {
  return [command, ...args].map(value => /\s/.test(value) ? `"${value}"` : value).join(' ');
}

function hashBytes(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

async function artifactManifest(directory) {
  const entries = [];
  async function visit(current) {
    for (const entry of await readdir(current, { withFileTypes: true })) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) await visit(fullPath);
      else if (entry.isFile()) {
        const bytes = await readFile(fullPath);
        entries.push({ path: path.relative(directory, fullPath).replaceAll(path.sep, '/'), sha256: hashBytes(bytes), bytes: bytes.length });
      }
    }
  }
  await visit(directory);
  entries.sort((left, right) => left.path.localeCompare(right.path));
  const aggregate = createHash('sha256');
  for (const entry of entries) aggregate.update(entry.path).update('\0').update(entry.sha256).update('\0');
  return { sha256: aggregate.digest('hex'), files: entries.length, entries };
}

async function copyFreshManifest(manifest, destination) {
  await mkdir(destination, { recursive: true });
  const contents = `${JSON.stringify(manifest, null, 2)}\n`;
  await writeFile(path.join(destination, 'wasm-manifest.json'), contents, { flag: 'wx' });
  return { path: 'wasm-manifest.json', sha256: hashBytes(Buffer.from(contents)) };
}

async function freePort() {
  const server = net.createServer();
  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const address = server.address();
  await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  return address.port;
}

async function startPreview(directory, logPath) {
  const port = await freePort();
  const url = `http://127.0.0.1:${port}/`;
  const viteCli = path.join(root, 'node_modules', 'vite', 'bin', 'vite.js');
  const args = [viteCli, 'preview', '--host', '127.0.0.1', '--port', String(port), '--strictPort', '--outDir', directory];
  const log = createWriteStream(logPath, { flags: 'wx' });
  await once(log, 'open');
  let output = '';
  let processError = null;
  let closed;
  const child = spawn(process.execPath, args, { cwd: root, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  const closedPromise = new Promise(resolve => { closed = resolve; });
  const consume = chunk => { log.write(chunk); output = `${output}${chunk.toString('utf8')}`.slice(-20_000); };
  child.stdout.on('data', consume);
  child.stderr.on('data', consume);
  child.on('error', error => { processError = error; closed({ exitCode: null, error }); });
  child.on('close', (code, signal) => { log.end(); closed({ exitCode: code, signal, error: processError }); });

  let ready = false;
  let reason = null;
  for (let attempt = 0; attempt < 60; attempt += 1) {
    if (child.exitCode !== null) break;
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(1000) });
      if (response.ok) { ready = true; break; }
      reason = `preview respondió HTTP ${response.status}`;
    } catch (error) {
      reason = error instanceof Error ? error.message : String(error);
    }
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  if (!ready) {
    child.kill('SIGTERM');
    await closedPromise;
    return { child, closedPromise, url, command: commandText(process.execPath, args), output, ready, reason: processError?.message ?? reason };
  }
  return {
    child, closedPromise, url, command: commandText(process.execPath, args), output, ready,
    stop: async () => {
      if (child.exitCode === null) child.kill('SIGTERM');
      await Promise.race([closedPromise, new Promise(resolve => setTimeout(resolve, 3000))]);
    },
  };
}

async function verifyServedArtifact(manifest, baseUrl) {
  const index = manifest.entries.find(entry => entry.path === 'index.html');
  if (!index) return { status: 'FAIL', reason: 'La distribución no contiene index.html.' };
  const mismatches = [];
  for (const entry of manifest.entries) {
    const urlPath = entry.path.split('/').map(encodeURIComponent).join('/');
    try {
      const response = await fetch(new URL(urlPath, baseUrl), { signal: AbortSignal.timeout(15_000) });
      if (!response.ok) {
        mismatches.push(`${entry.path}: HTTP ${response.status}`);
        continue;
      }
      const digest = hashBytes(Buffer.from(await response.arrayBuffer()));
      if (digest !== entry.sha256) mismatches.push(`${entry.path}: hash servido ${digest} ≠ build ${entry.sha256}`);
    } catch (error) {
      mismatches.push(`${entry.path}: ${error instanceof Error ? error.message : String(error)}`);
    }
  }
  return mismatches.length
    ? { status: 'FAIL', mismatches }
    : { status: 'PASS', reason: `${manifest.files} archivos servidos coinciden byte por byte con la distribución Wasm.` };
}

async function browserVersion() {
  let browser;
  try {
    const { chromium } = await import('@playwright/test');
    browser = await chromium.launch({ channel: 'chrome', headless: true });
    return { status: 'PASS', version: browser.version() };
  } catch (error) {
    return { status: 'BLOCKED', reason: error instanceof Error ? error.message : String(error) };
  } finally {
    await browser?.close();
  }
}

function statusOf(result) {
  if (result.exitCode === 0) return 'PASS';
  if (result.error?.code === 'ENOENT' || result.exitCode === null && result.error) return 'BLOCKED';
  return 'FAIL';
}

function safeText(value) {
  return String(value ?? '').replaceAll('|', '\\|').replaceAll('\n', ' ').replaceAll('\r', ' ');
}

function renderMarkdown(report) {
  const tree = report.source.workingTree;
  const buildIdentity = report.build.identity;
  const cases = report.catalog.cases.map(item => `### ${safeText(item.id)} — ${safeText(item.status)}\n\n**Criterio:** ${safeText(item.requirement)} · ${safeText(item.platform)}\n\n**Expectativa:** ${safeText(item.expectation)}\n\n**Fuente de expectativa:** ${safeText(item.expectationSource)}\n\n**Resultado observado:** ${safeText(item.actual ?? item.reason)}\n\n**Caso:** \`${safeText(item.title)}\` en \`${safeText(item.source)}\``).join('\n\n');
  const executions = report.suites.map(suite => `- **${safeText(suite.name)}:** ${suite.status}; ${suite.passed} PASS, ${suite.failed} FAIL, ${suite.notRun} NOT_RUN. [Salida](${safeText(suite.log)})`).join('\n');
  const steps = report.steps.map(step => `- **${safeText(step.id)} · ${step.status}:** ${safeText(step.actual ?? step.reason)}${step.log ? ` [Log](${safeText(step.log)})` : ''}`).join('\n');
  const coverage = report.coverageNotRun.map(item => `- **${safeText(item.criterion)} · ${safeText(item.platform)} · ${item.status}:** ${safeText(item.reason)} Retomar: ${safeText(item.resume)}`).join('\n');
  const evidence = report.evidenceFiles.map(item => `- \`${safeText(item.path)}\` · SHA-256 \`${item.sha256}\``).join('\n');
  const identityText = buildIdentity
    ? `**Fuente que produjo el artefacto:** \`${safeText(buildIdentity.buildCommit)}\` · árbol \`${safeText(buildIdentity.buildWorkingTreeSha256)}\`  \n**Fuente del verificador actual:** \`${safeText(buildIdentity.verifierCommit)}\` · árbol \`${safeText(buildIdentity.verifierWorkingTreeSha256)}\`  \n**Diferencia posterior al build:** ${buildIdentity.changedSinceBuild.map(file => `\`${safeText(file)}\``).join(', ') || 'ninguna'}  \n**Entradas de producto/build/dependencias posteriores:** ${buildIdentity.buildInputChangesSince.map(file => `\`${safeText(file)}\``).join(', ') || 'ninguna'}  \n**Reporte original del build:** \`${safeText(buildIdentity.referenceReport)}\` · SHA-256 \`${safeText(buildIdentity.referenceReportSha256)}\`  \n**Árbol local completo idéntico:** ${buildIdentity.workspaceHashMatches ? 'sí' : 'no; la diferencia está delimitada arriba'}`
    : 'Este runner construyó la distribución desde el árbol local identificado al inicio de esta ejecución.';
  return `# WEB — verificación de la distribución Wasm\n\n**Estado de ejecución:** ${safeText(report.executionState ?? 'INCOMPLETE')} · fase ${safeText(report.progress?.phase ?? 'inicializando')}${report.progress?.activeSuite ? ` · suite ${safeText(report.progress.activeSuite)}` : ''}${report.progress?.activeCase ? ` · caso ${safeText(report.progress.activeCase)}` : ''}  \n**Estado del alcance web:** ${report.result.status} (exit ${report.result.exitCode})  \n**Ejecución:** \`${report.runId}\` · ${report.startedAt}  \n**Origen actual:** \`${safeText(report.source.branch)}@${safeText(report.source.commit)}\`  \n**Cambios locales:** ${tree.hasLocalChanges ? 'sí' : 'no'} · SHA-256 \`${tree.sha256}\`  \n**Entorno:** ${safeText(report.environment.os)} ${safeText(report.environment.arch)} · Node ${safeText(report.environment.node)} · Gradle ${safeText(report.environment.gradleVersion ?? 'no disponible')} · Chrome ${safeText(report.environment.chrome ?? 'no disponible')}  \n**Evidencia:** \`${safeText(report.evidenceDirectory)}\`\n\n## Identidad de la fuente\n\n${identityText}\n\n## Distribución servida\n\n- **Comando de build:** \`${safeText(report.build.command ?? 'no ejecutado')}\`\n- **Artefacto:** \`${safeText(report.artifact.path)}\` · ${report.artifact.files ?? 0} archivos · SHA-256 \`${safeText(report.artifact.sha256 ?? 'no disponible')}\`\n- **URL:** ${safeText(report.server.url ?? 'no servida')}\n- **Verificación HTTP:** ${safeText(report.artifact.served ?? 'no ejecutada')}\n\n## Pasos de verificación\n\n${steps || 'Sin pasos.'}\n\n## Suites solicitadas\n\n${executions}\n\nLa prueba completa incluye validación del verificador y toda la suite browser. La orden test:e2e ejecuta la suite browser sola. Los casos browser se repiten en ambas órdenes; el reporte conserva cada resultado y la intersección, sin tratar las órdenes como suites independientes.\n\n## Casos del catálogo\n\n${cases || 'No se cargaron casos.'}\n\n## Cobertura pendiente\n\n${coverage || 'Sin omisiones registradas.'}\n\nEl viewport táctil representa interacción web en Chrome y no certifica una APK, lector de pantalla ni teléfono físico.\n\n## Evidencia externa\n\n${evidence || 'Sin logs.'}\n`;
}

async function replaceReportFile(filePath, content) {
  const temporaryPath = `${filePath}.${randomUUID()}.tmp`;
  await writeFile(temporaryPath, content, { flag: 'wx' });
  await rename(temporaryPath, filePath);
}

async function writeProgressReport(report, directory) {
  report.executionState = 'INCOMPLETE';
  report.progress.updatedAt = new Date().toISOString();
  const statuses = [
    ...report.steps.map(step => ({ status: step.status, reason: step.reason })),
    ...report.suites.map(suite => ({ status: suite.status, reason: suite.reason })),
    ...report.catalog.cases.map(item => ({ status: item.status, reason: item.reason })),
  ];
  report.result = summarizeVerification(statuses);
  await replaceReportFile(path.join(directory, 'report.json'), `${JSON.stringify(report, null, 2)}\n`);
  await replaceReportFile(path.join(directory, 'report.md'), renderMarkdown(report));
}

async function writeReport(report, directory) {
  report.finishedAt = new Date().toISOString();
  report.executionState = 'COMPLETE';
  report.progress.phase = 'complete';
  report.progress.activeSuite = null;
  report.progress.activeCase = null;
  report.progress.updatedAt = report.finishedAt;
  const statuses = [
    ...report.steps.map(step => ({ status: step.status, reason: step.reason })),
    ...report.suites.map(suite => ({ status: suite.status, reason: suite.reason })),
    ...report.catalog.cases.map(item => ({ status: item.status, reason: item.reason })),
  ];
  report.result = summarizeVerification(statuses);
  for (const file of report.evidenceFiles) {
    const fullPath = path.join(directory, file.path);
    try { file.sha256 = hashBytes(await readFile(fullPath)); } catch { file.sha256 = null; }
  }
  await replaceReportFile(path.join(directory, 'report.json'), `${JSON.stringify(report, null, 2)}\n`);
  await replaceReportFile(path.join(directory, 'report.md'), renderMarkdown(report));
  console.log(`WEB ${report.result.status}. Evidencia externa: ${directory}`);
  return report.result.exitCode;
}

function emptySuite(name, log) {
  return { name, status: 'NOT_RUN', passed: 0, failed: 0, notRun: 0, log, results: [], cases: [] };
}

function refreshCatalogResults(report, discovered) {
  report.catalog.cases = discovered.cases.map(item => {
    const executions = Object.fromEntries(report.suites.map(suite => [
      suite.name,
      suite.cases.find(check => check.id === item.id)?.status ?? 'NOT_RUN',
    ]));
    const statuses = Object.values(executions);
    return {
      ...item,
      status: statuses.every(status => status === 'PASS') ? 'PASS' : statuses.some(status => status === 'FAIL') ? 'FAIL' : 'NOT_RUN',
      executions,
      actual: report.suites.map(suite => `${suite.name}: ${suite.cases.find(check => check.id === item.id)?.actual ?? 'sin coincidencia exacta'}`).join(' '),
    };
  });
}

async function nodeDependencies() {
  const expected = [
    path.join(root, 'node_modules', 'vite', 'bin', 'vite.js'),
    path.join(root, 'node_modules', '@playwright', 'test', 'package.json'),
  ];
  const missing = [];
  for (const item of expected) {
    try { await stat(item); } catch { missing.push(path.relative(root, item).replaceAll(path.sep, '/')); }
  }
  return { ready: missing.length === 0, missing };
}

async function runBrowserChecks(report, directory, discovered, manifest) {
  report.artifact.sha256 = manifest.sha256;
  report.artifact.files = manifest.files;
  const manifestEvidence = await copyFreshManifest(manifest, directory);
  report.evidenceFiles.push(manifestEvidence);
  report.artifact.manifest = manifestEvidence.path;
  report.progress.phase = 'artifact-verified';
  await writeProgressReport(report, directory);

  const previewLog = 'preview.log';
  report.evidenceFiles.push({ path: previewLog, sha256: null });
  report.progress.phase = 'preview-starting';
  await writeProgressReport(report, directory);
  const preview = await startPreview(distRoot, path.join(directory, previewLog));
  report.server.url = preview.url;
  report.server.command = preview.command;
  report.server.status = preview.ready ? 'PASS' : 'FAIL';
  try {
    if (!preview.ready) {
      report.steps.push({ id: 'WEB-PREVIEW', status: 'FAIL', actual: preview.reason ?? 'No se pudo iniciar el servidor preview.', log: previewLog });
      report.steps.push({ id: 'WEB-BROWSER-SUITES', status: 'NOT_RUN', reason: 'No se pudo servir la distribución Wasm construida en esta ejecución.' });
      report.progress.phase = 'preview-failed';
      await writeProgressReport(report, directory);
      return;
    }

    report.steps.push({ id: 'WEB-PREVIEW', status: 'PASS', actual: `Preview responde en ${preview.url}.`, log: previewLog });
    report.progress.phase = 'preview-ready';
    await writeProgressReport(report, directory);
    const served = await verifyServedArtifact(manifest, preview.url);
    report.artifact.served = served.status;
    report.artifact.serveDetails = served;
    report.steps.push({ id: 'WEB-ARTIFACT-SERVED', status: served.status, actual: served.reason ?? served.mismatches.join('; '), log: previewLog });
    report.progress.phase = served.status === 'PASS' ? 'artifact-served' : 'artifact-serve-failed';
    await writeProgressReport(report, directory);
    if (served.status !== 'PASS') {
      report.steps.push({ id: 'WEB-BROWSER-SUITES', status: 'NOT_RUN', reason: 'El servidor no devolvió byte por byte los archivos del build Wasm.' });
      await writeProgressReport(report, directory);
      return;
    }

    const browser = await browserVersion();
    report.environment.chrome = browser.version ?? null;
    report.steps.push({ id: 'WEB-CHROME', status: browser.status, actual: browser.version ?? browser.reason });
    report.progress.phase = 'browser-ready';
    await writeProgressReport(report, directory);
    const testEnv = { ...process.env, FINSIM_WEB_URL: preview.url, CI: '1' };
    const suiteSpecs = [
      { name: 'npm test', command: 'npm', args: ['test'], log: 'npm-test.log' },
      { name: 'npm run test:e2e', command: 'npm', args: ['run', 'test:e2e'], log: 'test-e2e.log' },
    ];
    for (let index = 0; index < suiteSpecs.length; index += 1) {
      const spec = suiteSpecs[index];
      const suite = report.suites[index];
      suite.command = commandText(spec.command, spec.args);
      suite.log = spec.log;
      report.evidenceFiles.push({ path: spec.log, sha256: null });
      report.progress.phase = 'suite-starting';
      report.progress.activeSuite = spec.name;
      report.progress.activeCase = null;
      await writeProgressReport(report, directory);
      let lastProgressKey = '';
      let progressWrites = Promise.resolve();
      const result = await runProcess(spec.command, spec.args, path.join(directory, spec.log), {
        env: testEnv,
        onOutput: output => {
          const observedNow = parseTapResults(output);
          const activeCase = [...output.matchAll(/^\s*# Subtest: (.+?)\s*$/gm)].at(-1)?.[1] ?? null;
          const progressKey = `${observedNow.length}\0${activeCase ?? ''}`;
          if (progressKey === lastProgressKey) return;
          lastProgressKey = progressKey;
          suite.results = observedNow;
          suite.cases = evaluateWebCases(discovered.cases, observedNow);
          suite.passed = observedNow.filter(item => item.status === 'PASS').length;
          suite.failed = observedNow.filter(item => item.status === 'FAIL').length;
          suite.notRun = observedNow.filter(item => item.status === 'NOT_RUN').length;
          report.progress.activeCase = activeCase;
          report.progress.completedResults = observedNow.length;
          refreshCatalogResults(report, discovered);
          progressWrites = progressWrites.then(() => writeProgressReport(report, directory)).catch(error => {
            report.progress.persistenceError = error instanceof Error ? error.message : String(error);
          });
        },
      });
      await progressWrites;
      const observed = parseTapResults(result.output);
      suite.results = observed;
      suite.cases = evaluateWebCases(discovered.cases, observed);
      suite.passed = observed.filter(item => item.status === 'PASS').length;
      suite.failed = observed.filter(item => item.status === 'FAIL').length;
      suite.notRun = observed.filter(item => item.status === 'NOT_RUN').length;
      suite.status = statusOf(result);
      suite.exitCode = result.exitCode;
      suite.signal = result.signal;
      suite.reason = result.error?.message ?? (result.exitCode === 0 ? null : result.output.slice(-2000));
      report.steps.push({
        id: index === 0 ? 'WEB-NPM-TEST' : 'WEB-NPM-E2E',
        status: suite.status,
        actual: `exit ${result.exitCode}; ${suite.passed} PASS, ${suite.failed} FAIL, ${suite.notRun} NOT_RUN; ${suite.cases.filter(item => item.status === 'PASS').length}/${suite.cases.length} casos catalogados identificados.`,
        ...(suite.reason ? { reason: suite.reason } : {}),
        log: spec.log,
      });
      report.progress.activeSuite = null;
      report.progress.activeCase = null;
      report.progress.completedResults = observed.length;
      report.progress.phase = 'suite-complete';
      refreshCatalogResults(report, discovered);
      await writeProgressReport(report, directory);
    }

    const firstTitles = new Set(report.suites[0].results.map(item => item.title));
    report.intersection.titles = [...new Set(report.suites[1].results.map(item => item.title).filter(title => firstTitles.has(title)))].sort();
    report.intersection.titleCount = report.intersection.titles.length;
    refreshCatalogResults(report, discovered);
    report.progress.phase = 'browser-suites-complete';
    await writeProgressReport(report, directory);
  } finally {
    await preview.stop?.();
    report.server.status = preview.ready ? 'STOPPED' : report.server.status;
  }
}

async function loadReusableBuild(referencePath, source) {
  const absolutePath = path.resolve(referencePath);
  const bytes = await readFile(absolutePath);
  const previous = JSON.parse(bytes.toString('utf8'));
  const currentRelativePath = path.relative(root, distRoot).replaceAll(path.sep, '/');
  if (previous.artifact?.path !== currentRelativePath || !previous.artifact.sha256) throw new Error('La ruta o identidad del artefacto no coincide con el build referenciado.');
  const manifest = await artifactManifest(distRoot);
  const previousFinishedAt = Date.parse(previous.finishedAt);
  if (!Number.isFinite(previousFinishedAt)) throw new Error('El reporte de build no tiene un cierre temporal verificable.');
  const candidates = [
    ...git(['diff', '--name-only', 'HEAD', '--'], { encoding: 'utf8' }).split(/\r?\n/).filter(Boolean),
    ...git(['ls-files', '--others', '--exclude-standard'], { encoding: 'utf8' }).split(/\r?\n/).filter(Boolean),
  ];
  const changedSinceBuild = [];
  for (const file of [...new Set(candidates)].sort()) {
    try {
      const info = await stat(path.join(root, file));
      if (info.mtimeMs > previousFinishedAt) changedSinceBuild.push(file.replaceAll(path.sep, '/'));
    } catch {
      changedSinceBuild.push(file.replaceAll(path.sep, '/'));
    }
  }
  const buildInputChangesSince = changedSinceBuild.filter(isBuildInputPath);
  const previousManifestPath = path.resolve(previous.evidenceDirectory, previous.artifact.manifest ?? 'wasm-manifest.json');
  const previousManifestBytes = await readFile(previousManifestPath);
  const previousManifest = JSON.parse(previousManifestBytes.toString('utf8'));
  const previousManifestEvidence = previous.evidenceFiles?.find(item => item.path === previous.artifact.manifest);
  if (!previousManifestEvidence || hashBytes(previousManifestBytes) !== previousManifestEvidence.sha256) {
    throw new Error('El manifiesto guardado del build no coincide con su hash de evidencia.');
  }
  if (JSON.stringify(previousManifest) !== JSON.stringify(manifest)) {
    throw new Error('Los hashes individuales del artefacto actual no coinciden con el manifiesto original del build.');
  }
  const reuseErrors = validateReusableBuild({ previous, current: source, changedSinceBuild, buildInputChangesSince, artifactHash: manifest.sha256 });
  if (reuseErrors.length) throw new Error(reuseErrors.join(' '));
  return {
    manifest,
    previous,
    reportPath: absolutePath,
    reportSha256: hashBytes(bytes),
    changedSinceBuild,
    buildInputChangesSince,
    workspaceHashMatches: previous.source.workingTree.sha256 === source.workingTree.sha256,
  };
}

function isBuildInputPath(file) {
  const normalized = file.replaceAll('\\', '/');
  if (normalized === 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs') return false;
  return normalized === 'package.json' || normalized === 'package-lock.json' ||
    normalized === 'settings.gradle.kts' || normalized === 'build.gradle.kts' ||
    normalized === 'gradle.properties' || normalized.startsWith('gradle/') ||
    normalized === 'gradlew' || normalized === 'gradlew.bat' || normalized.startsWith('kmp/');
}

async function main() {
  const startedAt = new Date();
  const runId = `${startedAt.toISOString().replaceAll(':', '-').replaceAll('.', '-')}-${randomUUID().slice(0, 8)}`;
  const directory = path.join(reportRoot, runId);
  await mkdir(directory, { recursive: true });

  const source = await sourceIdentity();
  const discovered = await loadWebCatalogs();
  const catalogErrors = validateWebCatalog(discovered.cases);
  const report = {
    schemaVersion: 1,
    runId,
    startedAt: startedAt.toISOString(),
    executionState: 'INCOMPLETE',
    progress: { phase: 'initializing', activeSuite: null, activeCase: null, completedResults: 0, updatedAt: startedAt.toISOString() },
    evidenceDirectory: directory,
    profile: { id: 'WEB', name: 'Real Wasm browser verification', scope: 'build WasmJs real y ejecutar recorridos de navegador en escritorio y viewport táctil' },
    source,
    environment: { os: `${os.type()} ${os.release()}`, arch: os.arch(), node: process.version, gradleVersion: null, jvm: null, chrome: null },
    build: { command: null, status: 'NOT_RUN', exitCode: null },
    artifact: { path: path.relative(root, distRoot).replaceAll(path.sep, '/'), sha256: null, files: 0, served: 'NOT_RUN' },
    server: { url: null, command: null, status: 'NOT_RUN' },
    catalog: { files: discovered.files, cases: discovered.cases.map(item => ({ ...item, status: 'NOT_RUN', actual: 'El caso aún no se ha ejecutado.' })) },
    steps: [],
    suites: [emptySuite('npm test', 'npm-test.log'), emptySuite('npm run test:e2e', 'test-e2e.log')],
    coverageNotRun: [{
      criterion: 'Interacción en APK, dispositivo Android y lector de pantalla', status: 'NOT_RUN', platform: 'Android / hardware',
      reason: 'Esta ejecución construye y prueba la distribución Wasm en Chrome; el viewport táctil emula el tamaño e input web, no ejecuta APK ni certifica hardware o lector de pantalla.',
      resume: 'Verificar por separado el perfil Android cuando la implementación/runtime y un dispositivo de prueba estén disponibles; dejar el hardware físico identificado.',
    }],
    evidenceFiles: [],
    intersection: { titleCount: 0, titles: [], explanation: 'Las dos órdenes ejecutan la suite browser; npm test añade las pruebas Node del verificador.' },
  };

  await writeProgressReport(report, directory);
  try {
    if (catalogErrors.length) {
      report.steps.push({ id: 'WEB-CATALOG', status: 'FAIL', actual: catalogErrors.join(' ') });
    } else {
      report.steps.push({ id: 'WEB-CATALOG', status: 'PASS', actual: `${discovered.cases.length} casos cargados de ${discovered.files.length} catálogos; IDs y títulos únicos.` });
    }
    report.progress.phase = 'catalog-validated';
    await writeProgressReport(report, directory);

    const reuseReportPath = process.env.FINSIM_WEB_REUSE_BUILD_REPORT;
    const dependencies = await nodeDependencies();
    report.steps.push(dependencies.ready
      ? { id: 'WEB-NODE-PREFLIGHT', status: 'PASS', actual: 'Vite preview y Playwright están instalados.' }
      : { id: 'WEB-NODE-PREFLIGHT', status: 'BLOCKED', reason: `Faltan dependencias: ${dependencies.missing.join(', ')}.` });
    report.progress.phase = 'node-preflight';
    await writeProgressReport(report, directory);

    if (!dependencies.ready || catalogErrors.length) {
      if (reuseReportPath) {
        report.steps.push({ id: 'WEB-BUILD-REUSE', status: 'NOT_RUN', reason: 'El preflight bloqueó la ruta de navegador antes de reutilizar el build.' });
      } else {
        report.steps.push({ id: 'WEB-TOOLCHAIN', status: 'NOT_RUN', reason: 'El preflight bloqueó el build antes de iniciar Gradle.' });
        report.build = { command: null, status: 'NOT_RUN', exitCode: null };
      }
      report.steps.push({ id: 'WEB-BROWSER-SUITES', status: 'NOT_RUN', reason: 'Faltan dependencias de preview o los catálogos no pasaron validación.' });
      report.progress.phase = 'node-preflight-blocked';
      await writeProgressReport(report, directory);
    } else if (reuseReportPath) {
      try {
          const reusable = await loadReusableBuild(reuseReportPath, source);
          const previous = reusable.previous;
          report.environment.gradleVersion = previous.environment.gradleVersion;
          report.environment.jvm = previous.environment.jvm;
          report.build = {
            command: previous.build.command,
            status: 'PASS',
            exitCode: 0,
            reused: true,
            reusedFrom: reusable.reportPath,
            referenceReportSha256: reusable.reportSha256,
            identity: {
              buildCommit: previous.source.commit,
              buildWorkingTreeSha256: previous.source.workingTree.sha256,
              verifierCommit: source.commit,
              verifierWorkingTreeSha256: source.workingTree.sha256,
              workspaceHashMatches: reusable.workspaceHashMatches,
              changedSinceBuild: reusable.changedSinceBuild,
              buildInputChangesSince: reusable.buildInputChangesSince,
              referenceReport: reusable.reportPath,
              referenceReportSha256: reusable.reportSha256,
            },
          };
          report.steps.push({
            id: 'WEB-TOOLCHAIN-REUSE', status: 'PASS',
            actual: `Reporte de build confirma Gradle ${previous.environment.gradleVersion} y JVM ${previous.environment.jvm}.`,
          });
          report.steps.push({
            id: 'WEB-BUILD-REUSE', status: 'PASS',
            actual: `Artefacto del build ${previous.source.workingTree.sha256} reutilizado. Solo cambió ${reusable.changedSinceBuild.join(', ') || 'ningún archivo'}; los insumos de producto/build/dependencias permanecen iguales. Hash del reporte original ${reusable.reportSha256}.`,
          });
          report.progress.phase = 'build-reuse-validated';
          await writeProgressReport(report, directory);
          await runBrowserChecks(report, directory, discovered, reusable.manifest);
      } catch (error) {
          report.build.status = 'NOT_RUN';
          report.build.reuseValidation = 'FAIL';
          report.steps.push({ id: 'WEB-BUILD-REUSE', status: 'FAIL', actual: error instanceof Error ? error.message : String(error) });
          report.steps.push({ id: 'WEB-BROWSER-SUITES', status: 'NOT_RUN', reason: 'La validación rechazó el build referenciado; no se inició preview ni se ejecutaron las suites.' });
          report.progress.phase = 'build-reuse-rejected';
          await writeProgressReport(report, directory);
      }
    } else {
    const gradlePath = 'C:/Users/Lenovo/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle.bat';
    const javaHome = await stat(preferredJdk).then(() => preferredJdk, () => null);
    const gradleEnv = { ...process.env };
    if (javaHome) {
      gradleEnv.JAVA_HOME = javaHome;
      gradleEnv.PATH = `${path.join(javaHome, 'bin')}${path.delimiter}${process.env.PATH ?? ''}`;
    }
    const versionLog = 'gradle-version.log';
    report.evidenceFiles.push({ path: versionLog, sha256: null });
    const version = await runProcess(gradlePath, ['--version'], path.join(directory, versionLog), { env: gradleEnv, echo: false });
    const versionText = version.output;
    const gradleVersion = versionText.match(/^Gradle\s+(\S+)/m)?.[1] ?? null;
    const jvm = versionText.match(/^(?:Launcher JVM|JVM|Daemon JVM):\s*(.+)$/m)?.[1]?.trim() ?? null;
    report.environment.gradleVersion = gradleVersion;
    report.environment.jvm = jvm;
    if (version.exitCode !== 0 || !gradleVersion || !jvm) {
      const status = version.error?.code === 'ENOENT' || version.exitCode === null ? 'BLOCKED' : 'FAIL';
      report.steps.push({ id: 'WEB-TOOLCHAIN', status, reason: version.error?.message ?? version.output.slice(-2000), log: versionLog });
      report.build = { command: commandText(gradlePath, ['--version']), status: 'NOT_RUN', exitCode: null };
    } else if (gradleVersion !== expectedGradle || !/^21(?:\.|$)/.test(jvm)) {
      report.steps.push({ id: 'WEB-TOOLCHAIN', status: 'FAIL', actual: `Se requiere Gradle ${expectedGradle} y JDK 21; se detectó Gradle ${gradleVersion}, JVM ${jvm}.`, log: versionLog });
      report.build = { command: commandText(gradlePath, ['--version']), status: 'NOT_RUN', exitCode: null };
    } else {
      report.steps.push({ id: 'WEB-TOOLCHAIN', status: 'PASS', actual: `Gradle ${gradleVersion}; ${jvm}.`, log: versionLog });
      const gradleArgs = [
        '--no-daemon', '--max-workers=1', '-Pkotlin.compiler.execution.strategy=in-process',
        '-Dorg.gradle.jvmargs=-Xmx2g', '-p', 'kmp', ':composeApp:wasmJsBrowserDistribution', '--rerun-tasks',
      ];
      report.build.command = commandText(gradlePath, gradleArgs);
      const buildLog = 'wasm-build.log';
      report.evidenceFiles.push({ path: buildLog, sha256: null });
      const buildStartedAt = Date.now();
      report.progress.phase = 'wasm-build-running';
      await writeProgressReport(report, directory);
      const build = await runProcess(gradlePath, gradleArgs, path.join(directory, buildLog), { env: gradleEnv });
      report.build.exitCode = build.exitCode;
      report.build.status = statusOf(build);
      report.build.signal = build.signal;
      report.build.reason = build.error?.message ?? (build.exitCode === 0 ? null : build.output.slice(-2000));
      if (build.exitCode === 0) {
        try {
          const info = await stat(path.join(distRoot, 'index.html'));
          if (!info.isFile() || info.mtimeMs < buildStartedAt - 1000) throw new Error('index.html no pertenece a una distribución recién construida.');
          const manifest = await artifactManifest(distRoot);
          report.artifact.sha256 = manifest.sha256;
          report.artifact.files = manifest.files;
          report.steps.push({ id: 'WEB-BUILD', status: 'PASS', actual: `La tarea de distribución Wasm terminó con exit 0 y generó ${manifest.files} archivos; árbol SHA-256 ${manifest.sha256}.`, log: buildLog });
          report.progress.phase = 'wasm-build-complete';
          await writeProgressReport(report, directory);
          await runBrowserChecks(report, directory, discovered, manifest);
        } catch (error) {
          report.steps.push({ id: 'WEB-DISTRIBUTION', status: 'FAIL', actual: error instanceof Error ? error.message : String(error), log: buildLog });
          report.progress.phase = 'distribution-failed';
          await writeProgressReport(report, directory);
        }
      } else {
        report.steps.push({ id: 'WEB-BUILD', status: report.build.status, reason: report.build.reason, log: buildLog });
        report.progress.phase = 'wasm-build-failed';
        await writeProgressReport(report, directory);
      }
    }

    }

    if (!report.suites.some(suite => suite.status !== 'NOT_RUN') && !report.steps.some(step => step.id === 'WEB-BROWSER-SUITES')) {
      report.steps.push({ id: 'WEB-BROWSER-SUITES', status: 'NOT_RUN', reason: 'Las suites no se ejecutaron porque no hubo una distribución Wasm recién construida y verificada.' });
    }
  } catch (error) {
    report.steps.push({ id: 'WEB-RUNNER', status: 'FAIL', actual: error instanceof Error ? error.stack ?? error.message : String(error) });
  } finally {
    report.server.status = report.server.status === 'PASS' ? 'STOPPED' : report.server.status;
    process.exitCode = await writeReport(report, directory);
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => {
    console.error(error);
    process.exitCode = 1;
  });
}
