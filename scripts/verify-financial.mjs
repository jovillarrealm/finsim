import { createHash, randomUUID } from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import { createReadStream, createWriteStream } from 'node:fs';
import { lstat, mkdir, readFile, readdir, readlink, stat, writeFile } from 'node:fs/promises';
import { once } from 'node:events';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { summarizeVerification } from './verification-status.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const resultsRoot = path.join(root, 'kmp', 'shared', 'build', 'test-results', 'jvmTest');
const classRoot = path.join(root, 'kmp', 'shared', 'build', 'classes', 'kotlin', 'jvm');
const reportRoot = path.join(os.homedir(), '.finsim', 'verification', 'financial');
export const financialChecks = [
  ['PF-RATE-EA-ROUNDTRIP', 'RatesTest', 'testRatesConversionsAndRoundtrips', 'EM 0.01 → EA 0.126825030131969720661201; vuelta a EM con error absoluto < 1e-12.', 'FIN-01 · tasas', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-ZERO', 'RatesTest', 'testZeroRate', 'Tasa mensual cero conserva cero al convertir a EA y nominal anual con frecuencia 12.', 'FIN-01 · tasa cero', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-TINY', 'RatesTest', 'testTinyRate', 'EA 0.0001 vuelve desde EM con error absoluto < 1e-12.', 'FIN-01 · tolerancia de tasa', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-NOMINAL', 'RatesTest', 'testNominalFrequencies', 'Nominal 0.12 con frecuencia 12 produce EM 0.01; frecuencia 4 vuelve a 0.12 con error < 1e-12.', 'FIN-01 · frecuencia nominal', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-TYPED-EA', 'RatesTest', 'testTargetRateMonthlyAndAnnual', 'La interfaz tipada convierte EM 0.01 a EA 0.126825030131969720661201 y retorna con error < 1e-12.', 'FIN-01 · interfaz tipada de tasas', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-TYPED-NOMINAL', 'RatesTest', 'testTargetRateNominalFrequencies', 'La interfaz tipada conserva las frecuencias nominales 1, 4 y 12; nominal 12 vuelve a EM con error < 1e-12.', 'FIN-01 · frecuencia nominal tipada', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-RATE-VALIDATION', 'RatesTest', 'testTargetRateTypeSafetyAndValidation', 'Frecuencia nominal no positiva, frecuencia ausente y destino desconocido se rechazan.', 'FIN-01 · validación de tasas', 'docs/financial-conventions.md §6', 'RatesTest.kt'],
  ['PF-LOAN-EXAMPLE', 'LoanTest', 'testExampleScenarioSimulation', 'El escenario de ejemplo liquida en el mes 36, fecha 2029-09, con deuda final 0.00.', 'FIN-01 · cronograma de ejemplo', 'docs/financial-conventions.md §5', 'LoanTest.kt'],
  ['PF-LOAN-HALF-CENT', 'LoanTest', 'testHalfCentRounding', 'Capital 100.50 a 1 % mensual registra interés 1.01.', 'FIN-01 · redondeo monetario', 'docs/financial-conventions.md §5', 'LoanTest.kt'],
  ['PF-LOAN-ZERO-CONSERVATION', 'LoanTest', 'testZeroRateConservation', 'Capital 100.00 sin interés se paga como 33.33, 33.33 y 33.34; total 100.00 y saldo final cero.', 'FIN-01 · conservación de capital', 'docs/financial-conventions.md §5', 'LoanTest.kt'],
  ['PF-LOAN-INTEREST', 'LoanTest', 'testTwoMonthsLoanWithInterest', 'Capital 1000.00, 1 % y 2 meses produce cuota 507.51; mes 1 interés 10.00/capital 497.51/saldo 502.49; mes 2 interés 5.02 y liquidación.', 'FIN-01 · cuota e interés', 'docs/financial-conventions.md §5', 'LoanTest.kt'],
  ['PF-LOAN-INSURANCE', 'LoanTest', 'testInsuranceContinuingAfterPayoff', 'Préstamo liquidado en mes 1; seguro posterior 10.00 en meses 2 y 3; caja total 130.00.', 'FIN-01 · fin del seguro', 'docs/financial-conventions.md §3 y §5', 'LoanTest.kt'],
  ['PF-LOAN-ARREARS', 'LoanTest', 'testMissedPaymentAndFullCatchup', 'Omisión y recuperación: cargos del mes 1 pendientes 10.00/5.00, atrasos 105.00; recuperación total liquida atrasos sin capitalizar intereses.', 'FIN-03 · una referencia de omisión y recuperación', 'docs/financial-conventions.md §4 y §5', 'LoanTest.kt'],
  ['PF-LOAN-ARGENTINA-SAMPLE', 'LoanTest', 'testArgentinaPrepaymentProfile', 'Caso ARS de la prueba: abono 1000.00 produce comisión 30.00 e IVA 6.30; no valida todos los supuestos regionales.', 'FIN-02 · muestra limitada del perfil ARS', 'Caso controlado en la prueba; no es regla legal ni de prestamista.', 'LoanTest.kt'],
  ['PF-LOAN-INCOMPLETE', 'LoanTest', 'testIncompleteSimulationAt1200Months', 'Un pago menor al interés termina en 1200 meses con estado incompleto, capital remanente positivo y sin liquidación falsa.', 'FIN-01 · resultado incompleto', 'docs/financial-conventions.md §4 y §6', 'LoanTest.kt'],
  ['PF-LOAN-CORPUS-INVARIANTS', 'ParityCorpusTest', 'testCorpusInvariantsAcrossAllScenarios', 'Seis escenarios controlados: caja = capital + interés + seguro + comisión + IVA (tolerancia 0.05); si liquidan, capital pagado = inicial y deuda = 0.', 'FIN-01 · invariantes del corpus controlado', 'docs/financial-conventions.md §6', 'ParityCorpusTest.kt'],
  ['PF-LOAN-VALIDATION', 'FinancialProfileValidationTest', 'testNonPositivePrincipalIsRejectedAtPublicSimulationSeam', 'simulateLoan rechaza capital 0.00 antes de iniciar el cronograma.', 'FIN-02 · validación de capital', 'docs/financial-conventions.md §6', 'FinancialProfileValidationTest.kt'],
];

export function parseJUnit(xml) {
  const cases = [];
  const suitePattern = /<testsuite\b[^>]*>([\s\S]*?)<\/testsuite>/gi;
  const casePattern = /<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/gi;
  for (const suite of xml.matchAll(suitePattern)) {
    for (const item of suite[1].matchAll(casePattern)) {
      const attributes = Object.fromEntries([...item[1].matchAll(/([\w:-]+)="([^"]*)"/g)].map(([, key, value]) => [key, value]));
      const body = item[2] ?? '';
      cases.push({
        className: attributes.classname ?? '',
        name: attributes.name ?? '',
        status: /<(?:failure|error)\b/i.test(body) ? 'FAIL' : /<skipped\b/i.test(body) ? 'NOT_RUN' : 'PASS',
      });
    }
  }
  return cases;
}

export function evaluateExpectedCases(expected, observed, freshResults) {
  return expected.map(check => {
    const matches = freshResults
      ? observed.filter(item => item.className === check.className && [check.testName, `${check.testName}[jvm]`].includes(item.name))
      : [];
    if (matches.length > 1) {
      return { ...check, status: 'FAIL', actual: 'El reporte contiene más de un caso con el mismo identificador.' };
    }
    const found = matches[0];
    return {
      ...check,
      status: found?.status ?? 'NOT_RUN',
      actual: found ? `JUnit identificó ${check.className}.${found.name}: ${found.status}.` : 'No hay un caso correspondiente en un reporte nuevo de esta ejecución.',
      evidence: found?.reportPath ?? null,
      ...(found?.status === 'NOT_RUN' ? { reason: 'JUnit marcó el caso como omitido.' } : {}),
    };
  });
}

const checkSpecs = financialChecks.map(([id, className, testName, expectation, requirement, expectationSource, sourceFile]) => ({
  id,
  className: className === 'ParityCorpusTest' ? 'com.finsim.domain.ParityCorpusTest' : `com.finsim.engine.${className}`,
  testName,
  expectation,
  source: `kmp/shared/src/commonTest/kotlin/com/finsim/${className === 'ParityCorpusTest' ? 'domain' : 'engine'}/${sourceFile}`,
  expectationSource,
  requirement,
  platform: 'JVM host',
}));

const coverageNotRun = [
  {
    criterion: 'FIN-01 en WasmJs', status: 'NOT_RUN', platform: 'WebAssembly',
    reason: 'PF ejecuta :shared:jvmTest sobre JVM host; esta ejecución no construye ni inicia WasmJs.',
    resume: 'Construir Wasm desde el mismo commit, identificar el hash de la distribución, servir ese artefacto y ejecutar los recorridos browser pertinentes.',
  },
  {
    criterion: 'FIN-01/FIN-02/FIN-03 en APK', status: 'NOT_RUN', platform: 'Android',
    reason: 'PF no compila, instala ni interactúa con un APK; no se seleccionó dispositivo en esta ejecución.',
    resume: 'Ejecutar el perfil Android cuando la toolchain (#34) y un AVD o dispositivo estén disponibles; registrar aparte emulador y hardware físico.',
  },
  {
    criterion: 'FIN-02/FIN-03 y UI-01 en interfaz', status: 'NOT_RUN', platform: 'WasmJs / Android',
    reason: 'Estos casos requieren configurar el préstamo y operar la interfaz; PF sólo verifica métodos del motor en JVM.',
    resume: 'Ejecutar los perfiles de interfaz de #40 y #43 sobre sus artefactos construidos desde el commit revisado.',
  },
];

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
  try {
    return {
      repository: git(['rev-parse', '--show-toplevel'], { encoding: 'utf8' }).trim(),
      branch: git(['branch', '--show-current'], { encoding: 'utf8' }).trim() || 'DETACHED',
      commit: git(['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
      workingTree: await workingTreeIdentity(),
    };
  } catch (error) {
    return { error: error instanceof Error ? error.message : String(error) };
  }
}

function runProcess(command, args, logPath, echo = true) {
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
        shell: process.platform === 'win32',
        windowsHide: true,
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      const consume = chunk => {
        log.write(chunk);
        if (echo) process.stdout.write(chunk);
        output = `${output}${chunk.toString('utf8')}`.slice(-96_000);
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

async function hashTree(directory) {
  try {
    await stat(directory);
  } catch {
    return null;
  }
  const hash = createHash('sha256');
  const files = [];
  async function visit(current) {
    for (const entry of await readdir(current, { withFileTypes: true })) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) await visit(fullPath);
      else if (entry.isFile()) files.push(fullPath);
    }
  }
  await visit(directory);
  files.sort();
  for (const file of files) {
    hash.update(path.relative(directory, file).replaceAll(path.sep, '/')).update('\0');
    const stream = createReadStream(file);
    for await (const chunk of stream) hash.update(chunk);
  }
  return { sha256: hash.digest('hex'), files: files.length };
}

async function copyFreshJUnit(startedAtMs, destination) {
  let names;
  try {
    names = await readdir(resultsRoot);
  } catch {
    return [];
  }
  const copied = [];
  for (const name of names.filter(value => /^TEST-.*\.xml$/.test(value)).sort()) {
    const sourcePath = path.join(resultsRoot, name);
    const info = await stat(sourcePath);
    if (info.mtimeMs < startedAtMs) continue;
    const target = `junit/${name}`;
    await mkdir(path.dirname(path.join(destination, target)), { recursive: true });
    const contents = await readFile(sourcePath);
    await writeFile(path.join(destination, target), contents, { flag: 'wx' });
    const digest = createHash('sha256').update(contents).digest('hex');
    copied.push(...parseJUnit(contents.toString('utf8')).map(item => ({ ...item, reportPath: target, reportSha256: digest })));
  }
  return copied;
}

function runCommand(args) {
  return `gradle ${args.map(value => /\s/.test(value) ? `"${value}"` : value).join(' ')}`;
}

function isPrerequisiteFailure(result) {
  return result.error?.code === 'ENOENT' || /JAVA_HOME|no java|java\.home|could not resolve|could not get resource|unknown host|connection timed out|offline mode/i.test(result.output);
}

function safeText(value) {
  return String(value ?? '').replaceAll('|', '\\|').replaceAll('\n', ' ').replaceAll('\r', ' ');
}

function renderMarkdown(report) {
  const tree = report.source.workingTree;
  const checks = report.checks.map(check => [
    `### ${safeText(check.id)} — ${safeText(check.status)}`,
    `**Criterio:** ${safeText(check.requirement)} · ${safeText(check.platform)}`,
    `**Expectativa:** ${safeText(check.expectation ?? check.expected)}`,
    check.expectationSource ? `**Fuente de expectativa:** ${safeText(check.expectationSource)}` : '',
    `**Resultado observado:** ${safeText(check.actual ?? check.reason)}`,
    check.source ? `**Prueba:** \`${safeText(check.className)}.${safeText(check.testName)}\` en \`${safeText(check.source)}\`${check.evidence ? ` · [JUnit](${safeText(check.evidence)})` : ''}` : '',
  ].filter(Boolean).join('\n\n')).join('\n\n');
  const pending = report.coverageNotRun.map(item => `- **${safeText(item.criterion)} · ${safeText(item.platform)} · ${item.status}:** ${safeText(item.reason)} Retomar: ${safeText(item.resume)}`).join('\n');
  const artifacts = Object.entries(report.artifacts).map(([name, item]) => `- \`${safeText(name)}\`: ${item ? `SHA-256 \`${item.sha256}\` (${item.files} archivos)` : 'no generado'}${item?.path ? ` · \`${safeText(item.path)}\`` : ''}`).join('\n');
  const evidenceFiles = report.evidenceFiles.map(file => `- [${safeText(file)}](${safeText(file)})`).join('\n');
  return `# PF — verificación financiera en JVM host

**Estado del alcance PF:** ${report.result.status} (exit ${report.result.exitCode})  
**Ejecución:** \`${report.runId}\` · ${report.startedAt}  
**Origen:** \`${safeText(report.source.branch)}@${safeText(report.source.commit)}\`  
**Cambios locales:** ${tree ? `${tree.hasLocalChanges ? 'sí' : 'no'} · SHA-256 \`${tree.sha256}\` · ${tree.trackedFilesChanged} archivos tracked y ${tree.untrackedFiles} no tracked` : `identidad no disponible: ${safeText(report.source.error)}`}  
**Plataforma:** ${safeText(report.environment.os)} ${safeText(report.environment.arch)} · Node ${safeText(report.environment.node)} · Gradle ${safeText(report.environment.gradleVersion ?? 'no disponible')} · JVM ${safeText(report.environment.jvm ?? 'no disponible')}  
**Comando:** \`${safeText(report.execution.command)}\`  
**Evidencia:** \`${safeText(report.evidenceDirectory)}\`

Este PASS, si se obtiene, cubre sólo los casos PF identificados que se ejecutaron de nuevo sobre JVM host. No acredita Wasm, interacción de APK, interfaz, rendimiento móvil ni inferencia documental.

## Comprobaciones PF

${checks || 'No se ejecutaron comprobaciones.'}

## Cobertura fuera de PF

${pending}

## Artefactos de esta ejecución

${artifacts}

## Archivos de evidencia

${evidenceFiles}

El JSON conserva estados por caso, expectativas, comandos, identidad de fuente, hashes y rutas de evidencia. Las salidas JUnit y los logs son exclusivos de esta ejecución; el comando devuelve un código no exitoso si PF falla o queda incompleto.
`;
}

async function writeEvidence(report, directory) {
  report.finishedAt = new Date().toISOString();
  report.result = summarizeVerification(report.checks);
  await writeFile(path.join(directory, 'report.json'), `${JSON.stringify(report, null, 2)}\n`, { flag: 'wx' });
  await writeFile(path.join(directory, 'report.md'), renderMarkdown(report), { flag: 'wx' });
  console.log(`PF ${report.result.status}. Evidencia: ${directory}`);
  return report.result.exitCode;
}

async function main() {
  const startedAt = new Date();
  const runId = `${startedAt.toISOString().replaceAll(':', '-').replaceAll('.', '-')}-${randomUUID().slice(0, 8)}`;
  const directory = path.join(reportRoot, runId);
  await mkdir(directory, { recursive: true });
  const source = await sourceIdentity();
  const report = {
    schemaVersion: 1,
    runId,
    startedAt: startedAt.toISOString(),
    evidenceDirectory: directory,
    profile: { id: 'PF', name: 'Financial quality and engine checks', scope: 'casos financieros identificados ejecutados en JVM host' },
    source,
    environment: { os: `${os.type()} ${os.release()}`, arch: os.arch(), node: process.version, gradleVersion: null, jvm: null },
    execution: { command: null, status: 'NOT_RUN', exitCode: null },
    checks: [],
    coverageNotRun,
    artifacts: {},
    evidenceFiles: [],
  };

  if (source.error) report.checks.push({ id: 'PF-SOURCE-IDENTITY', status: 'BLOCKED', requirement: 'VERIFY-02', platform: 'host', reason: source.error });

  const versionPath = path.join(directory, 'gradle-version.log');
  const version = await runProcess('gradle', ['--version'], versionPath, false);
  report.evidenceFiles.push('gradle-version.log');
  const versionLine = version.output.match(/^Gradle\s+(\S+)/m)?.[1];
  const jvmLine = version.output.match(/^(?:Launcher JVM|JVM|Daemon JVM):\s*(.+)$/m)?.[1]?.trim();
  report.environment.gradleVersion = versionLine ?? null;
  report.environment.jvm = jvmLine ?? null;

  if (version.exitCode !== 0 || !versionLine) {
    const status = isPrerequisiteFailure(version) || version.exitCode === null ? 'BLOCKED' : 'FAIL';
    report.execution = { command: 'gradle --version', status, exitCode: version.exitCode, reason: version.error?.message ?? version.output.slice(-2000) };
    report.checks.push({ id: 'PF-GRADLE-START', status, requirement: 'VERIFY-02/03', platform: 'JVM host', expected: 'Gradle y JVM identificables para iniciar la ejecución.', actual: report.execution.reason });
    report.checks.push(...evaluateExpectedCases(checkSpecs, [], false));
    report.evidenceFiles.push('report.json', 'report.md');
    process.exitCode = await writeEvidence(report, directory);
    return;
  }

  const patterns = checkSpecs.flatMap(check => ['--tests', `${check.className}.${check.testName}`]);
  const args = [
    '--no-daemon', '--max-workers=1', '-Pkotlin.compiler.execution.strategy=in-process',
    '-Dorg.gradle.jvmargs=-Xmx2g', '-p', 'kmp', ':shared:jvmTest', '--rerun-tasks', ...patterns,
  ];
  report.execution.command = runCommand(args);
  const runStartedAtMs = Date.now();
  const profilePath = path.join(directory, 'gradle-profile.log');
  const execution = await runProcess('gradle', args, profilePath);
  report.evidenceFiles.push('gradle-profile.log');
  const upToDate = /:shared:jvmTest[^\r\n]*\bUP-TO-DATE\b/.test(execution.output);
  const observed = await copyFreshJUnit(runStartedAtMs, directory);
  const fresh = !upToDate && observed.length > 0;
  report.checks.push(...evaluateExpectedCases(checkSpecs, observed, fresh));
  const commandStatus = execution.exitCode === 0 ? 'PASS' : isPrerequisiteFailure(execution) ? 'BLOCKED' : 'FAIL';
  report.execution = {
    command: report.execution.command,
    status: commandStatus,
    exitCode: execution.exitCode,
    signal: execution.signal,
    freshResults: fresh,
    reason: execution.error?.message ?? (execution.exitCode === 0 ? null : execution.output.slice(-2000)),
  };
  report.checks.push({
    id: 'PF-EXECUTION', status: commandStatus, requirement: 'VERIFY-02/03', platform: 'JVM host',
    expectation: 'El comando Gradle termina correctamente y genera resultados de esta ejecución.',
    actual: `exit ${execution.exitCode}; resultados JUnit ${fresh ? 'nuevos' : 'ausentes o no ejecutados'}.`,
  });

  report.artifacts['kmp/shared/build/classes/kotlin/jvm/main'] = await hashTree(path.join(classRoot, 'main'));
  report.artifacts['kmp/shared/build/classes/kotlin/jvm/test'] = await hashTree(path.join(classRoot, 'test'));
  const junitFileHashes = [...new Map(observed.map(item => [item.reportPath, item.reportSha256])).entries()]
    .sort(([left], [right]) => left < right ? -1 : left > right ? 1 : 0);
  report.artifacts['JUnit de la ejecución'] = junitFileHashes.length ? {
    path: 'junit/',
    files: junitFileHashes.length,
    sha256: createHash('sha256').update(junitFileHashes.map(([file, digest]) => `${file}\0${digest}`).join('\n')).digest('hex'),
  } : null;
  report.evidenceFiles.push(...[...new Set(observed.map(item => item.reportPath))]);
  report.evidenceFiles.push('report.json', 'report.md');
  process.exitCode = await writeEvidence(report, directory);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(async error => {
    console.error(error);
    process.exitCode = 1;
  });
}
