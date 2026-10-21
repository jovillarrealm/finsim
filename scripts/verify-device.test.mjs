import assert from 'node:assert/strict';
import test from 'node:test';
import { chooseDevice, compareInstalledApkCandidate, evaluateFinancialCase, evaluateFinancialObservation, financialRunReadiness, FINANCIAL_RECIPES, foregroundPackageFromDumpsys, isVisibleInDisplay, parseActivityStart, parseFontScale, parseHierarchy, parseMemoryInfo, parseMemoryPressure, parseOptions, runFinancialActionSequence, touchTargetDp } from './verify-device.mjs';
import { summarizeVerification } from './verification-status.mjs';

test('Android hierarchy preserves semantic labels, parent click targets, focus, and pixel bounds', () => {
  const [label, field] = parseHierarchy(`
    <hierarchy><node text="root" clickable="true" bounds="[0,0][480,800]">
      <node text="Tasa (%)" content-desc="Tasa de origen" class="android.widget.EditText" editable="true" focused="true" bounds="[24,100][456,176]" />
      <node text="Confirmar todos los cargos de la oferta A" class="android.widget.CheckBox" checkable="true" checked="false" bounds="[24,200][456,248]" />
    </node></hierarchy>
  `);

  assert.equal(field.text, 'Tasa (%)');
  assert.equal(field.contentDescription, 'Tasa de origen');
  assert.equal(field.focused, true);
  assert.equal(field.editable, true);
  const checkbox = label.children.find(node => node.checkable);
  assert.equal(checkbox?.checked, false);
  assert.equal(checkbox?.checkable, true);
  assert.equal(field.parent, label);
  assert.deepEqual([field.bounds.width, field.bounds.height], [432, 76]);
  assert.equal(isVisibleInDisplay(field, { width: 480, height: 800 }), true);
  assert.equal(isVisibleInDisplay({ ...field, bounds: { ...field.bounds, top: 900, bottom: 976 } }, { width: 480, height: 800 }), false);
  assert.equal(isVisibleInDisplay({ ...field, visibleToUser: false }, { width: 480, height: 800 }), false);
});

test('48 dp touch target uses the selected device density instead of a fixed pixel size', () => {
  assert.deepEqual(touchTargetDp({ width: 144, height: 144 }, 480), { width: 48, height: 48, passes: true });
  assert.equal(touchTargetDp({ width: 144, height: 144 }, 640).passes, false);
  assert.throws(() => touchTargetDp({ width: 48, height: 48 }, 0), /densidad/i);
});

test('UI actions require an explicitly selected device and malformed options are rejected', () => {
  assert.throws(() => parseOptions(['--allow-ui-actions']), /--device/i);
  assert.equal(parseOptions(['--device', 'emulator-5554']).allowUiActions, false);
  assert.equal(parseOptions(['--device', 'emulator-5554', '--allow-ui-actions']).allowUiActions, true);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--start-app']), /--allow-ui-actions/i);
  assert.equal(parseOptions(['--device', 'emulator-5554', '--allow-ui-actions', '--start-app']).startApp, true);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--restart-app']), /--allow-ui-actions/i);
  assert.equal(parseOptions(['--device', 'emulator-5554', '--allow-ui-actions', '--restart-app']).restartApp, true);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--allow-ui-actions', '--start-app', '--restart-app']), /ambos/i);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--run-financial-cases']), /requiere/i);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--allow-ui-actions', '--run-financial-cases']), /--restart-app/i);
  assert.throws(() => parseOptions(['--device', 'emulator-5554', '--allow-ui-actions', '--restart-app', '--run-financial-cases']), /--apk/i);
  assert.equal(parseOptions(['--device', 'emulator-5554', '--apk', 'integrated.apk', '--allow-ui-actions', '--restart-app', '--run-financial-cases']).runFinancialCases, true);
  assert.throws(() => parseOptions(['--apk', 'app.apk']), /--device/i);
  assert.throws(() => parseOptions(['--unknown']), /opción/i);
});

test('device memory records physical total and available pressure averages without inference', () => {
  assert.deepEqual(parseMemoryInfo('MemTotal:       11910616 kB\nMemFree: 100 kB'), {
    memTotalKb: 11910616, memTotalBytes: 12196470784, memTotalGiB: 11.36,
  });
  assert.equal(parseMemoryInfo('MemFree: 100 kB'), null);
  assert.deepEqual(parseMemoryPressure('some avg10=0.19 avg60=0.05 avg300=0.01 total=97715157\nfull avg10=0.19 avg60=0.05 avg300=0.01 total=80249225'), {
    some: { avg10: 0.19, avg60: 0.05, avg300: 0.01, total: 97715157 },
    full: { avg10: 0.19, avg60: 0.05, avg300: 0.01, total: 80249225 },
  });
  assert.equal(parseMemoryPressure('unsupported'), null);
});

test('font scale is parsed as a read-only display setting', () => {
  assert.equal(parseFontScale('1.5\n'), 1.5);
  assert.equal(parseFontScale('0'), null);
  assert.equal(parseFontScale('null'), null);
});

test('opt-in app start records Android launch result fields', () => {
  assert.deepEqual(parseActivityStart('Status: ok\nLaunchState: COLD\nActivity: io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity\nTotalTime: 305'), {
    status: 'ok', activity: 'io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity', launchState: 'COLD', totalTimeMs: 305,
  });
});

test('an available device is never selected implicitly', () => {
  const devices = [{ serial: 'emulator-5554', state: 'device' }, { serial: 'personal-phone', state: 'device' }];
  assert.equal(chooseDevice(devices, null), null);
  assert.equal(chooseDevice(devices, 'personal-phone'), devices[1]);
  assert.equal(chooseDevice(devices, 'missing'), null);
});

test('a report with pending device checks cannot become a pass', () => {
  assert.equal(summarizeVerification([{ status: 'PASS' }, { status: 'NOT_RUN' }]).status, 'INCOMPLETE');
  assert.equal(summarizeVerification([{ status: 'PASS' }, { status: 'BLOCKED' }]).status, 'INCOMPLETE');
  assert.equal(summarizeVerification([{ status: 'FAIL' }, { status: 'NOT_RUN' }]).status, 'FAIL');
});

test('a financial observation cannot pass without an action and an independent numeric match', () => {
  const expected = 12.682503013196972;
  assert.equal(evaluateFinancialObservation({ actionPerformed: false, observedText: '12.682503013196972%', expected, tolerance: 1e-12 }).status, 'NOT_RUN');
  assert.equal(evaluateFinancialObservation({ actionPerformed: true, observedText: '12,682503013196972%', expected, tolerance: 1e-12 }).status, 'PASS');
  assert.equal(evaluateFinancialObservation({ actionPerformed: true, observedText: '12.68%', expected, tolerance: 1e-12 }).status, 'FAIL');
  assert.equal(evaluateFinancialObservation({ actionPerformed: true, observedText: '—', expected, tolerance: 1e-12 }).status, 'FAIL');
});

test('financial recipes define executable actions and literal observations for every requested Android surface', () => {
  assert.deepEqual(FINANCIAL_RECIPES.map(recipe => recipe.id), [
    'ANDROID-LOAN-ORIGINAL', 'ANDROID-LOAN-CURRENT', 'ANDROID-INSURANCE', 'ANDROID-EVENTS',
    'ANDROID-RATE', 'ANDROID-RECONCILIATION', 'ANDROID-OFFERS',
  ]);
  for (const recipe of FINANCIAL_RECIPES) {
    assert.ok(recipe.actions.length > 0, `${recipe.id} must contain actions, not just labels`);
    assert.ok(recipe.outputs.length > 0, `${recipe.id} must require numeric outputs`);
    assert.ok(recipe.actions.some(action => action.primary), `${recipe.id} must trigger its financial operation`);
    assert.ok(recipe.actions.every(action => ['tab', 'reset-example', 'choice', 'field', 'button', 'table', 'event-add', 'check'].includes(action.type)), `${recipe.id} contains an unsupported runner action`);
    assert.ok(recipe.actions.every(action => action.label !== 'Guardar'), `${recipe.id} must not save a test draft`);
    assert.ok(recipe.outputs.every(output => output.semantic && Number.isFinite(output.expected)), `${recipe.id} outputs must be independently labeled numeric literals`);
  }
  assert.equal(FINANCIAL_RECIPES.find(recipe => recipe.id === 'ANDROID-LOAN-ORIGINAL').outputs.find(output => output.semantic.includes('pago total') && output.semantic.endsWith('mes 1')).expected, 507.51);
  assert.equal(FINANCIAL_RECIPES.find(recipe => recipe.id === 'ANDROID-RATE').actions.some(action => action.value === '1'), true);
  assert.equal(FINANCIAL_RECIPES.find(recipe => recipe.id === 'ANDROID-OFFERS').actions.some(action => action.value === '10'), true);
});

test('a financial case passes only after its action and every independent numeric output match', () => {
  const outputs = [
    { semantic: 'tabla pago total Modificado mes 1', expected: 507.51 },
    { semantic: 'tabla saldo de capital Modificado mes 1', expected: 502.49 },
  ];
  assert.equal(evaluateFinancialCase({ actionPerformed: false, outputs, observed: [] }).status, 'NOT_RUN');
  assert.equal(evaluateFinancialCase({ actionPerformed: true, outputs, observed: [{ semantic: outputs[0].semantic, text: '$507,51' }] }).status, 'FAIL');
  assert.equal(evaluateFinancialCase({ actionPerformed: true, outputs, observed: outputs.map(output => ({ ...output, text: `$${output.expected.toFixed(2).replace('.', ',')}` })) }).status, 'PASS');
  assert.equal(evaluateFinancialCase({ actionPerformed: true, outputs, observed: outputs.map((output, index) => ({ ...output, text: `$${(output.expected + (index ? 1 : 0)).toFixed(2).replace('.', ',')}` })) }).status, 'FAIL');
  assert.equal(evaluateFinancialCase({ actionPerformed: true, actionsComplete: false, outputs, observed: outputs.map(output => ({ ...output, text: `$${output.expected.toFixed(2).replace('.', ',')}` })) }).status, 'FAIL');
});

test('a failed action after mutation still requires restart and cannot complete the recipe', async () => {
  let calls = 0;
  const sequence = await runFinancialActionSequence([
    { type: 'reset-example' }, { type: 'button', label: 'Aplicar comparación' },
  ], async (_action, state) => {
    calls += 1;
    state.mutated = true;
    throw new Error('falló la lectura después del toque');
  });
  assert.equal(calls, 1);
  assert.equal(sequence.restoreRequired, true);
  assert.equal(sequence.actionsComplete, false);
  assert.equal(sequence.actions[0].status, 'NOT_RUN');
  assert.equal(sequence.actions[1].status, 'NOT_RUN');
  assert.match(sequence.failure, /falló la lectura/);
  const allOutputsMatch = FINANCIAL_RECIPES[0].outputs.map(output => ({ ...output, text: `$${output.expected.toFixed(2).replace('.', ',')}` }));
  assert.equal(evaluateFinancialCase({ actionPerformed: true, actionsComplete: sequence.actionsComplete, outputs: FINANCIAL_RECIPES[0].outputs, observed: allOutputsMatch }).status, 'FAIL');
});

test('APK candidate comparison accounts for the full installed base and split set', () => {
  const base = { sha256: 'base-hash' };
  const split = { sha256: 'split-hash' };
  assert.equal(compareInstalledApkCandidate(null, [base]).status, 'NOT_RUN');
  assert.equal(compareInstalledApkCandidate({ sha256: 'base-hash' }, [base]).status, 'PASS');
  assert.equal(compareInstalledApkCandidate({ sha256: 'other' }, [base]).status, 'FAIL');
  const splitResult = compareInstalledApkCandidate({ sha256: 'base-hash' }, [base, split]);
  assert.equal(splitResult.status, 'NOT_RUN');
  assert.equal(splitResult.baseMatches, true);
  assert.equal(compareInstalledApkCandidate({ sha256: 'base-hash' }, []).status, 'BLOCKED');
});

test('financial device actions require installed APK bytes and hash-bound producer identity, not runner source equality', () => {
  const producerIdentity = { matchesApkHash: true, sourceCommit: 'product-commit' };
  assert.equal(financialRunReadiness({ candidateStatus: 'PASS', producerIdentity }).ready, true);
  assert.equal(financialRunReadiness({ candidateStatus: 'NOT_RUN', producerIdentity }).ready, false);
  assert.equal(financialRunReadiness({ candidateStatus: 'PASS', producerIdentity: { ...producerIdentity, matchesApkHash: false } }).ready, false);
  assert.equal(financialRunReadiness({ candidateStatus: 'PASS', producerIdentity: { matchesApkHash: true, sourceCommit: null } }).ready, false);
});

test('foreground activity detection supports legacy windows and current Android activity dumps', () => {
  assert.equal(foregroundPackageFromDumpsys('mCurrentFocus=Window{abc u0 io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity}'), 'io.github.jovillarrealm.finsim_alpha');
  assert.equal(foregroundPackageFromDumpsys('topResumedActivity=ActivityRecord{123 u0 io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity t1}'), 'io.github.jovillarrealm.finsim_alpha');
  assert.equal(foregroundPackageFromDumpsys('no resumed activity'), null);
});
