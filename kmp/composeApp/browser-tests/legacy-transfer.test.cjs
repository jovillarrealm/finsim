const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFile } = require('node:fs/promises');
const { chromium, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const { waitForStableBox } = require('./stable-box.cjs');

let screenshotSequence = 0;
async function capture(page, label) {
  if (!process.env.FINSIM_WEB_DIAGNOSTICS) return;
  fs.mkdirSync(process.env.FINSIM_WEB_DIAGNOSTICS, { recursive: true });
  const file = path.join(process.env.FINSIM_WEB_DIAGNOSTICS, `${Date.now()}-${process.pid}-${++screenshotSequence}-${label.replace(/[^a-z0-9-]/gi, '-')}.png`);
  await page.screenshot({ path: file });
  fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `${new Date().toISOString()} ${label} box-capture=${path.basename(file)}\n`);
}

test('legacy loan JSON recalculates, exports and survives a rejected import', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 5000 } });
  async function click(page, name) {
    const button = page.getByRole('button', { name, exact: true });
    await expect(button).toBeVisible();
    const box = await waitForStableBox(page, button);
    const viewport = page.viewportSize();
    if (!box || box.x < 0 || box.y < 0 || box.x + box.width > viewport.width || box.y + box.height > viewport.height) {
      await capture(page, `click-failure-${name}`);
      throw new Error(`No hay bounds estables dentro del viewport para ${name}: ${JSON.stringify(box)} viewport=${JSON.stringify(viewport)}`);
    }
    await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
    await page.waitForTimeout(300);
  }
  // Historical React acceptance fixture: currency omitted and an existing extra payment.
  const legacy = { version: 1, name: 'Monedas', source: 'original', principal: '1000', startDate: '2026-01', termMonths: 2, rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [{ id: 'e', kind: 'extra', month: 1, amount: '100' }] };
  async function importJson(value) {
    const [chooser] = await Promise.all([
      page.waitForEvent('filechooser'),
      click(page, 'Importar archivo JSON')
    ]);
    await chooser.setFiles({ name: 'prestamo-anterior.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(value)) });
  }
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await importJson(legacy);
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText(legacy.name);
    await expect(tree).toContainText('COP 1.014,02');
    await click(page, 'Guardar');
    const saved = await page.evaluate(() => localStorage.getItem('finsim.scenario'));
    const [file] = await Promise.all([
      page.waitForEvent('download'),
      click(page, 'Exportar archivo JSON')
    ]);
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
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) {
      if (process.env.FINSIM_WEB_DIAGNOSTICS) fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `capture:error ${captureError.message}\n`);
    }
    throw error;
  } finally { await browser.close(); }
});
