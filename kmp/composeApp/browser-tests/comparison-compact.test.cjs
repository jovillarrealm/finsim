const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');

let browser;
before(async () => { browser = await chromium.launch({ channel: 'chrome', headless: true }); });
after(async () => { await browser?.close(); });

async function reveal(page, locator) {
  const positions = [];
  await page.mouse.move(180, 400);
  for (let i = 0; i < 30; i++) {
    const top = await page.getByRole('button', { name: 'Ofertas', exact: true }).boundingBox();
    if (top?.width > 0 && top.y >= 56) break;
    await page.mouse.wheel(0, -600);
    await page.waitForTimeout(300);
  }
  for (let i = 0; i < 60; i++) {
    const box = await locator.boundingBox();
    positions.push(box);
    if (box?.width > 0 && box.y >= 56 && box.y + box.height <= 780) return box;
    await page.mouse.wheel(0, box?.width > 0 && box.y < 56 ? -200 : 400);
    await page.waitForTimeout(300);
  }
  await page.screenshot({ path: '.scratch/financing-offer-comparison/verification/compact-scroll-failure.png' });
  throw new Error('Control could not be reached by scrolling: ' + locator + ' ' + JSON.stringify(positions.slice(-4)));
}
async function click(page, name) {
  const box = await reveal(page, page.getByRole('button', { name, exact: true }));
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(300);
}
async function enter(page, name, text) {
  const field = page.getByRole('textbox', { name, exact: true });
  const box = await reveal(page, field);
  await page.mouse.click(box.x + 40, box.y + 35);
  await page.waitForTimeout(150);
  await page.keyboard.press('Control+A');
  await page.waitForTimeout(150);
  await page.keyboard.press('Backspace');
  await expect(field).toHaveText('');
  await page.keyboard.insertText(text);
  await expect(field).toHaveText(text);
}

test('Tab leaves an input without losing keyboard activation at 360x800', async () => {
  const page = await browser.newPage({ viewport: { width: 360, height: 800 }, isMobile: true, hasTouch: true });
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await page.getByRole('button', { name: 'Ofertas', exact: true }).waitFor();
    await click(page, 'Ofertas');
    const goal = page.getByRole('textbox', { name: 'Efectivo neto que necesitas', exact: true });
    await goal.waitFor();
    const box = await goal.boundingBox();
    assert.ok(box && box.y > 56 && box.y + box.height < 800);
    await page.mouse.click(box.x + 40, box.y + 35);
    await page.keyboard.insertText('1000');
    await expect(goal).toContainText('1000');
    await page.keyboard.press('Tab');
    await page.waitForTimeout(150); // Compose removes the backing input on its next frame.
    await page.keyboard.press('Space');
    await expect(page.getByRole('textbox', { name: 'Fuente: Efectivo neto que necesitas', exact: true })).toHaveCount(1);
    await page.keyboard.press('Space');
    await expect(page.getByRole('textbox', { name: 'Fuente: Efectivo neto que necesitas', exact: true })).toHaveCount(0);
    await page.keyboard.press('Tab');
    await page.waitForTimeout(150);
    await page.keyboard.insertText('2026-10');
    await expect(page.getByRole('textbox', { name: 'Mes inicial (AAAA-MM, opcional)', exact: true })).toContainText('2026-10');
    await page.keyboard.press('Shift+Tab');
    await page.waitForTimeout(150);
    await page.keyboard.press('Space');
    await expect(page.getByRole('textbox', { name: 'Fuente: Efectivo neto que necesitas', exact: true })).toHaveCount(1);
  } finally { await page.close(); }
});

test('compact quotes, validation, context, pending edits and recovery use visible controls', async () => {
  const page = await browser.newPage({ viewport: { width: 360, height: 800 }, isMobile: true, hasTouch: true });
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await page.getByRole('button', { name: 'Ofertas', exact: true }).waitFor();
    await click(page, 'Ofertas');
    await click(page, 'País de la operación: Sin completar');
    await click(page, 'Colombia');
    await click(page, 'Moneda común: Sin completar');
    await click(page, 'COP');
    await enter(page, 'Efectivo neto que necesitas', '1000');
    await enter(page, 'Cantidad de cuotas A', '8');
    await enter(page, 'Importe por cuota A', '150');
    await click(page, 'Confirmar todos los cargos de la oferta A');
    await click(page, 'Cerrar editor de oferta A');
    await click(page, 'Editar oferta B');
    await enter(page, 'Cantidad de cuotas B', '8');
    await enter(page, 'Importe por cuota B', '140');
    await click(page, 'Confirmar todos los cargos de la oferta B');
    await click(page, 'Cerrar editor de oferta B');
    await click(page, 'Añadir contexto personal opcional');
    await enter(page, 'Ingreso neto mensual', '600');
    await enter(page, 'Gastos habituales mensuales', '400');
    await enter(page, 'Cuotas vigentes mensuales', '100');
    await click(page, 'Aplicar comparación');
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    await expect(tree).toContainText('Cuotas registradas: 1120.00');
    await click(page, 'Editar oferta A');
    await enter(page, 'Importe por cuota A', '150.001');
    await click(page, 'Aplicar comparación');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    await expect(tree).toContainText('decimales');
    await enter(page, 'Importe por cuota A', '160');
    await click(page, 'Aplicar comparación');
    await expect(tree).toContainText('Cuotas registradas: 1280.00');
    await click(page, 'Editar oferta A');
    await enter(page, 'Importe por cuota A', '170');
    await click(page, 'Guardar borrador local');
    await page.reload();
    await page.getByRole('button', { name: 'Ofertas', exact: true }).waitFor();
    await click(page, 'Ofertas');
    await expect(tree).toContainText('Cuotas registradas: 1280.00');
    await expect(page.getByRole('textbox', { name: 'Importe por cuota A', exact: true })).toContainText('170');
    await click(page, 'Descartar cambios');
    await expect(page.getByRole('textbox', { name: 'Importe por cuota A', exact: true })).toContainText('160');
    await expect(tree).toContainText('Borrador con trabajo sin guardar.');
    await click(page, 'Cerrar editor de oferta A');
    await click(page, 'Mostrar tabla mensual accesible');
    const month0 = await reveal(page, page.getByRole('button', { name: 'Seleccionar mes 0', exact: true }));
    await page.mouse.move(month0.x + 80, month0.y + 30);
    const month1 = page.getByRole('button', { name: 'Seleccionar mes 1', exact: true });
    let box;
    for (let i = 0; i < 12; i++) {
      box = await month1.boundingBox();
      if (box?.width > 0 && box.y >= 56 && box.y + box.height < 780) break;
      await page.mouse.wheel(0, box?.width > 0 && box.y < 56 ? -100 : 150);
      await page.waitForTimeout(300);
    }
    assert.ok(box && box.width > 0 && box.y >= 56 && box.y + box.height < 800);
    await page.mouse.click(box.x + 50, box.y + box.height / 2);
    await expect(tree).toContainText('Mes seleccionado: 1');
    await expect(tree).toContainText('Margen mensual: -60.00');
    await expect(tree).toContainText('Margen mensual: -40.00');
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), 360);
  } finally { await page.close(); }
});
