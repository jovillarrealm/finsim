import assert from 'node:assert/strict';
import test from 'node:test';
import { evaluateExpectedCases, parseJUnit } from './verify-financial.mjs';
import { summarizeVerification } from './verification-status.mjs';

test('JUnit cases keep pass, failure, and skipped states identifiable', () => {
  const cases = parseJUnit(`
    <testsuites><testsuite>
      <testcase classname="rates" name="roundtrip" />
      <testcase classname="loan" name="bad"><failure message="mismatch" /></testcase>
      <testcase classname="loan" name="skipped"><skipped /></testcase>
    </testsuite></testsuites>
  `);

  assert.deepEqual(cases, [
    { className: 'rates', name: 'roundtrip', status: 'PASS' },
    { className: 'loan', name: 'bad', status: 'FAIL' },
    { className: 'loan', name: 'skipped', status: 'NOT_RUN' },
  ]);
});

test('an expected case passes only when a fresh matching JUnit case executed', () => {
  const expected = [{ id: 'FIN-01/RATE', className: 'rates', testName: 'roundtrip' }];
  const passed = [{ className: 'rates', name: 'roundtrip', status: 'PASS' }];
  const failed = [{ className: 'rates', name: 'roundtrip', status: 'FAIL' }];

  assert.equal(evaluateExpectedCases(expected, passed, true)[0].status, 'PASS');
  assert.equal(evaluateExpectedCases(expected, failed, true)[0].status, 'FAIL');
  assert.equal(evaluateExpectedCases(expected, passed, false)[0].status, 'NOT_RUN');
  assert.equal(evaluateExpectedCases(expected, [], true)[0].status, 'NOT_RUN');
});

test('KMP JVM platform names still match the requested method and stay identifiable', () => {
  const expected = [{ id: 'FIN-01/RATE', className: 'rates', testName: 'roundtrip' }];
  const observed = [{ className: 'rates', name: 'roundtrip[jvm]', status: 'PASS' }];

  assert.equal(evaluateExpectedCases(expected, observed, true)[0].status, 'PASS');
  assert.match(evaluateExpectedCases(expected, observed, true)[0].actual, /roundtrip\[jvm\]/);
  assert.equal(evaluateExpectedCases(expected, [{ ...observed[0], status: 'FAIL' }], true)[0].status, 'FAIL');
});

test('an incomplete or mixed profile cannot become PASS', () => {
  assert.deepEqual(summarizeVerification([]), { complete: false, exitCode: 1, status: 'INCOMPLETE' });
  assert.equal(summarizeVerification([{ status: 'PASS' }, { status: 'BLOCKED' }]).status, 'INCOMPLETE');
  assert.equal(summarizeVerification([{ status: 'PASS' }, { status: 'NOT_RUN' }]).status, 'INCOMPLETE');
  assert.equal(summarizeVerification([{ status: 'FAIL' }, { status: 'NOT_RUN' }]).status, 'FAIL');
  assert.equal(summarizeVerification([{ status: 'PASS' }, { status: 'NOT_APPLICABLE', reason: 'Fuera del perfil.' }]).status, 'PASS');
  assert.throws(() => summarizeVerification([{ status: 'NOT_APPLICABLE' }]), /motivo/i);
  assert.throws(() => summarizeVerification([{ status: 'UNKNOWN' }]), /estado/i);
});
