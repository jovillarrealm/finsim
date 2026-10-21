import assert from 'node:assert/strict';
import test from 'node:test';
import { chooseDevice, compareInstalledApkCandidate, evaluateFinancialObservation, foregroundPackageFromDumpsys, isVisibleInDisplay, parseActivityStart, parseHierarchy, parseMemoryInfo, parseMemoryPressure, parseOptions, touchTargetDp } from './verify-device.mjs';
import { summarizeVerification } from './verification-status.mjs';

test('Android hierarchy preserves semantic labels, parent click targets, focus, and pixel bounds', () => {
  const [label, field] = parseHierarchy(`
    <hierarchy><node text="root" clickable="true" bounds="[0,0][480,800]">
      <node text="Tasa (%)" content-desc="Tasa de origen" class="android.widget.EditText" editable="true" focused="true" bounds="[24,100][456,176]" />
    </node></hierarchy>
  `);

  assert.equal(field.text, 'Tasa (%)');
  assert.equal(field.contentDescription, 'Tasa de origen');
  assert.equal(field.focused, true);
  assert.equal(field.editable, true);
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

test('foreground activity detection supports legacy windows and current Android activity dumps', () => {
  assert.equal(foregroundPackageFromDumpsys('mCurrentFocus=Window{abc u0 io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity}'), 'io.github.jovillarrealm.finsim_alpha');
  assert.equal(foregroundPackageFromDumpsys('topResumedActivity=ActivityRecord{123 u0 io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity t1}'), 'io.github.jovillarrealm.finsim_alpha');
  assert.equal(foregroundPackageFromDumpsys('no resumed activity'), null);
});
