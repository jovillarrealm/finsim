const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { readFile } = require('node:fs/promises');
const { chromium, expect } = require('@playwright/test');

let browser;
before(async () => { browser = await chromium.launch({ channel: 'chrome', headless: true }); });
after(async () => { await browser?.close(); });

async function click(page, name) {
  const button = page.getByRole('button', { name, exact: true });
  await expect(button).toBeVisible();
  await button.click({ force: true });
  await page.waitForTimeout(300); // Allow the Compose accessibility geometry to follow the rendered frame.
}
async function enter(page, name, value) {
  const field = page.getByRole('textbox', { name, exact: true });
  await field.click({ force: true });
  await page.waitForTimeout(100); // Wait for Compose to focus its backing input.
  await page.keyboard.press('Control+A');
  await page.waitForTimeout(150);
  await page.keyboard.press('Backspace');
  await expect(field).toHaveText('');
  await page.keyboard.insertText(value);
  await expect(field).toHaveText(value);
}
async function importFile(page, file) {
  const chooser = page.waitForEvent('filechooser');
  await click(page, 'Importar archivo JSON');
  await (await chooser).setFiles(file);
}

test('quotes and actual JSON transfer preserve applied and saved data', async () => {
  const page = await browser.newPage({ viewport: { width: 1280, height: 5000 } });
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await click(page, 'Ofertas');
    await click(page, 'País de la operación: Sin completar');
    await click(page, 'Colombia');
    await expect(page.getByRole('button', { name: 'País de la operación: Colombia', exact: true })).toHaveCount(1);
    await click(page, 'Moneda común: Sin completar');
    await click(page, 'COP');
    await expect(page.getByRole('button', { name: 'Moneda común: COP', exact: true })).toHaveCount(1);
    await enter(page, 'Efectivo neto que necesitas', '1000');
    await enter(page, 'Cantidad de cuotas A', '8');
    await enter(page, 'Importe por cuota A', '150');
    await click(page, 'Cerrar editor de oferta A');
    await expect(page.getByRole('button', { name: 'Editar oferta A', exact: true })).toHaveCount(1);
    await click(page, 'Editar oferta B');
    await expect(page.getByRole('button', { name: 'Cerrar editor de oferta B', exact: true })).toHaveCount(1);
    await enter(page, 'Cantidad de cuotas B', '8');
    await enter(page, 'Importe por cuota B', '140');
    await click(page, 'Cerrar editor de oferta B');
    await click(page, 'Aplicar comparación');
    await click(page, 'Guardar borrador local');
    const downloadPromise = page.waitForEvent('download');
    await click(page, 'Exportar archivo JSON');
    const downloaded = await downloadPromise;
    const downloadPath = await downloaded.path();
    const exported = JSON.parse(await readFile(downloadPath, 'utf8'));
    assert.equal(downloaded.suggestedFilename(), 'comparacion-finsim.json');
    assert.equal(exported.type, 'financing-comparison');
    assert.equal(exported.applied.offers[0].payment.text, '150');
    assert.equal(exported.applied.offers[1].payment.text, '140');

    await click(page, 'Editar oferta A');
    await enter(page, 'Importe por cuota A', '160');
    await click(page, 'Cerrar editor de oferta A');
    await expect(page.getByRole('button', { name: 'Editar oferta A', exact: true })).toHaveCount(1);
    await click(page, 'Aplicar comparación');
    await click(page, 'Guardar borrador local');
    await importFile(page, { name: 'invalid.json', mimeType: 'application/json', buffer: Buffer.from('{oops') });
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Archivo rechazado; los datos previos se conservaron.');
    await expect(tree).toContainText('Cuotas registradas: 1280.00');
    await importFile(page, downloadPath);
    await expect(tree).toContainText('Archivo importado; guarda explícitamente.');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    assert.equal(await page.evaluate(() => JSON.parse(localStorage.getItem('finsim.comparison.v1')).applied.offers[0].payment.text), '160');
    await click(page, 'Guardar borrador local');
    await page.reload();
    await click(page, 'Ofertas');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    await expect(tree).toContainText('Cuotas registradas: 1120.00');
    const comparison = await page.evaluate(() => localStorage.getItem('finsim.comparison.v1'));
    await page.reload();
    await click(page, 'Guardar');
    assert.equal(await page.evaluate(() => localStorage.getItem('finsim.comparison.v1')), comparison);
  } finally { await page.close(); }
});
