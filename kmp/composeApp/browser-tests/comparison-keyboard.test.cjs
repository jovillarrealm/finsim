const { test } = require('node:test');
const { chromium, expect } = require('@playwright/test');

test('compact comparison can be completed, corrected and read using keyboard navigation', async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 360, height: 800 } });
  async function tabs(count, key = 'Tab') {
    for (let i = 0; i < count; i++) {
      await page.keyboard.press(key);
      await page.waitForTimeout(150); // Compose applies focus and brings the control into view on its next frame.
    }
  }
  async function space() {
    await page.keyboard.press('Space');
    await page.waitForTimeout(150);
  }
  async function type(name, value, replace = false) {
    const field = page.getByRole('textbox', { name, exact: true });
    await expect(field).toBeInViewport({ ratio: 1 });
    if (replace) {
      await page.keyboard.press('Control+A');
      await page.waitForTimeout(150);
      await page.keyboard.press('Backspace');
      await expect(field).toHaveText('');
    }
    await page.keyboard.insertText(value);
    await expect(field).toHaveText(value);
  }
  try {
    await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
    await page.getByRole('button', { name: 'Ofertas', exact: true }).waitFor();
    await tabs(5); await space();
    await expect(page.getByRole('button', { name: 'País de la operación: Sin completar', exact: true })).toBeVisible();
    await tabs(1); await space(); await tabs(1); await space();
    await expect(page.getByRole('button', { name: 'País de la operación: Colombia', exact: true })).toHaveCount(1);
    await tabs(1); await space(); await tabs(1); await space();
    await expect(page.getByRole('button', { name: 'Moneda común: COP', exact: true })).toHaveCount(1);
    await tabs(2); await type('Efectivo neto que necesitas', '1000');
    await tabs(14); await type('Cantidad de cuotas A', '8');
    await tabs(1); await type('Importe por cuota A', '150');
    await tabs(5); await space();
    await tabs(1); await space();
    await tabs(11); await type('Cantidad de cuotas B', '8');
    await tabs(1); await type('Importe por cuota B', '140');
    await tabs(5); await space();
    await tabs(1); await space();
    await tabs(1); await type('Ingreso neto mensual', '600');
    await tabs(2); await type('Gastos habituales mensuales', '400');
    await tabs(2); await type('Cuotas vigentes mensuales', '100');
    await tabs(2); await space();
    await tabs(3); await space();
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    await expect(tree).toContainText('Cuotas registradas: 1120.00');
    await tabs(12, 'Shift+Tab'); await space();
    await tabs(12); await type('Importe por cuota A', '150.001', true);
    await tabs(17); await space();
    await expect(tree).toContainText('máximo dos decimales');
    await expect(tree).toContainText('Cuotas registradas: 1200.00');
    await tabs(17, 'Shift+Tab'); await type('Importe por cuota A', '160', true);
    await tabs(17); await space();
    await expect(tree).toContainText('Cuotas registradas: 1280.00');
    await tabs(20, 'Shift+Tab');
    await page.keyboard.press('ArrowRight');
    await expect(tree).toContainText('Mes seleccionado: 1');
    await page.keyboard.press('ArrowRight');
    await expect(tree).toContainText('Mes seleccionado: 2');
    await tabs(1); await space();
    await tabs(1); await space();
    await expect(tree).toContainText('Mes seleccionado: 0');
    await tabs(1);
    await expect(page.getByRole('button', { name: 'Seleccionar mes 1', exact: true })).toBeInViewport({ ratio: 1 });
    await space();
    await expect(tree).toContainText('Mes seleccionado: 1');
    await expect(tree).toContainText('Margen mensual: -60.00');
    await expect(tree).toContainText('Margen mensual: -40.00');
  } finally { await browser.close(); }
});
