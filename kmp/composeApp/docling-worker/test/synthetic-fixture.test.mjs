import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const fixtureDirectory = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '../../browser-tests/fixtures/docling-synthetic',
);

test('repository fixture is synthetic and retains an independent PDF and image reference', async () => {
  const expected = JSON.parse(await readFile(path.join(fixtureDirectory, 'expected.json'), 'utf8'));
  const pdf = await readFile(path.join(fixtureDirectory, 'synthetic-loan-cop-01.pdf'));
  const png = await readFile(path.join(fixtureDirectory, 'synthetic-loan-cop-01.png'));
  const pdfHash = createHash('sha256').update(pdf).digest('hex');

  assert.equal(expected.synthetic, true);
  assert.equal(expected.humanPrivateReference, false);
  assert.equal(expected.status, 'NOT_RUN');
  assert.equal(pdfHash, expected.pdfSha256);
  assert.deepEqual([...png.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
  assert.deepEqual(expected.expectedBeforeCorrection, {
    originalPrincipal: '1000.00',
    capitalBalance: '502.49',
    originalTermMonths: 2,
    remainingTermMonths: 1,
    ratePercentage: '1.00',
    rateKind: 'monthly',
    basePayment: '507.51',
    totalPayment: '527.51',
    principalPaid: '497.51',
    interestPaid: '10.00',
    insurancePaid: '20.00',
    finalBalance: '502.49',
  });
});
