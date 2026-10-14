const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFile } = require('node:fs/promises');
const { chromium, expect } = require('@playwright/test');

test('legacy loan JSON recalculates, exports and survives a rejected import', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  // Historical React acceptance fixture: currency omitted and an existing extra payment.
  const legacy = { version: 1, name: 'Monedas', source: 'original', principal: '1000', startDate: '2026-01', termMonths: 2, rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [{ id: 'e', kind: 'extra', month: 1, amount: '100' }] };
  async function importJson(value) {
    const chooser = page.waitForEvent('filechooser');
    await page.getByRole('button', { name: 'Importar archivo JSON', exact: true }).click({ force: true });
    await (await chooser).setFiles({ name: 'prestamo-anterior.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(value)) });
  }
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await importJson(legacy);
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText(legacy.name);
    await expect(tree).toContainText('COP 1.014,02');
    await page.getByRole('button', { name: 'Guardar', exact: true }).click({ force: true });
    const saved = await page.evaluate(() => localStorage.getItem('finsim.scenario'));
    const download = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar archivo JSON', exact: true }).click({ force: true });
    const file = await download;
    assert.equal(file.suggestedFilename(), 'escenario-finsim.json');
    const exported = JSON.parse(await readFile(await file.path(), 'utf8'));
    assert.equal(exported.name, legacy.name);
    assert.equal(exported.principal, '1000');
    assert.equal(exported.currency, 'COP');
    assert.deepEqual(exported.events, legacy.events);
    assert.equal(exported.original, undefined);
    await importJson({ ...legacy, version: 99 });
    await expect(tree).toContainText('versión');
    await expect(tree).toContainText(legacy.name);
    assert.equal(await page.evaluate(() => localStorage.getItem('finsim.scenario')), saved);
    await page.reload();
    await expect(tree).toContainText(legacy.name);
    await expect(tree).toContainText('COP 1.014,02');
  } finally { await browser.close(); }
});
