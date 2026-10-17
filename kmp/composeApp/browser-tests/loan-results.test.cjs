const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');
const url = process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4176/';
const fixture = { version: 1, name: 'FS18 F01', source: 'original', principal: '1000.00', startDate: '2026-01', termMonths: 2, rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [], currency: 'COP' };

async function reveal(page, target) {
  await target.waitFor();
  await page.mouse.move(180, 450);
  for (let i = 0; i < 100; i++) {
    const box = await target.boundingBox();
    const height = page.viewportSize().height;
    if (box?.width > 0 && box.x >= 0 && box.y >= 64 && box.y + box.height < height - 8) return box;
    await page.mouse.wheel(0, box?.width > 0 && box.y < 64 ? -350 : 500);
    await page.waitForTimeout(80);
  }
  throw new Error('Unreachable control: ' + target);
}
async function click(page, target, touch = false) {
  const box = await reveal(page, target);
  if (touch) await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
  else await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(120);
}
const button = (page, name) => page.getByRole('button', { name, exact: true });
async function selectMonth(page, month) {
  const input = page.getByRole('textbox', { name: /^Mes seleccionado/ });
  await click(page, input);
  await page.keyboard.press('Control+A');
  await page.keyboard.press('Backspace');
  await page.keyboard.insertText(String(month));
  await page.keyboard.press('Tab');
  await expect(input).toContainText(String(month));
}
async function open(browser, scenario, mobile = false) {
  const context = await browser.newContext({ permissions: ['clipboard-read', 'clipboard-write'], viewport: { width: mobile ? 360 : 1280, height: mobile ? 800 : 900 }, isMobile: mobile, hasTouch: mobile });
  await context.addInitScript(value => { if (!localStorage.getItem('finsim.scenario')) localStorage.setItem('finsim.scenario', JSON.stringify(value)); }, scenario);
  const page = await context.newPage();
  await page.goto(url);
  await button(page, 'Copiar Cuota base Modificado').waitFor();
  return { context, page };
}

test('FS18 real clipboard in summary, detail and table; filters preserve totals and persistence', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const { page } = await open(browser, fixture);
    const tree = page.locator('#cmp_a11y_root');
    const saved = await page.evaluate(() => localStorage.getItem('finsim.scenario'));
    await click(page, button(page, 'Copiar Cuota base Modificado'));
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), '507,51');
    await click(page, button(page, 'Copiar total pagado Modificado mes 1'));
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), '507,51');
    await click(page, page.getByRole('checkbox', { name: 'Intereses pagados', exact: true }));
    await expect(button(page, 'Copiar importe visible Modificado mes 1')).toContainText('COP 497,51');
    await expect(button(page, 'Copiar total pagado Modificado mes 1')).toContainText('COP 507,51');
    await click(page, page.getByRole('checkbox', { name: 'Capital habitual', exact: true }));
    await expect(tree).toContainText('Sin series visibles');
    await click(page, button(page, 'Restablecer'));
    await expect(page.getByRole('checkbox', { name: 'Intereses pagados', exact: true })).toBeChecked();
    await selectMonth(page, 0);
    await expect(button(page, 'Añadir mes')).toBeDisabled();
    await expect(tree).toContainText('M0: capital inicial, antes de pagos. No admite eventos.');
    await selectMonth(page, 1);
    await click(page, button(page, 'Ver tabla'));
    await click(page, button(page, 'Copiar Capital habitual Modificado mes 1'));
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), '497,51');
    await click(page, button(page, 'Seleccionar M2'));
    await expect(tree).toContainText('Detalle del mes 2');
    await page.evaluate(() => { navigator.clipboard.writeText = () => Promise.reject(new Error('denied')); });
    await click(page, button(page, 'Copiar total pagado Modificado mes 2'));
    await expect(tree).toContainText('No se pudo copiar. Inténtalo de nuevo.');
    await expect(tree).toContainText('Detalle del mes 2');
    assert.equal(await page.evaluate(() => localStorage.getItem('finsim.scenario')), saved);
    await page.reload();
    await expect(button(page, 'Copiar Cuota base Modificado')).toContainText('COP 507,51');
  } finally { await browser.close(); }
});

test('FS18 360px touch navigation reaches M1200 and both canvases select the common month', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const { page } = await open(browser, { ...fixture, name: 'FS18 largo', principal: '100000', payment: '1', termMonths: 1200, rate: { kind: 'monthly', value: '0' } }, true);
    const tree = page.locator('#cmp_a11y_root');
    await selectMonth(page, 1200);
    await expect(tree).toContainText('Tramo M1189–M1200 de M0–M1200');
    await expect(button(page, 'Siguiente')).toBeDisabled();
    await click(page, button(page, 'Anterior'), true);
    await expect(tree).toContainText('Tramo M1177–M1188 de M0–M1200');
    for (const title of ['Saldo de capital al cierre', 'Pagos mensuales por componente']) {
      const canvas = page.getByLabel(`${title}; selecciona el mes con el control Mes seleccionado`, { exact: true });
      const box = await reveal(page, canvas);
      await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
      await expect(tree).toContainText('Detalle del mes 1183');
      await expect(tree).toContainText('Mes seleccionado: 1183.');
    }
    await click(page, button(page, 'Copiar Cuota base Modificado'), true);
    assert.equal(await page.evaluate(() => navigator.clipboard.readText()), '1,00');
    await expect(tree).toContainText('Resultado incompleto: diferencias parciales');
  } finally { await browser.close(); }
});
