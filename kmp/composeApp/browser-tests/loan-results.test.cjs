const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const { waitForStableBox } = require('./stable-box.cjs');
const url = process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4176/';
const fixture = { version: 1, name: 'FS18 F01', source: 'original', principal: '1000.00', startDate: '2026-01', termMonths: 2, rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [], currency: 'COP' };
let screenshotSequence = 0;
let clipboardSequence = 0;

async function capture(page, label) {
  if (!process.env.FINSIM_WEB_DIAGNOSTICS) return;
  fs.mkdirSync(process.env.FINSIM_WEB_DIAGNOSTICS, { recursive: true });
  const file = path.join(process.env.FINSIM_WEB_DIAGNOSTICS, `${Date.now()}-${process.pid}-${++screenshotSequence}-${label.replace(/[^a-z0-9-]/gi, '-')}.png`);
  await page.screenshot({ path: file });
  fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `${new Date().toISOString()} ${label} screenshot=${path.basename(file)}\n`);
}

async function markClipboard(page) {
  const marker = `FINSIM-ISSUE40-PENDING-${process.pid}-${Date.now()}-${++clipboardSequence}`;
  await page.evaluate(value => navigator.clipboard.writeText(value), marker);
  return marker;
}

async function waitForClipboard(page, marker, expected) {
  try {
    await expect.poll(() => page.evaluate(() => navigator.clipboard.readText()), { timeout: 5000 }).not.toBe(marker);
    const actual = await page.evaluate(() => navigator.clipboard.readText());
    assert.equal(actual, expected);
    return actual;
  } catch (error) {
    await capture(page, `clipboard-expected-${expected}`);
    throw error;
  }
}

async function copyAndAssert(page, target, expected, touch = false) {
  const marker = await markClipboard(page);
  await click(page, target, touch);
  return waitForClipboard(page, marker, expected);
}

async function reveal(page, target) {
  await target.waitFor({ state: 'attached' });
  await page.mouse.move(180, 450);
  const initial = await target.boundingBox();
  if (!initial?.width) {
    for (let i = 0; i < 30; i++) {
      const top = await button(page, 'Ofertas').boundingBox();
      if (top?.width && top.y >= 64) break;
      await page.mouse.wheel(0, -1200);
      await page.waitForTimeout(300);
    }
  }
  for (let i = 0; i < 100; i++) {
    const box = await waitForStableBox(page, target);
    const height = page.viewportSize().height;
    if (box?.width > 0 && box.x >= 0 && box.y >= 64 && box.y + box.height < height - 8) return box;
    await page.mouse.wheel(0, box?.width > 0 && box.y < 64 ? -200 : 300);
    await page.waitForTimeout(300);
  }
  const box = await target.boundingBox();
  await capture(page, 'reveal-failure');
  throw new Error(`Unreachable control ${target}; lastBox=${JSON.stringify(box)} viewport=${JSON.stringify(page.viewportSize())}`);
}
async function click(page, target, touch = false) {
  const box = await reveal(page, target);
  if (touch) await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
  else await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(150);
}
const button = (page, name) => page.getByRole('button', { name, exact: true });
async function selectMonth(page, month) {
  const input = page.getByRole('textbox', { name: /^Mes seleccionado/ });
  await click(page, input);
  await page.keyboard.press('Control+A');
  await page.waitForTimeout(150);
  await page.keyboard.press('Backspace');
  await expect(input).toHaveText('');
  await page.keyboard.insertText(String(month));
  await expect(input).toHaveText(String(month));
  await page.keyboard.press('Tab');
  await page.waitForTimeout(150);
  await expect(input).toContainText(String(month));
}
async function open(browser, scenario, mobile = false) {
  const context = await browser.newContext({ permissions: ['clipboard-read', 'clipboard-write'], viewport: { width: mobile ? 360 : 1280, height: mobile ? 800 : 900 }, isMobile: mobile, hasTouch: mobile });
  await context.addInitScript(value => { if (!localStorage.getItem('finsim.scenario')) localStorage.setItem('finsim.scenario', JSON.stringify(value)); }, scenario);
  const page = await context.newPage();
  await page.goto(url);
  await button(page, 'Copiar Cuota base Modificado').waitFor({ state: 'attached' });
  return { context, page };
}

test('FS18 real clipboard in summary, detail and table; filters preserve totals and persistence', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  let page;
  try {
    ({ page } = await open(browser, fixture));
    const tree = page.locator('#cmp_a11y_root');
    const saved = await page.evaluate(() => localStorage.getItem('finsim.scenario'));
    await copyAndAssert(page, button(page, 'Copiar Cuota base Modificado'), '507,51');
    await copyAndAssert(page, button(page, 'Copiar total pagado Modificado mes 1'), '507,51');
    await click(page, button(page, 'Intereses pagados: visible'));
    await expect(button(page, 'Copiar importe visible Modificado mes 1')).toContainText('COP 497,51');
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('COP 507,51');
    await click(page, button(page, 'Capital habitual: visible'));
    await expect(tree).toContainText('Sin series visibles');
    await click(page, button(page, 'Restablecer'));
    await expect(button(page, 'Intereses pagados: visible')).toHaveCount(1);
    await click(page, button(page, 'Modificado: visible'));
    await reveal(page, page.getByLabel('Saldo de capital al cierre; selecciona el mes con el control Mes seleccionado', { exact: true }));
    await capture(page, 'fs18-original-dashed');
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('COP 507,51');
    await click(page, button(page, 'Original: visible'));
    await expect(tree).toContainText('Sin series visibles');
    await click(page, button(page, 'Restablecer'));
    await selectMonth(page, 0);
    await click(page, button(page, 'Añadir mes'));
    await expect(tree).not.toContainText('Cambio 1');
    await expect(tree).toContainText('M0: capital inicial, antes de pagos. No admite eventos.');
    await selectMonth(page, 1);
    await click(page, button(page, 'Ver tabla'));
    await copyAndAssert(page, button(page, 'Copiar tabla Capital habitual Modificado mes 1'), '497,51');
    await click(page, button(page, 'Seleccionar M2'));
    await expect(tree).toContainText('Detalle del mes 2');
    await copyAndAssert(page, button(page, 'Copiar saldo de capital Modificado mes 2'), '0,00');
    const deniedMarker = await markClipboard(page);
    await page.evaluate(() => { navigator.clipboard.writeText = () => Promise.reject(new Error('denied')); });
    await click(page, button(page, 'Copiar total pagado Modificado mes 2'));
    await expect(tree).toContainText('No se pudo copiar. Inténtalo de nuevo.');
    await expect(tree).toContainText('Detalle del mes 2');
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), deniedMarker);
    assert.equal(await page.evaluate(() => localStorage.getItem('finsim.scenario')), saved);
    await page.reload();
    await expect(button(page, 'Copiar Cuota base Modificado')).toContainText('COP 507,51');
  } catch (error) {
    try { if (page) await capture(page, 'test-failure'); } catch (captureError) {
      if (process.env.FINSIM_WEB_DIAGNOSTICS) fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `capture:error ${captureError.message}\n`);
    }
    throw error;
  } finally { await browser.close(); }
});

test('FS18 omission separates generated and paid amounts; applying removal clamps the common month', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  let page;
  try {
    ({ page } = await open(browser, { ...fixture, name: 'FS18 omisión',
      insurance: [{ id: 'i', name: 'Seguro', kind: 'fixed', value: '5', endsAtPayoff: true }],
      events: [{ id: 'm', kind: 'missed', month: 1 }] }));
    const tree = page.locator('#cmp_a11y_root');
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('COP 0,00');
    await expect(button(page, 'Copiar Intereses pagados Modificado mes 1')).toContainText('COP 0,00');
    await expect(button(page, 'Copiar Interés generado Modificado mes 1')).toContainText('COP 10,00');
    await expect(button(page, 'Copiar Seguros pagados Modificado mes 1')).toContainText('COP 0,00');
    await expect(button(page, 'Copiar Seguro generado Modificado mes 1')).toContainText('COP 5,00');
    const label = await page.getByRole('textbox', { name: /^Mes seleccionado/ }).getAttribute('aria-label');
    const last = Number(label.match(/–(\d+)/)[1]);
    assert.ok(last > 2);
    await selectMonth(page, last);
    await click(page, button(page, 'Quitar'));
    await expect(button(page, 'Quitar')).toHaveCount(0);
    await expect(tree).toContainText('No hay eventos programados en este escenario.');
    await click(page, button(page, 'Aplicar cambios'));
    await expect(tree).toContainText('Tramo M0–M2 de M0–M2');
    await expect(tree).toContainText('Detalle del mes 2');
    await expect(tree).toContainText('Mes seleccionado: 2.');
  } catch (error) {
    try { if (page) await capture(page, 'test-failure'); } catch (captureError) {
      if (process.env.FINSIM_WEB_DIAGNOSTICS) fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `capture:error ${captureError.message}\n`);
    }
    throw error;
  } finally { await browser.close(); }
});

test('FS18 360px touch navigation reaches M1200 and both canvases select the common month', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  let page;
  try {
    ({ page } = await open(browser, { ...fixture, name: 'FS18 largo', principal: '100000', payment: '1', termMonths: 1200, rate: { kind: 'monthly', value: '0' } }, true));
    const tree = page.locator('#cmp_a11y_root');
    await selectMonth(page, 1200);
    await expect(tree).toContainText('Tramo M1189–M1200 de M0–M1200');
    await click(page, button(page, 'Siguiente'), true);
    await expect(tree).toContainText('Tramo M1189–M1200 de M0–M1200');
    await click(page, button(page, 'Anterior'), true);
    await expect(tree).toContainText('Tramo M1177–M1188 de M0–M1200');
    for (const title of ['Saldo de capital al cierre', 'Pagos mensuales por componente']) {
      const canvas = page.getByLabel(`${title}; selecciona el mes con el control Mes seleccionado`, { exact: true });
      const box = await reveal(page, canvas);
      await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
      await expect(tree).toContainText('Detalle del mes 1183');
      await expect(tree).toContainText('Mes seleccionado: 1183.');
    }
    await copyAndAssert(page, button(page, 'Copiar Cuota base Modificado'), '1,00', true);
    await expect(tree).toContainText('Resultado incompleto: diferencias parciales');
  } catch (error) {
    try { if (page) await capture(page, 'test-failure'); } catch (captureError) {
      if (process.env.FINSIM_WEB_DIAGNOSTICS) fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `capture:error ${captureError.message}\n`);
    }
    throw error;
  } finally { await browser.close(); }
});

test('FS18 pending events survive navigation and only change applied results after Apply', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  let page;
  try {
    ({ page } = await open(browser, fixture));
    const tree = page.locator('#cmp_a11y_root');
    await click(page, button(page, 'Intereses pagados: visible'));
    await click(page, button(page, 'Añadir mes'));
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('507,51');
    await click(page, button(page, 'Conversor'));
    await click(page, button(page, 'Préstamo'));
    await expect(tree).toContainText('Cambio 1 · Mes 1');
    await expect(button(page, 'Intereses pagados: oculto')).toHaveCount(1);
    await selectMonth(page, 2);
    await expect(tree).toContainText('Cambio 1 · Mes 1');
    await click(page, button(page, 'Aplicar cambios'));
    await expect(button(page, 'Capital adicional: visible')).toHaveCount(1);
    await expect(button(page, 'Intereses pagados: oculto')).toHaveCount(1);
    await copyAndAssert(page, button(page, 'Copiar diferencia Caja total Modificado menos Original'), '-1,00');
    await click(page, page.getByRole('textbox', { name: /^Mes seleccionado/ }));
    const keyboardMarker = await markClipboard(page);
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Shift+Tab');
      await page.waitForTimeout(150);
    }
    await page.keyboard.press('Space');
    await page.waitForTimeout(150);
    await waitForClipboard(page, keyboardMarker, 'Mes 2 de amortización');
    await selectMonth(page, 1);
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('607,51');
    await expect(button(page, 'Copiar Capital adicional Modificado mes 1')).toContainText('100,00');
    await copyAndAssert(page, button(page, 'Copiar Caja total pagada Modificado'), '1014,02');
  } catch (error) {
    try { if (page) await capture(page, 'test-failure'); } catch (captureError) {
      if (process.env.FINSIM_WEB_DIAGNOSTICS) fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `capture:error ${captureError.message}\n`);
    }
    throw error;
  } finally { await browser.close(); }
});
