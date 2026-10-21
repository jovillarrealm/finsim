import { spawn, spawnSync, execFileSync } from 'node:child_process';
import { createHash, randomUUID } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { lstat, mkdir, readFile, readlink, stat, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { summarizeVerification } from './verification-status.mjs';

export const PACKAGE_NAME = 'io.github.jovillarrealm.finsim_alpha';
export const FINANCIAL_REFERENCES = [
  {
    id: 'ANDROID-LOAN-ORIGINAL',
    expected: 'Capital 1000.00, EM 1%, 2 meses: cuota 507.51; M1 interés 10.00, capital 497.51, saldo 502.49; M2 interés 5.02 y pago final 507.51.',
    source: 'docs/financial-conventions.md, sección 5 (Cuota con interés)',
  },
  {
    id: 'ANDROID-LOAN-CURRENT',
    expected: 'Saldo actual 502.49, EM 1%, 1 mes: interés 5.02, capital 502.49 y pago final 507.51.',
    source: 'Derivación directa del saldo y la última cuota de la referencia Cuota con interés; debe cotejarse en APK.',
  },
  {
    id: 'ANDROID-INSURANCE',
    expected: 'Capital 1000.00, tasa 0%, seguro 0.1%, cuota base 100 y extra 1000: seguro 1.00, pago habitual 101.00, extra aplicado 900.00, no aplicado 100.00, caja 1001.00 y saldo 0.',
    source: 'docs/financial-conventions.md, sección 5 (Base del seguro y liquidación)',
  },
  {
    id: 'ANDROID-EVENTS',
    expected: 'Capital 1000.00, EM 1%, cuota 100, seguro fijo 5; omisión M1 y recuperación 105 en M2: seguro pagado 10.00, interés pagado 20.00, capital pagado 180.00 y saldo 820.00.',
    source: 'docs/financial-conventions.md, sección 5 (Omisión con recuperación completa)',
  },
  {
    id: 'ANDROID-RATE',
    expected: 'EM 0.01 equivale a EA 0.126825030131969720661201; en pantalla 12.6825030131969720661201% (tolerancia absoluta 1e-12).',
    source: 'docs/financial-conventions.md, sección 6 (referencia exacta de tasas)',
  },
  {
    id: 'ANDROID-RECONCILIATION',
    expected: 'Fila M1 del caso de préstamo: interés 10.00, seguro 0.00, capital 497.51 y saldo final 502.49; las cuatro diferencias deben ser cero.',
    source: 'docs/financial-conventions.md, sección 5 (Cuota con interés), reconciliación derivada de sus valores literales.',
  },
  {
    id: 'ANDROID-OFFERS',
    expected: 'Misma compra: A precio 900, aporte 200, capital 700, 0% y 2 cuotas produce total 910/costo financiero 10; B precio 1000, 4 cuotas de 250 produce total 1000/costo 0; diferencia de total 90.',
    source: 'kmp/shared/src/commonTest/kotlin/com/finsim/comparison/ComparisonWorkspaceTest.kt, samePurchaseSeparatesPriceInitialOutlayAndFinanceCost.',
  },
];

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const reportRoot = path.join(os.homedir(), '.finsim', 'verification', 'android');
const tabs = ['Préstamo', 'Conversor', 'Conciliación', 'Ofertas'];

export function parseOptions(args) {
  const options = { device: null, apk: null, allowUiActions: false, startApp: false, restartApp: false, help: false };
  for (let i = 0; i < args.length; i += 1) {
    const arg = args[i];
    if (arg === '--device' || arg === '--apk') {
      const value = args[++i];
      if (!value || value.startsWith('--')) throw new Error(`${arg} requiere un valor.`);
      options[arg === '--device' ? 'device' : 'apk'] = value;
    } else if (arg === '--allow-ui-actions') {
      options.allowUiActions = true;
    } else if (arg === '--start-app') {
      options.startApp = true;
    } else if (arg === '--restart-app') {
      options.restartApp = true;
    } else if (arg === '--help' || arg === '-h') {
      options.help = true;
    } else {
      throw new Error(`Opción desconocida: ${arg}`);
    }
  }
  if (options.allowUiActions && !options.device) throw new Error('--allow-ui-actions requiere --device <serial>.');
  if (options.startApp && (!options.device || !options.allowUiActions)) throw new Error('--start-app requiere --device y --allow-ui-actions explícitos.');
  if (options.restartApp && (!options.device || !options.allowUiActions)) throw new Error('--restart-app requiere --device y --allow-ui-actions explícitos.');
  if (options.startApp && options.restartApp) throw new Error('Usa --start-app o --restart-app, no ambos.');
  if (options.apk && !options.device) throw new Error('--apk requiere --device <serial> para comparar el candidato con el APK instalado.');
  return options;
}

export function chooseDevice(devices, serial) {
  return serial ? devices.find(device => device.serial === serial) ?? null : null;
}

function decodeXml(value) {
  return value.replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&quot;/g, '"').replace(/&apos;|&#39;/g, "'")
    .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
}

function parseNodeAttributes(raw) {
  const attrs = Object.fromEntries([...raw.matchAll(/([\w:-]+)="([^"]*)"/g)].map(([, key, value]) => [key, decodeXml(value)]));
  const bounds = attrs.bounds?.match(/^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$/);
  const [left, top, right, bottom] = bounds ? bounds.slice(1).map(Number) : [0, 0, 0, 0];
  return {
    text: attrs.text ?? '', contentDescription: attrs['content-desc'] ?? '', resourceId: attrs['resource-id'] ?? '',
    hint: attrs.hint ?? '', className: attrs.class ?? '', clickable: attrs.clickable === 'true',
    selected: attrs.selected === 'true', enabled: attrs.enabled !== 'false', focusable: attrs.focusable === 'true',
    focused: attrs.focused === 'true', editable: attrs.editable === 'true' || /EditText/.test(attrs.class ?? ''),
    scrollable: attrs.scrollable === 'true', visibleToUser: attrs['visible-to-user'] !== 'false',
    bounds: { left, top, right, bottom, width: right - left, height: bottom - top }, parent: null, children: [],
  };
}

export function parseHierarchy(xml) {
  const rootNode = { parent: null, children: [] };
  const stack = [rootNode];
  for (const match of xml.matchAll(/<node\b([^>]*?)(\/?)>|<\/node>/g)) {
    const token = match[0];
    if (token.startsWith('</')) {
      if (stack.length > 1) stack.pop();
      continue;
    }
    const node = parseNodeAttributes(match[1]);
    node.parent = stack.at(-1);
    node.parent.children.push(node);
    if (match[2] !== '/') stack.push(node);
  }
  const nodes = [];
  const visit = node => { for (const child of node.children) { nodes.push(child); visit(child); } };
  visit(rootNode);
  return nodes;
}

export function touchTargetDp(bounds, densityDpi) {
  if (!Number.isFinite(densityDpi) || densityDpi <= 0) throw new Error('La densidad Android debe ser un número positivo.');
  const scale = 160 / densityDpi;
  const width = bounds.width * scale;
  const height = bounds.height * scale;
  return { width, height, passes: width >= 48 && height >= 48 };
}

export function isVisibleInDisplay(node, size) {
  return node.visibleToUser && !!size && node.bounds.width > 0 && node.bounds.height > 0
    && node.bounds.right > 0 && node.bounds.bottom > 0 && node.bounds.left < size.width && node.bounds.top < size.height;
}

export function evaluateFinancialObservation({ actionPerformed, observedText, expected, tolerance = 0 }) {
  if (!actionPerformed) {
    return { status: 'NOT_RUN', observed: observedText ?? null, parsed: null, expected, actual: 'No se ejecutó la acción financiera de referencia.' };
  }
  const parsed = typeof observedText === 'string' ? parseNumericText(observedText) : null;
  if (parsed === null) {
    return { status: 'FAIL', observed: observedText ?? null, parsed, expected, actual: 'La acción se ejecutó, pero no se observó una cifra numérica.' };
  }
  const status = Math.abs(parsed - expected) <= tolerance ? 'PASS' : 'FAIL';
  return { status, observed: observedText, parsed, expected, actual: `${observedText} (parseado ${parsed})` };
}

export function compareInstalledApkCandidate(candidate, installedApks) {
  if (!candidate) return {
    status: 'NOT_RUN', expected: 'El artefacto candidato coincide byte a byte con la instalación.',
    actual: 'No se proporcionó --apk; la instalación se identificó sin atribuirla al código local.',
  };
  if (!Array.isArray(installedApks) || installedApks.length === 0) {
    return { status: 'BLOCKED', expected: 'Hay al menos una ruta APK instalada para comparar.', actual: 'No hay APK instalados identificados.' };
  }
  const baseMatches = candidate.sha256 === installedApks[0].sha256;
  if (installedApks.length > 1) {
    return {
      status: 'NOT_RUN', baseMatches,
      expected: 'El artefacto candidato representa el conjunto instalado completo, incluida cada división.',
      actual: `Se identificaron ${installedApks.length} APK instalados; el candidato solo coincide con el APK base=${baseMatches}; no se afirmó igualdad del conjunto.`,
    };
  }
  return {
    status: baseMatches ? 'PASS' : 'FAIL', baseMatches,
    expected: `SHA-256 candidato ${candidate.sha256} coincide con el APK instalado.`,
    actual: baseMatches ? 'Coincidencia byte a byte; el commit de construcción del candidato no queda demostrado por el hash.' : `Diferencia: instalado ${installedApks[0].sha256}, candidato ${candidate.sha256}.`,
  };
}

export function foregroundPackageFromDumpsys(output) {
  const patterns = [
    /mCurrentFocus=Window\{[^}]*?\s([^\s/]+)\//,
    /mFocusedApp=ActivityRecord\{[^}]*?\s([^\s/]+)\//,
    /topResumedActivity=ActivityRecord\{[^}]*?\s([^\s/]+)\//,
    /ResumedActivity:\s*ActivityRecord\{[^}]*?\s([^\s/]+)\//,
  ];
  for (const pattern of patterns) {
    const match = output.match(pattern);
    if (match) return match[1];
  }
  return null;
}

export function parseActivityStart(output) {
  const status = output.match(/^Status:\s*(\S+)/mi)?.[1] ?? null;
  const activity = output.match(/^Activity:\s*(\S+)/mi)?.[1] ?? null;
  const launchState = output.match(/^LaunchState:\s*(\S+)/mi)?.[1] ?? null;
  const totalTimeMs = Number(output.match(/^TotalTime:\s*(\d+)/mi)?.[1]);
  return { status, activity, launchState, totalTimeMs: Number.isFinite(totalTimeMs) ? totalTimeMs : null };
}

export function parseMemoryInfo(output) {
  const totalKb = Number(output.match(/^MemTotal:\s+(\d+)\s+kB$/m)?.[1]);
  if (!Number.isFinite(totalKb) || totalKb <= 0) return null;
  const totalBytes = totalKb * 1024;
  return { memTotalKb: totalKb, memTotalBytes: totalBytes, memTotalGiB: Number((totalBytes / (1024 ** 3)).toFixed(2)) };
}

export function parseMemoryPressure(output) {
  const result = {};
  for (const line of output.split(/\r?\n/).filter(Boolean)) {
    const [scope, ...fields] = line.trim().split(/\s+/);
    if (scope !== 'some' && scope !== 'full') continue;
    const metrics = Object.fromEntries(fields.map(field => {
      const [key, value] = field.split('=');
      return [key, Number(value)];
    }).filter(([, value]) => Number.isFinite(value)));
    result[scope] = metrics;
  }
  return Object.keys(result).length ? result : null;
}

function adb(serial, args, timeout = 15000) {
  const fullArgs = serial ? ['-s', serial, ...args] : args;
  const result = spawnSync('adb', fullArgs, { cwd: root, encoding: 'utf8', timeout, windowsHide: true, maxBuffer: 8 * 1024 * 1024 });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error((result.stderr || result.stdout || `adb terminó con estado ${result.status}`).trim());
  return result.stdout.trim();
}

function git(args, options = {}) {
  return execFileSync('git', args, { cwd: root, ...options });
}

async function sourceIdentity() {
  try {
    const diff = git(['diff', '--binary', 'HEAD', '--']);
    const untracked = git(['ls-files', '--others', '--exclude-standard', '-z'], { encoding: 'utf8' }).split('\0').filter(Boolean).sort();
    const hash = createHash('sha256').update(diff).update('\0');
    for (const relativePath of untracked) {
      const absolute = path.resolve(root, relativePath);
      const relative = path.relative(root, absolute);
      if (relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) throw new Error('Ruta local fuera del repositorio.');
      const metadata = await lstat(absolute);
      hash.update(relativePath).update('\0');
      if (metadata.isSymbolicLink()) hash.update('symlink:').update(await readlink(absolute));
      else if (metadata.isFile()) hash.update(await readFile(absolute));
      hash.update('\0');
    }
    const status = git(['status', '--porcelain', '--untracked-files=no'], { encoding: 'utf8' }).trim();
    return {
      repository: git(['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim(),
      branch: git(['branch', '--show-current'], { encoding: 'utf8' }).trim() || 'DETACHED',
      commit: git(['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
      workingTree: { hasLocalChanges: diff.length > 0 || untracked.length > 0 || status.length > 0, sha256: hash.digest('hex'), trackedFilesChanged: status ? status.split(/\r?\n/).length : 0, untrackedFiles: untracked.length },
    };
  } catch (error) {
    return { error: error instanceof Error ? error.message : String(error) };
  }
}

function parseDevices(output) {
  return output.split(/\r?\n/).slice(1).filter(line => line.trim()).map(line => {
    const [serial, state, ...details] = line.trim().split(/\s+/);
    return { serial, state, details: details.join(' ') };
  });
}

function deviceFingerprint(serial) {
  return createHash('sha256').update(serial).digest('hex').slice(0, 16);
}

function deviceFact(serial, property) {
  return adb(serial, ['shell', 'getprop', property]);
}

async function hashFile(filePath) {
  const hash = createHash('sha256');
  let bytes = 0;
  for await (const chunk of createReadStream(filePath)) { hash.update(chunk); bytes += chunk.length; }
  return { sha256: hash.digest('hex'), bytes };
}

async function readCandidateMetadata(candidateApk, candidateHash) {
  if (!candidateApk) return null;
  const file = path.join(path.dirname(candidateApk), 'identity.json');
  try {
    const metadata = JSON.parse(await readFile(file, 'utf8'));
    return {
      file: path.basename(file),
      matchesApkHash: typeof metadata.apkSha256 === 'string' && metadata.apkSha256.toLowerCase() === candidateHash.sha256,
      sourceCommit: metadata.sourceCommit ?? null,
      implementationCommit: metadata.implementationCommit ?? null,
      applicationId: metadata.applicationId ?? null,
      versionName: metadata.versionName ?? null,
      versionCode: metadata.versionCode ?? null,
      certificateSha256: metadata.certificateSha256 ?? null,
    };
  } catch (error) {
    if (error?.code === 'ENOENT') return null;
    return { file: path.basename(file), metadataError: error instanceof Error ? error.message : String(error) };
  }
}

function hashRemoteFile(serial, remotePath) {
  return new Promise((resolve, reject) => {
    const child = spawn('adb', ['-s', serial, 'exec-out', 'cat', remotePath], { cwd: root, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
    const hash = createHash('sha256');
    let bytes = 0;
    let stderr = '';
    const timeout = setTimeout(() => child.kill(), 30000);
    child.stdout.on('data', chunk => { hash.update(chunk); bytes += chunk.length; });
    child.stderr.on('data', chunk => { stderr += chunk.toString('utf8'); });
    child.on('error', error => { clearTimeout(timeout); reject(error); });
    child.on('close', code => {
      clearTimeout(timeout);
      if (code === 0) resolve({ sha256: hash.digest('hex'), bytes });
      else reject(new Error(stderr.trim() || `No se pudo leer el APK instalado (exit ${code}).`));
    });
  });
}

function getDeviceDisplay(serial) {
  const sizeText = adb(serial, ['shell', 'wm', 'size']);
  const densityText = adb(serial, ['shell', 'wm', 'density']);
  const sizeMatches = [...sizeText.matchAll(/(?:Physical|Override) size:\s*(\d+)x(\d+)/g)];
  const densityMatches = [...densityText.matchAll(/(?:Physical|Override) density:\s*(\d+)/g)];
  const size = sizeMatches.at(-1);
  const density = densityMatches.at(-1);
  return {
    size: size ? { width: Number(size[1]), height: Number(size[2]) } : null,
    densityDpi: density ? Number(density[1]) : null,
  };
}

function readDeviceMemory(serial) {
  let memory = null;
  let memoryError = null;
  try { memory = parseMemoryInfo(adb(serial, ['shell', 'cat', '/proc/meminfo'])); }
  catch (error) { memoryError = error instanceof Error ? error.message : String(error); }
  let pressure = null;
  let pressureError = null;
  try { pressure = parseMemoryPressure(adb(serial, ['shell', 'cat', '/proc/pressure/memory'])); }
  catch (error) { pressureError = error instanceof Error ? error.message : String(error); }
  return { ...memory, pressure, memoryError, pressureError };
}

async function identifyDevice(serial, candidateApk) {
  const state = adb(serial, ['get-state']);
  if (state !== 'device') throw new Error(`ADB target no está listo: ${state}.`);
  const androidUserId = adb(serial, ['shell', 'am', 'get-current-user']);
  if (!/^\d+$/.test(androidUserId)) throw new Error('No se pudo identificar el usuario Android activo para localizar la instalación.');
  const installedPackages = adb(serial, ['shell', 'pm', 'list', 'packages', '--user', androidUserId, PACKAGE_NAME]);
  if (!installedPackages.split(/\r?\n/).some(line => line.trim() === `package:${PACKAGE_NAME}`)) {
    throw new Error(`No se encontró ${PACKAGE_NAME} instalado para el usuario Android ${androidUserId}; este perfil no instala APK automáticamente.`);
  }
  const packagePaths = adb(serial, ['shell', 'pm', 'path', '--user', androidUserId, PACKAGE_NAME]).split(/\r?\n/).map(line => line.replace(/^package:/, '')).filter(Boolean);
  if (packagePaths.length === 0) throw new Error(`El paquete ${PACKAGE_NAME} aparece instalado para el usuario Android ${androidUserId}, pero no expuso rutas APK.`);
  const packageInfo = adb(serial, ['shell', 'dumpsys', 'package', PACKAGE_NAME]);
  const versionName = packageInfo.match(/\bversionName=([^\s]+)/)?.[1] ?? null;
  const versionCode = packageInfo.match(/\bversionCode=(\d+)/)?.[1] ?? null;
  const emu = deviceFact(serial, 'ro.kernel.qemu') === '1';
  const display = getDeviceDisplay(serial);
  const installedApks = await Promise.all(packagePaths.map(async packagePath => ({
    packagePath,
    ...await hashRemoteFile(serial, packagePath),
  })));
  const candidate = candidateApk ? await hashFile(candidateApk) : null;
  const candidateMetadata = await readCandidateMetadata(candidateApk, candidate);
  const candidateMatchesInstalledBase = candidate ? candidate.sha256 === installedApks[0].sha256 : null;
  const candidateMatchesInstalledApk = candidate && installedApks.length === 1 ? candidateMatchesInstalledBase : null;
  return {
    state,
    androidUserId,
    fingerprint: deviceFingerprint(serial),
    kind: emu ? 'emulator' : 'physical',
    manufacturer: deviceFact(serial, 'ro.product.manufacturer') || null,
    model: deviceFact(serial, 'ro.product.model') || null,
    androidVersion: deviceFact(serial, 'ro.build.version.release') || null,
    androidSdk: deviceFact(serial, 'ro.build.version.sdk') || null,
    display,
    memory: readDeviceMemory(serial),
    packageName: PACKAGE_NAME,
    versionName,
    versionCode,
    installedApks,
    installedApkCount: installedApks.length,
    candidateApk: candidate ? { file: path.basename(candidateApk), ...candidate } : null,
    candidateMetadata,
    candidateMatchesInstalledBase,
    candidateMatchesInstalledApk,
  };
}

function parseBoundsFromNode(node) {
  return node.bounds.width > 0 && node.bounds.height > 0;
}

function findNode(nodes, query, { exact = false } = {}) {
  const normalized = query.toLocaleLowerCase('es');
  return nodes.find(node => {
    const text = [node.text, node.contentDescription, node.resourceId, node.hint].join(' ').toLocaleLowerCase('es');
    return exact ? text.split(/\s+/).includes(normalized) || [node.text, node.contentDescription, node.hint].some(value => value.toLocaleLowerCase('es') === normalized) : text.includes(normalized);
  }) ?? null;
}

function findVisibleNode(nodes, query, display, options = {}) {
  return findNode(nodes.filter(node => isVisibleInDisplay(node, display)), query, options);
}

function getInteractiveTarget(node) {
  let current = node;
  while (current) {
    if (current.clickable || current.selected) return current;
    current = current.parent;
  }
  return node;
}

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const MAX_HIERARCHY_DUMPS = 16;
const MAX_LOAN_SWIPES = 2;
const MAX_PAGE_SWIPES = 1;

async function dumpHierarchy(serial, context) {
  if (context.dumps.length >= MAX_HIERARCHY_DUMPS) throw new Error(`Presupuesto de ${MAX_HIERARCHY_DUMPS} dumps de jerarquía agotado; se detiene la inspección sin gestos adicionales.`);
  const remotePath = `/data/local/tmp/finsim-verify-${context.runId}-${Date.now()}.xml`;
  const started = Date.now();
  const entry = { index: context.dumps.length + 1, startedAt: new Date().toISOString(), remoteFileRemoved: false };
  context.dumps.push(entry);
  try {
    adb(serial, ['shell', 'uiautomator', 'dump', remotePath], 45000);
    const xml = adb(serial, ['exec-out', 'cat', remotePath], 20000);
    entry.elapsedMs = Date.now() - started;
    entry.nodeCount = parseHierarchy(xml).length;
    entry.status = 'PASS';
    return parseHierarchy(xml);
  } catch (error) {
    entry.elapsedMs = Date.now() - started;
    entry.status = 'FAIL';
    entry.error = error instanceof Error ? error.message : String(error);
    throw error;
  } finally {
    try { adb(serial, ['shell', 'rm', '-f', remotePath]); entry.remoteFileRemoved = true; }
    catch { /* Preserve the original inspection error. */ }
  }
}

async function captureScreenshot(serial, context, label) {
  const safeLabel = label.toLocaleLowerCase('es').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  const file = `screen-${safeLabel || 'view'}.png`;
  const result = spawnSync('adb', ['-s', serial, 'exec-out', 'screencap', '-p'], { cwd: root, windowsHide: true, timeout: 15000, maxBuffer: 24 * 1024 * 1024 });
  if (result.error) throw result.error;
  if (result.status !== 0 || !result.stdout?.length) throw new Error((result.stderr || 'No se recibió la captura PNG.').toString().trim());
  const output = path.join(context.evidenceDirectory, file);
  await writeFile(output, result.stdout, { flag: 'wx' });
  const evidence = { label, file, bytes: result.stdout.length, sha256: createHash('sha256').update(result.stdout).digest('hex') };
  context.screenshots.push(evidence);
  return evidence;
}

function addCheck(report, check) {
  report.checks.push(check);
}

function pendingCheck(id, requirement, expected, reason, resume) {
  return { id, requirement, platform: 'APK Android', status: 'NOT_RUN', expected, actual: reason, resume };
}

function addMissingUiPending(report, reason) {
  const existing = new Set(report.checks.map(check => check.id));
  for (const check of uiPendingChecks(reason)) {
    if (!existing.has(check.id)) addCheck(report, check);
  }
}

function isAppForeground(serial) {
  const windows = adb(serial, ['shell', 'dumpsys', 'window', 'windows']);
  const focus = foregroundPackageFromDumpsys(windows)
    ?? foregroundPackageFromDumpsys(adb(serial, ['shell', 'dumpsys', 'activity', 'activities']));
  return { packageName: focus, app: focus === PACKAGE_NAME };
}

function startApp(serial) {
  const output = adb(serial, ['shell', 'am', 'start', '-W', '-n', `${PACKAGE_NAME}/com.finsim.app.MainActivity`], 30000);
  const parsed = parseActivityStart(output);
  return { ...parsed, output, succeeded: parsed.status?.toLowerCase() === 'ok' && parsed.activity?.endsWith('/com.finsim.app.MainActivity') };
}

function imeShown(serial) {
  const output = adb(serial, ['shell', 'dumpsys', 'input_method']);
  const match = output.match(/(?:mInputShown|isInputViewShown|mIsInputViewShown)=(true|false)/i);
  return match ? match[1].toLowerCase() === 'true' : null;
}

function parseNumericText(text) {
  const normalized = text.replace(/\s/g, '').replace(/%/g, '').replace(/\.(?=.*[,])/g, '').replace(',', '.');
  const match = normalized.match(/[+-]?\d+(?:\.\d+)?/);
  return match ? Number(match[0]) : null;
}

function scrollContainer(nodes) {
  return nodes.filter(node => node.scrollable && node.bounds.width > 0 && node.bounds.height > 0)
    .sort((left, right) => right.bounds.width * right.bounds.height - left.bounds.width * left.bounds.height)[0] ?? null;
}

function isDescendantOf(node, parent) {
  let current = node.parent;
  while (current) {
    if (current === parent) return true;
    current = current.parent;
  }
  return false;
}

function stableNodeKey(node) {
  return node.resourceId ? `id:${node.resourceId}` : `${node.className}|${node.text}|${node.contentDescription}|${node.hint}`;
}

function observedTab(nodes, tab, size) {
  return findVisibleNode(nodes, tab, size, { exact: true });
}

function tapObservedNode(serial, node, size) {
  if (!node || !isVisibleInDisplay(node, size)) throw new Error('El control ya no está dentro del viewport observado.');
  const target = getInteractiveTarget(node);
  if (!parseBoundsFromNode(target) || !isVisibleInDisplay(target, size)) throw new Error('El control observado no tiene bounds interactivos visibles.');
  adb(serial, ['shell', 'input', 'tap', String(Math.floor((target.bounds.left + target.bounds.right) / 2)), String(Math.floor((target.bounds.top + target.bounds.bottom) / 2))]);
}

async function swipeObservedContainer(serial, context, nodes, size, direction = 'down') {
  const container = scrollContainer(nodes);
  if (!container) return { status: 'BLOCKED', reason: 'La jerarquía observada no contiene un contenedor scrollable con bounds.' };
  const anchors = nodes.filter(node => isDescendantOf(node, container) && node.text.trim()
    && node.bounds.width > 0 && node.bounds.height > 0
    && node.bounds.left >= container.bounds.left && node.bounds.right <= container.bounds.right
    && node.bounds.top >= container.bounds.top && node.bounds.bottom <= container.bounds.bottom);
  if (anchors.length === 0) return { status: 'BLOCKED', reason: 'El contenedor observado no expone un ancla semántica visible para comparar posición.' };
  const x = Math.floor(container.bounds.left + container.bounds.width / 2);
  const startY = Math.floor(container.bounds.top + container.bounds.height * (direction === 'down' ? 0.78 : 0.3));
  const endY = Math.floor(container.bounds.top + container.bounds.height * (direction === 'down' ? 0.3 : 0.78));
  if (x < 0 || x >= size.width || startY >= size.height || endY < 0 || (direction === 'down' ? startY <= endY : startY >= endY)) {
    return { status: 'BLOCKED', reason: 'Los bounds observados del contenedor no permiten un gesto vertical dentro del viewport.' };
  }
  adb(serial, ['shell', 'input', 'swipe', String(x), String(startY), String(x), String(endY), '280']);
  await delay(300);
  const afterNodes = await dumpHierarchy(serial, context);
  const movedAnchor = anchors.map(anchor => {
    const after = afterNodes.find(node => stableNodeKey(node) === stableNodeKey(anchor));
    return after ? { label: anchor.text, topBefore: anchor.bounds.top, topAfter: after.bounds.top, deltaPx: after.bounds.top - anchor.bounds.top } : null;
  }).find(anchor => anchor && Math.abs(anchor.deltaPx) >= 4) ?? null;
  return {
    status: movedAnchor ? 'PASS' : 'FAIL',
    reason: movedAnchor ? 'El ancla semántica cambió de posición tras el gesto en bounds observados.' : 'El gesto se emitió desde bounds observados, pero no se pudo verificar movimiento vertical de contenido.',
    container: { left: container.bounds.left, top: container.bounds.top, right: container.bounds.right, bottom: container.bounds.bottom },
    gesture: { direction, x, startY, endY, durationMs: 280 }, movedAnchor, afterNodes,
  };
}

function targetSourceMatches(report) {
  const metadata = report.device.candidateMetadata;
  return metadata?.matchesApkHash === true && metadata.sourceCommit === report.source.commit;
}

async function inspectPageWithBoundedScroll(serial, context, nodes, size, labels, caseName, alwaysScroll = false) {
  const collected = new Map();
  const collect = page => page.forEach(node => collected.set(`${stableNodeKey(node)}|${node.bounds.top}`, node));
  let current = nodes;
  collect(current);
  let scroll = null;
  const missing = () => labels.filter(label => !findVisibleNode([...collected.values()], label, size));
  if (alwaysScroll || missing().length) {
    scroll = await swipeObservedContainer(serial, context, current, size, 'down');
    context.scrolls.push({ case: caseName, ...scroll, afterNodes: undefined });
    if (scroll.afterNodes) {
      current = scroll.afterNodes;
      collect(current);
    }
  }
  return { current, nodes: [...collected.values()], missing: missing(), scroll };
}

async function restoreNavigation(serial, context, nodes, size, caseName) {
  if (tabs.every(tab => observedTab(nodes, tab, size))) return nodes;
  const scroll = await swipeObservedContainer(serial, context, nodes, size, 'up');
  context.scrolls.push({ case: `${caseName}-NAVIGATION`, ...scroll, afterNodes: undefined });
  if (!scroll.afterNodes || !tabs.every(tab => observedTab(scroll.afterNodes, tab, size))) return null;
  await captureScreenshot(serial, context, `${caseName} navegación`);
  return scroll.afterNodes;
}

function addSurfaceResult(report, id, expected, missing, sourceMatches, actualPrefix = 'Rótulos observados') {
  const status = missing.length === 0 ? 'PASS' : sourceMatches ? 'FAIL' : 'NOT_RUN';
  addCheck(report, {
    id, requirement: 'UI-01', platform: report.device.kind, status, expected,
    actual: missing.length === 0 ? `${actualPrefix}: todos los controles esperados están visibles.`
      : `${actualPrefix}: faltan ${missing.join(', ')}.${sourceMatches ? '' : ' El APK identificado procede de otra revisión; el resultado no atribuye estos controles a código ausente.'}`,
    resume: status === 'NOT_RUN' ? 'Repetir sobre un APK construido desde la revisión integrada de #43/#40/#45 y comparar los controles semánticos.' : undefined,
  });
}

async function navigateToTab(serial, context, nodes, tab, size) {
  const tabNode = observedTab(nodes, tab, size);
  if (!tabNode) throw new Error(`No se encontró «${tab}» en la jerarquía observada antes del toque.`);
  tapObservedNode(serial, tabNode, size);
  await delay(300);
  const next = await dumpHierarchy(serial, context);
  await captureScreenshot(serial, context, tab);
  return next;
}

async function runUiChecks(report, serial, context) {
  const foreground = isAppForeground(serial);
  report.device.foregroundPackage = foreground.packageName;
  if (!foreground.app) {
    for (const check of uiPendingChecks('La app no está en primer plano; el runner no la inicia ni fuerza su cierre.')) addCheck(report, check);
    return;
  }

  const display = report.device.display;
  const initial = await dumpHierarchy(serial, context);
  const initialScreenshot = await captureScreenshot(serial, context, 'Pantalla inicial');
  const hasAppContent = initial.some(node => isVisibleInDisplay(node, display.size)
    && (node.text.trim() || node.contentDescription.trim()) && !tabs.includes(node.text.trim()));
  addCheck(report, {
    id: 'UI-STARTUP-RENDER', requirement: 'UI-01/VERIFY-02', platform: report.device.kind,
    status: hasAppContent && initialScreenshot.bytes > 0 ? 'PASS' : 'FAIL',
    expected: 'La actividad Android de Finsim está en primer plano, muestra contenido y se conserva una captura PNG verificable.',
    actual: `foreground=${foreground.packageName}; nodos visibles de contenido=${initial.filter(node => isVisibleInDisplay(node, display.size) && (node.text.trim() || node.contentDescription.trim())).length}; captura=${initialScreenshot.file} SHA-256 ${initialScreenshot.sha256}.`,
  });

  const sourceMatches = targetSourceMatches(report);
  let navigation = initial;
  let navigationRecovery = null;
  if (!tabs.every(tab => observedTab(navigation, tab, display.size))) {
    navigationRecovery = await swipeObservedContainer(serial, context, navigation, display.size, 'up');
    context.scrolls.push({ case: 'UI-NAVIGATION-RESTORE', ...navigationRecovery, afterNodes: undefined });
    if (navigationRecovery.afterNodes) {
      navigation = navigationRecovery.afterNodes;
      await captureScreenshot(serial, context, 'Navegacion recuperada');
    }
  }
  const originalTab = tabs.find(tab => observedTab(navigation, tab, display.size)?.selected) ?? 'Préstamo';
  const navNodes = tabs.map(tab => ({ tab, node: observedTab(navigation, tab, display.size) })).filter(entry => entry.node);
  const touchResults = navNodes.map(({ tab, node }) => {
    const target = getInteractiveTarget(node);
    const size = display.densityDpi ? touchTargetDp(target.bounds, display.densityDpi) : null;
    return { tab, dp: size ? { width: Number(size.width.toFixed(2)), height: Number(size.height.toFixed(2)) } : null, passes: size?.passes ?? false };
  });
  const touchMissing = tabs.filter(tab => !touchResults.some(result => result.tab === tab));
  const undersized = touchResults.filter(result => !result.passes).map(result => result.tab);
  const touchStatus = !display.densityDpi || !display.size ? 'BLOCKED'
    : touchMissing.length ? sourceMatches ? 'FAIL' : 'NOT_RUN'
      : undersized.length ? 'FAIL' : 'PASS';
  addCheck(report, {
    id: 'UI-TOUCH-TARGETS', requirement: 'UI-03', platform: report.device.kind,
    status: touchStatus,
    expected: 'Las cuatro pestañas de navegación medidas en el viewport inicial tienen un área interactiva de al menos 48 × 48 dp.',
    actual: JSON.stringify({ densityDpi: display.densityDpi, measuredScope: 'solo las cuatro pestañas de navegación', recoverySwipe: navigationRecovery ? { status: navigationRecovery.status, gesture: navigationRecovery.gesture ?? null, movedAnchor: navigationRecovery.movedAnchor ?? null } : null, targets: touchResults, missing: touchMissing, undersized }),
    resume: touchStatus === 'NOT_RUN' ? 'Abrir una vista que muestre la navegación; el perfil solo intenta un desplazamiento con bounds observados.' : undefined,
  });

  if (touchMissing.length) {
    addMissingUiPending(report, 'La navegación completa no quedó visible tras una sola recuperación de viewport basada en un contenedor observado; no se hicieron toques de pestaña.');
    return;
  }

  const loanNodes = originalTab === 'Préstamo' ? navigation
    : await navigateToTab(serial, context, navigation, 'Préstamo', display.size);
  const originControl = findVisibleNode(loanNodes, 'Origen', display.size);
  if (sourceMatches && originControl) {
    tapObservedNode(serial, originControl, display.size);
    await delay(250);
    const options = await dumpHierarchy(serial, context);
    const optionsPresent = ['Préstamo original', 'Saldo actual'].every(label => findVisibleNode(options, label, display.size, { exact: true }));
    addSurfaceResult(report, 'UI-SURFACE-LOAN-ORIGIN', 'El selector Origen ofrece Préstamo original y Saldo actual.', optionsPresent ? [] : ['Préstamo original / Saldo actual'], true, 'Opciones del selector');
    adb(serial, ['shell', 'input', 'keyevent', '4']);
    await delay(250);
  } else {
    addCheck(report, pendingCheck('UI-SURFACE-LOAN-ORIGIN', 'UI-01', 'El selector Origen ofrece Préstamo original y Saldo actual.',
      `No se abrió el selector: ${originControl ? 'el APK candidato procede de una revisión anterior' : 'Origen no está visible en el viewport observado'}.`,
      'Abrir Origen en el APK integrado y observar las dos opciones sin guardar condiciones.'));
  }

  const loanLabels = ['Capital', 'Tasa de interés'];
  const conditionLabels = ['SEGUROS DEL PRÉSTAMO', '+ Agregar seguro', 'Editar meses · Lote de eventos', 'Aplicar cambios'];
  let loanScan;
  if (sourceMatches) {
    let current = loanNodes;
    const collected = new Map();
    const collect = page => page.forEach(node => collected.set(`${stableNodeKey(node)}|${node.bounds.top}`, node));
    collect(current);
    let swipes = 0;
    while (swipes < MAX_LOAN_SWIPES && !conditionLabels.every(label => findVisibleNode([...collected.values()], label, display.size))) {
      const outcome = await swipeObservedContainer(serial, context, current, display.size);
      context.scrolls.push({ case: 'UI-SURFACE-CONDITIONS-EVENTS', ...outcome, afterNodes: undefined });
      if (outcome.status !== 'PASS') break;
      current = outcome.afterNodes;
      collect(current);
      swipes += 1;
    }
    loanScan = { current, nodes: [...collected.values()], swipes };
    if (swipes) await captureScreenshot(serial, context, 'Préstamo controles visibles');
  } else {
    loanScan = await inspectPageWithBoundedScroll(serial, context, loanNodes, display.size, loanLabels, 'UI-SURFACE-LOAN');
    loanScan.swipes = loanScan.scroll?.status === 'PASS' ? 1 : 0;
    if (loanScan.swipes) await captureScreenshot(serial, context, 'Préstamo campos visibles');
  }

  const loanMissing = loanLabels.filter(label => !findVisibleNode(loanScan.nodes, label, display.size));
  const loanStatus = loanMissing.length === 0 ? 'PASS' : sourceMatches && loanScan.swipes < MAX_LOAN_SWIPES ? 'FAIL' : 'NOT_RUN';
  addCheck(report, {
    id: 'UI-SURFACE-LOAN', requirement: 'UI-01', platform: report.device.kind, status: loanStatus,
    expected: 'Préstamo muestra los campos de capital y tasa.',
    actual: loanMissing.length ? `Pantalla de préstamo: faltan ${loanMissing.join(', ')} en ${loanScan.swipes} desplazamientos limitados.${sourceMatches ? '' : ' El APK identificado procede de otra revisión.'}` : 'Campos de capital y tasa observados en la pantalla de préstamo.',
    resume: loanStatus === 'NOT_RUN' ? 'Repetir sobre el APK integrado o extender el recorrido solo con el criterio de scroll acordado.' : undefined,
  });

  const loanNavigation = await restoreNavigation(serial, context, loanScan.current, display.size, 'Préstamo');
  if (!loanNavigation) {
    addMissingUiPending(report, 'La navegación no volvió a quedar visible después del scan acotado de Préstamo; no se intentaron toques fuera del viewport.');
    return;
  }
  if (sourceMatches) {
    const missing = conditionLabels.filter(label => !findVisibleNode(loanScan.nodes, label, display.size));
    const status = missing.length === 0 ? 'PASS' : loanScan.swipes >= MAX_LOAN_SWIPES ? 'NOT_RUN' : 'FAIL';
    addCheck(report, {
      id: 'UI-SURFACE-CONDITIONS-EVENTS', requirement: 'UI-01', platform: report.device.kind, status,
      expected: 'LoanForm expone condiciones de seguros y controles del lote de eventos.',
      actual: missing.length ? `Faltan ${missing.join(', ')} tras ${loanScan.swipes}/${MAX_LOAN_SWIPES} desplazamientos observados; se detuvo al alcanzar el límite por caso.` : `Controles de seguro y eventos encontrados con ${loanScan.swipes} desplazamientos observados.`,
      resume: status === 'NOT_RUN' ? 'Continuar en el perfil integrado con el desplazamiento acotado por caso; no inferir disponibilidad desde este resultado.' : undefined,
    });
  } else {
    addCheck(report, pendingCheck('UI-SURFACE-CONDITIONS-EVENTS', 'UI-01', 'LoanForm expone condiciones de seguros y controles del lote de eventos.',
      'El APK candidato procede de una revisión anterior a la integración de las etiquetas de condiciones/eventos; no se recorrió contenido ausente de esa fuente.',
      'Repetir sobre el APK integrado; el runner limita esta búsqueda a dos desplazamientos medidos desde bounds observados.'));
  }

  const reconInitial = await navigateToTab(serial, context, loanNavigation, 'Conciliación', display.size);
  const reconciliationLabels = ['Comparar extracto bancario', 'Número de mes del extracto'];
  const reconScan = await inspectPageWithBoundedScroll(serial, context, reconInitial, display.size,
    reconciliationLabels, 'UI-SURFACE-RECONCILIATION');
  if (reconScan.scroll?.status === 'PASS') await captureScreenshot(serial, context, 'Conciliación controles visibles');
  addSurfaceResult(report, 'UI-SURFACE-RECONCILIATION', 'La pantalla Conciliación expone comparación y entrada de mes.',
    reconciliationLabels.filter(label => !findVisibleNode(reconScan.nodes, label, display.size)), sourceMatches);
  const reconNavigation = await restoreNavigation(serial, context, reconScan.current, display.size, 'Conciliación');
  if (!reconNavigation) {
    addMissingUiPending(report, 'La navegación no volvió a quedar visible después de una inspección acotada de Conciliación; no se intentaron toques fuera del viewport.');
    return;
  }

  let offers = await navigateToTab(serial, context, reconNavigation, 'Ofertas', display.size);
  const offerLabels = ['Compara ofertas con tu dinero', 'Aplicar comparación'];
  const offerScan = await inspectPageWithBoundedScroll(serial, context, offers, display.size,
    offerLabels, 'UI-SURFACE-OFFERS', true);
  if (offerScan.scroll?.status === 'PASS') await captureScreenshot(serial, context, 'Ofertas controles visibles');
  addSurfaceResult(report, 'UI-SURFACE-OFFERS', 'La pantalla Ofertas expone comparación y acción de aplicación.',
    offerLabels.filter(label => !findVisibleNode(offerScan.nodes, label, display.size)), sourceMatches);
  const scroll = offerScan.scroll ?? { status: 'BLOCKED', reason: 'No se emitió el desplazamiento de Ofertas.' };
  addCheck(report, {
    id: 'UI-SCROLL', requirement: 'UI-01', platform: report.device.kind, status: scroll.status,
    expected: 'Un gesto calculado desde bounds visibles desplaza contenido vertical y mueve un ancla semántica observada.',
    actual: JSON.stringify({ reason: scroll.reason, container: scroll.container ?? null, gesture: scroll.gesture ?? null, movedAnchor: scroll.movedAnchor ?? null }),
    resume: scroll.status === 'BLOCKED' ? 'Abrir una superficie con contenido desplazable visible en el APK de pruebas y repetir.' : undefined,
  });
  const offersNavigation = await restoreNavigation(serial, context, offerScan.current, display.size, 'Ofertas');
  if (!offersNavigation) {
    addMissingUiPending(report, 'La navegación no volvió a quedar visible después del gesto observado de Ofertas; no se intentaron toques fuera del viewport.');
    return;
  }

  const converterInitial = await navigateToTab(serial, context, offersNavigation, 'Conversor', display.size);
  const converterLabels = ['Conversor de tasas de interés', 'TASAS EQUIVALENTES CALCULADAS', 'Tasa (%)'];
  const converterScan = await inspectPageWithBoundedScroll(serial, context, converterInitial, display.size,
    converterLabels, 'UI-SURFACE-RATE-CONVERTER');
  if (converterScan.scroll?.status === 'PASS') await captureScreenshot(serial, context, 'Conversor controles visibles');
  addSurfaceResult(report, 'UI-SURFACE-RATE-CONVERTER', 'El Conversor muestra tasa de entrada y resultados equivalentes.',
    converterLabels.filter(label => !findVisibleNode(converterScan.nodes, label, display.size)), sourceMatches);
  const rateReference = FINANCIAL_REFERENCES.find(item => item.id === 'ANDROID-RATE');
  const rateField = converterScan.current.find(node => node.editable && isVisibleInDisplay(node, display.size))
    ?? findVisibleNode(converterScan.current, 'Tasa (%)', display.size);
  const rateObservation = evaluateFinancialObservation({ actionPerformed: false, observedText: null, expected: 12.6825030131969720661201, tolerance: 1e-12 });
  addCheck(report, {
    id: 'ANDROID-RATE', requirement: 'FIN-01', platform: report.device.kind,
    status: rateObservation.status, expected: rateReference.expected, expectationSource: rateReference.source,
    actual: 'No se ingresó la referencia EM 1% ni se leyó una EA generada por esa acción.',
    resume: 'Ingresar 1 en Tasa (%) con el destino de pruebas controlado, esperar la recomposición y cotejar la EA visible con la referencia independiente.',
  });

  let focusStatus = 'NOT_RUN';
  let focusActual = 'No se encontró un campo de tasa editable visible en la jerarquía observada.';
  if (rateField) {
    tapObservedNode(serial, rateField, display.size);
    await delay(450);
    const focusedNodes = await dumpHierarchy(serial, context);
    const focusedField = focusedNodes.some(node => node.editable && node.focused);
    const keyboard = imeShown(serial);
    focusStatus = keyboard === null ? 'BLOCKED' : focusedField && keyboard ? 'PASS' : 'FAIL';
    let keyboardAfterDismiss = keyboard;
    if (keyboard) {
      adb(serial, ['shell', 'input', 'keyevent', '4']);
      await delay(300);
      keyboardAfterDismiss = imeShown(serial);
    }
    focusActual = `campo editable enfocado=${focusedField}; teclado antes=${keyboard}; después de cierre=${keyboardAfterDismiss}.`;
    if (keyboard && keyboardAfterDismiss !== false) focusStatus = 'BLOCKED';
  } else if (sourceMatches) {
    focusStatus = 'FAIL';
  }
  addCheck(report, {
    id: 'UI-FOCUS-KEYBOARD', requirement: 'UI-01', platform: report.device.kind, status: focusStatus,
    expected: 'Un campo editable recibe foco y el teclado virtual aparece; el teclado se cierra antes de navegar.',
    actual: focusActual,
    resume: focusStatus === 'NOT_RUN' ? 'Repetir en el APK integrado con un campo editable presente.' : undefined,
  });
  for (const reference of FINANCIAL_REFERENCES.filter(item => item.id !== 'ANDROID-RATE')) {
    addCheck(report, pendingCheck(reference.id, 'FIN-01/FIN-02/UI-01', reference.expected,
      'Esta corrida de baseline inspecciona navegación, teclado y desplazamiento; no introduce ni guarda datos financieros.',
      'Ejecutar el caso en el APK integrado con controles observables y comparar las cifras independientes definidas en docs/financial-conventions.md.'));
  }
}

function uiPendingChecks(reason) {
  return [
    pendingCheck('UI-TOUCH-TARGETS', 'UI-03', '48 × 48 dp medidos con la densidad real.', reason, 'Seleccionar un dispositivo de pruebas y autorizar --allow-ui-actions.'),
    pendingCheck('UI-STARTUP-RENDER', 'UI-01/VERIFY-02', 'La actividad Android de Finsim está en primer plano, muestra contenido y se conserva una captura PNG verificable.', reason, 'Abrir Finsim en el destino seleccionado y autorizar --allow-ui-actions.'),
    ...['LOAN', 'RATE-CONVERTER', 'RECONCILIATION', 'OFFERS', 'CONDITIONS-EVENTS'].map(id => pendingCheck(`UI-SURFACE-${id}`, 'UI-01', `La superficie ${id} está disponible y expone controles semánticos.`, reason, 'Abrir Finsim en el dispositivo y autorizar --allow-ui-actions.')),
    pendingCheck('UI-SURFACE-LOAN-ORIGIN', 'UI-01', 'El selector Origen ofrece Préstamo original y Saldo actual.', reason, 'Abrir Origen en la pantalla de préstamo y observar sus opciones.'),
    pendingCheck('ANDROID-RATE', 'FIN-01', FINANCIAL_REFERENCES.find(item => item.id === 'ANDROID-RATE').expected, reason, 'Abrir el conversor y observar su resultado frente a la referencia literal.'),
    pendingCheck('UI-FOCUS-KEYBOARD', 'UI-01', 'Un campo recibe foco y muestra el teclado virtual.', reason, 'Abrir Finsim en el dispositivo y autorizar --allow-ui-actions.'),
    pendingCheck('UI-SCROLL', 'UI-01', 'Un contenedor vertical desplaza su contenido.', reason, 'Abrir Finsim en el dispositivo y autorizar --allow-ui-actions.'),
    ...FINANCIAL_REFERENCES.filter(item => item.id !== 'ANDROID-RATE').map(reference => pendingCheck(reference.id, 'FIN-01/FIN-02/UI-01', reference.expected, reason, 'Correr el caso en un APK/dispositivo de prueba controlado y comparar cada cifra con su expectativa literal.')),
  ];
}

function renderMarkdown(report) {
  const checks = report.checks.map(check => [
    `### ${check.id} — ${check.status}`,
    `**Criterio:** ${check.requirement} · ${check.platform}`,
    `**Esperado:** ${check.expected}`,
    check.expectationSource ? `**Fuente independiente:** ${check.expectationSource}` : '',
    `**Observado:** ${check.actual}`,
    check.resume ? `**Retomar:** ${check.resume}` : '',
  ].filter(Boolean).join('\n\n')).join('\n\n');
  const refs = report.financialReferences.map(reference => `- **${reference.id}:** ${reference.expected} Fuente: ${reference.source}`).join('\n');
  const apkParts = report.device?.installedApks?.map((apk, index) => `${index === 0 ? 'base' : `split ${index}`}: ${apk.packagePath} ${apk.sha256}`).join('; ') ?? 'no disponible';
  const candidateStatus = report.device?.candidateMatchesInstalledApk === true ? 'coincide byte a byte' : report.device?.candidateMatchesInstalledApk === false ? 'no coincide' : report.device?.candidateApk ? `sin comparación del paquete completo (base coincide=${report.device.candidateMatchesInstalledBase})` : 'sin APK candidato';
  const candidateMetadata = report.device?.candidateMetadata;
  const candidateSource = candidateMetadata
    ? candidateMetadata.matchesApkHash
      ? `${candidateMetadata.file} declara fuente ${candidateMetadata.sourceCommit ?? 'sin commit'}${candidateMetadata.implementationCommit ? ` (implementación ${candidateMetadata.implementationCommit})` : ''}; SHA del sidecar coincide con candidato`
      : `${candidateMetadata.file} no verifica SHA del candidato; fuente sin atribuir`
    : 'sin metadatos laterales';
  return `# PA — perfil Android de verificación

**Estado:** ${report.result.status} (exit ${report.result.exitCode})

**Ejecución:** ${report.runId} · ${report.startedAt}

**Fuente local:** ${report.source.branch ?? 'desconocida'}@${report.source.commit ?? 'desconocido'} · árbol ${report.source.workingTree?.hasLocalChanges ? 'con cambios' : 'limpio'} · huella ${report.source.workingTree?.sha256 ?? 'no disponible'}

**Dispositivo:** ${report.device ? `${report.device.kind ?? 'sin identificar'} ${report.device.manufacturer ?? ''} ${report.device.model ?? ''} · Android ${report.device.androidVersion ?? '?'} (API ${report.device.androidSdk ?? '?'}) · ${report.device.display?.size ? `${report.device.display.size.width}×${report.device.display.size.height}px` : 'tamaño desconocido'} @ ${report.device.display?.densityDpi ?? '?'} dpi` : 'ningún destino seleccionado'}

**Memoria física observada:** ${report.device?.memory?.memTotalBytes ? `${report.device.memory.memTotalKb} kB (${report.device.memory.memTotalBytes} bytes; ${report.device.memory.memTotalGiB} GiB)` : 'MemTotal no disponible'} · PSI ${report.device?.memory?.pressure ? JSON.stringify(report.device.memory.pressure) : report.device?.memory?.pressureError ?? 'no disponible'}

**APK instalado:** ${report.device?.packageName ?? PACKAGE_NAME} ${report.device?.versionName ?? '?'} (${report.device?.versionCode ?? '?'}) · ${apkParts} · candidato ${candidateStatus}

**Origen declarado del APK candidato:** ${candidateSource}

**Evidencia:** ${report.evidenceDirectory} · capturas ${report.execution.screenshots?.map(item => `${item.file} SHA-256 ${item.sha256}`).join('; ') || 'ninguna'} · dumps de jerarquía ${report.execution.uiautomatorDumps?.length ?? 0}/${report.execution.uiautomatorDumpLimit ?? 'sin límite'}

## Comprobaciones

${checks || 'Sin comprobaciones.'}

## Referencias financieras independientes

${refs}

El commit local y el hash del APK son identidades separadas; la coincidencia entre un candidato y el APK instalado no demuestra por sí sola qué revisión lo generó. Los metadatos laterales solo se presentan como declaración y se asocian al candidato cuando su SHA declarado coincide con los bytes. Se enumeran todas las rutas de pm path y sus hashes; un único --apk candidato solo puede demostrar coincidencia completa cuando el paquete instalado tiene un solo APK. El runner no instala ni borra datos. No inicia ni fuerza el cierre por defecto; --start-app abre explícitamente y --restart-app cierra/reabre MainActivity preservando almacenamiento, ambos requieren --allow-ui-actions. Los gestos UI requieren opt-in y usan un presupuesto máximo de dieciséis dumps de jerarquía, una recuperación de viewport, hasta dos desplazamientos para controles del préstamo y como máximo uno por cada superficie de conciliación, ofertas y conversor. Las recuperaciones de navegación solo usan contenedores y bounds observados; si la pestaña no vuelve al viewport, el recorrido se detiene sin toques adicionales. Sin --device no selecciona automáticamente un destino. Emulador y dispositivo físico se informan por separado; ninguna corrida de emulador acredita inferencia Gemma.
`;
}

async function writeReport(report, directory) {
  report.finishedAt = new Date().toISOString();
  report.result = summarizeVerification(report.checks);
  await writeFile(path.join(directory, 'report.json'), `${JSON.stringify(report, null, 2)}\n`, { flag: 'wx' });
  await writeFile(path.join(directory, 'report.md'), renderMarkdown(report), { flag: 'wx' });
  console.log(`Android ${report.result.status}. Evidencia: ${directory}`);
  return report.result.exitCode;
}

async function main(args = process.argv.slice(2)) {
  let options;
  try { options = parseOptions(args); }
  catch (error) { console.error(error.message); process.exitCode = 2; return; }
  if (options.help) {
    console.log('Uso: npm run verify:device -- [--device <serial> [--allow-ui-actions [--start-app | --restart-app]]] [--apk <APK local>]');
    console.log('Sin --device solo consulta el inventario ADB. --start-app abre MainActivity; --restart-app hace force-stop y abre MainActivity. Ambos requieren opt-in explícito y preservan almacenamiento.');
    return;
  }

  const started = new Date();
  const runId = `${started.toISOString().replaceAll(':', '-').replaceAll('.', '-')}-${randomUUID().slice(0, 8)}`;
  const directory = path.join(reportRoot, runId);
  await mkdir(directory, { recursive: true });
  const uiContext = { runId, evidenceDirectory: directory, dumps: [], screenshots: [], scrolls: [], restoredTab: null };
  const source = await sourceIdentity();
  const report = {
    schemaVersion: 1, runId, startedAt: started.toISOString(), evidenceDirectory: directory,
    profile: { id: 'PA', name: 'Android installed UI inspection', scope: 'APK instalado; superficies UI y condiciones de interacción en destino explícito' },
    source, device: null, inventory: null, financialReferences: FINANCIAL_REFERENCES,
    checks: [], execution: {
      targetSelected: !!options.device, uiActionsOptedIn: options.allowUiActions,
      install: false, launch: false, forceStop: false, clearData: false,
      uiautomatorDumpLimit: MAX_HIERARCHY_DUMPS, uiautomatorDumps: uiContext.dumps,
      screenshots: uiContext.screenshots, scrolls: uiContext.scrolls, restoredTab: null,
      launchResult: null,
    },
  };

  let devices;
  try {
    devices = parseDevices(adb(null, ['devices', '-l']));
    report.inventory = { adbAvailable: true, targetCount: devices.length, states: devices.map(device => device.state) };
    addCheck(report, { id: 'ADB-INVENTORY', requirement: 'VERIFY-01', platform: 'host', status: 'PASS', expected: 'ADB responde y enumera estados de dispositivos sin seleccionarlos.', actual: `${devices.length} destino(s) enumerado(s); no se seleccionó ninguno automáticamente.` });
  } catch (error) {
    report.inventory = { adbAvailable: false };
    addCheck(report, { id: 'ADB-INVENTORY', requirement: 'VERIFY-01', platform: 'host', status: 'BLOCKED', expected: 'ADB responde al inventario de dispositivos.', actual: error instanceof Error ? error.message : String(error), resume: 'Instalar o reparar Android Platform Tools y volver a ejecutar el perfil.' });
  }

  if (!options.device) {
    for (const check of uiPendingChecks('No se eligió un destino con --device; la inspección ADB solo enumeró disponibilidad.')) addCheck(report, check);
  } else {
    const selected = chooseDevice(devices ?? [], options.device);
    if (!selected || selected.state !== 'device') {
      addCheck(report, { id: 'DEVICE-SELECTION', requirement: 'VERIFY-01', platform: 'Android', status: 'BLOCKED', expected: 'El serial explícito corresponde a un dispositivo en estado device.', actual: selected ? `Destino seleccionado en estado ${selected.state}.` : 'El serial no aparece en adb devices -l.', resume: 'Conectar un destino de pruebas autorizado, aceptar su autorización ADB y repetir con su serial explícito.' });
      for (const check of uiPendingChecks('El destino seleccionado no está listo; no se realizaron acciones en él.')) addCheck(report, check);
    } else {
      try {
        const candidate = options.apk ? path.resolve(root, options.apk) : null;
        if (candidate) await stat(candidate);
        report.device = await identifyDevice(options.device, candidate);
        addCheck(report, { id: 'DEVICE-SELECTION', requirement: 'VERIFY-01', platform: report.device.kind, status: 'PASS', expected: 'Destino seleccionado explícitamente e identificado como emulador o dispositivo físico.', actual: `${report.device.kind}; fingerprint ${report.device.fingerprint}; ${report.device.manufacturer ?? ''} ${report.device.model ?? ''}.` });
        const installedApkDetails = report.device.installedApks.map((apk, index) => `${index === 0 ? 'base' : `split ${index}`}: ${apk.packagePath} SHA-256 ${apk.sha256}; ${apk.bytes} bytes`).join(' · ');
        addCheck(report, { id: 'APK-IDENTITY', requirement: 'VERIFY-02', platform: report.device.kind, status: 'PASS', expected: `Todas las rutas de APK instalado para ${PACKAGE_NAME} tienen versión y hash SHA-256 identificables.`, actual: `${report.device.versionName ?? 'versión desconocida'} (${report.device.versionCode ?? 'código desconocido'}); ${installedApkDetails}.` });
        const candidateComparison = compareInstalledApkCandidate(report.device.candidateApk, report.device.installedApks);
        addCheck(report, { id: 'APK-CANDIDATE-MATCH', requirement: 'VERIFY-02', platform: report.device.kind, ...candidateComparison, resume: candidateComparison.status === 'NOT_RUN' ? candidateComparison.baseMatches === undefined ? 'Construir el APK de la revisión que se quiere verificar y repetir con --apk <ruta del APK construido>.' : 'Proporcionar el conjunto completo de artefactos split para comparar cada componente.' : undefined });
        if (options.restartApp) {
          report.execution.forceStop = true;
          report.execution.restart = true;
          adb(options.device, ['shell', 'am', 'force-stop', PACKAGE_NAME], 15000);
          report.execution.launch = true;
          report.execution.launchResult = startApp(options.device);
          if (!report.execution.launchResult.succeeded) throw new Error(`am start no confirmó MainActivity después del reinicio: ${report.execution.launchResult.status ?? 'sin Status'} / ${report.execution.launchResult.activity ?? 'sin Activity'}.`);
        } else if (options.startApp) {
          report.execution.launch = true;
          report.execution.launchResult = startApp(options.device);
          if (!report.execution.launchResult.succeeded) throw new Error(`am start no confirmó MainActivity: ${report.execution.launchResult.status ?? 'sin Status'} / ${report.execution.launchResult.activity ?? 'sin Activity'}.`);
        }
        if (options.allowUiActions) {
          await runUiChecks(report, options.device, uiContext);
          report.execution.restoredTab = uiContext.restoredTab;
        }
        else for (const check of uiPendingChecks('Se seleccionó el dispositivo, pero faltó el opt-in --allow-ui-actions; no se consultó ni manipuló la interfaz.')) addCheck(report, check);
      } catch (error) {
        const message = error instanceof Error ? error.message.replaceAll(options.device, '[serial oculto]') : String(error);
        addCheck(report, { id: 'DEVICE-PROFILE-EXECUTION', requirement: 'VERIFY-02', platform: 'Android', status: 'BLOCKED', expected: 'Identidad del APK y perfil de UI completados en el destino seleccionado.', actual: message, resume: 'Resolver el prerequisito indicado y repetir; el reporte actual conserva el bloqueo.' });
        addMissingUiPending(report, 'La ejecución terminó antes de observar este criterio; los checks ya completados se conservan sin duplicarse.');
      }
    }
  }

  process.exitCode = await writeReport(report, directory);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => { console.error(error); process.exitCode = 1; });
}
