const { test, before, after } = require('node:test');
const { chromium, expect } = require('@playwright/test');

let browser;
before(async () => { browser = await chromium.launch({ channel: 'chrome', headless: true }); });
after(async () => { await browser?.close(); });

for (const width of [1280, 360]) {
  test(`comparison fields have accessible names and accept keyboard input at ${width}px`, async () => {
    const page = await browser.newPage({ viewport: { width, height: 900 } });
    try {
      await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
      await page.getByRole('button', { name: 'Ofertas', exact: true }).click({ force: true });
      const goal = page.getByRole('textbox', { name: 'Efectivo neto que necesitas', exact: true });
      await expect(goal).toHaveCount(1);
      await goal.click({ force: true });
      await page.keyboard.type('1000');
      await expect(goal).toContainText('1000');
      await expect(page.getByRole('textbox', { name: 'Mes inicial (AAAA-MM, opcional)', exact: true })).toHaveCount(1);
    } finally { await page.close(); }
  });

  test(`choice selection keeps the form accessible and returns keyboard focus at ${width}px`, async () => {
    const page = await browser.newPage({ viewport: { width, height: 900 } });
    try {
      await page.goto(process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/');
      await page.getByRole('button', { name: 'Ofertas', exact: true }).click({ force: true });
      await page.getByRole('button', { name: 'País de la operación: Sin completar', exact: true }).click({ force: true });
      const countryOption = page.getByRole('button', { name: 'Colombia', exact: true });
      await expect(countryOption).toBeVisible();
      await countryOption.click({ force: true });
      const currency = page.getByRole('button', { name: 'Moneda común: Sin completar', exact: true });
      await expect(currency).toHaveCount(1);
      await page.keyboard.press('Space');
      await expect(page.getByRole('button', { name: 'Colombia', exact: true })).toHaveCount(1);
      await page.keyboard.press('Space');
      await expect(page.getByRole('button', { name: 'Colombia', exact: true })).toHaveCount(0);
      await currency.click({ force: true });
      const currencyOption = page.getByRole('button', { name: 'COP', exact: true });
      await expect(currencyOption).toBeVisible();
      await currencyOption.click({ force: true });
      await expect(page.getByRole('button', { name: 'País de la operación: Colombia', exact: true })).toHaveCount(1);
      await expect(page.getByRole('button', { name: 'Moneda común: COP', exact: true })).toHaveCount(1);
      await expect(page.getByRole('textbox', { name: 'Efectivo neto que necesitas', exact: true })).toHaveCount(1);
    } finally { await page.close(); }
  });
}
