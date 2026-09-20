import { expect, test } from '@playwright/test';
import { readFile } from 'node:fs/promises';

test('préstamo, edición de evento, exportación y persistencia mantienen resultados', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Nombre del escenario', { exact: true }).fill('Referencia');
  await page.getByLabel('Capital inicial (COP)', { exact: true }).fill('1000');
  await page.getByLabel('Tipo de tasa', { exact: true }).selectOption('monthly');
  await page.locator('.loan-form').getByLabel('Tasa (%)', { exact: true }).fill('1');
  await page.getByLabel('Plazo (meses)', { exact: true }).fill('2');
  await page.getByRole('button', { name: 'Quitar seguro 1', exact: true }).click();
  await page.getByRole('button', { name: 'Aplicar cambios', exact: true }).click();
  const summary = page.getByRole('region', { name: 'Resumen del préstamo' });
  await expect(summary).toContainText('COP 507,51');
  await expect(summary).toContainText('COP 1.015,02');
  await page.getByLabel('Importe ofrecido (COP)', { exact: true }).fill('100');
  await page.getByRole('button', { name: 'Agregar evento', exact: true }).click();
  await expect(summary).toContainText('COP 1.014,02');
  await expect(page.getByText('Pago extra único · mes 1', { exact: false })).toBeVisible();
  await page.getByRole('button', { name: 'Guardar escenario', exact: true }).click();
  await page.reload();
  await expect(summary).toContainText('COP 1.014,02');
  await expect(page.getByLabel('Nombre del escenario', { exact: true })).toHaveValue('Referencia');
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar', exact: true }).click();
  const download = await downloadPromise;
  const exported = JSON.parse(await readFile((await download.path())!, 'utf8'));
  expect(exported.events).toEqual([expect.objectContaining({ kind: 'extra', month: 1, amount: '100' })]);
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles({ name: 'invalid.json', mimeType: 'application/json', buffer: Buffer.from('{oops') });
  await expect(page.getByRole('alert')).toContainText('JSON válido');
  await expect(summary).toContainText('COP 1.014,02');
  await page.getByRole('button', { name: 'Usar ejemplo', exact: true }).click();
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles({ name: 'reference.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(exported)) });
  await expect(summary).toContainText('COP 1.014,02');
  await page.getByRole('button', { name: 'Editar Pago extra único del mes 1', exact: true }).click();
  await page.getByLabel('Importe ofrecido (COP)', { exact: true }).fill('200');
  await page.getByRole('button', { name: 'Guardar evento', exact: true }).click();
  await expect(summary).toContainText('COP 1.013,02');
  await page.getByRole('button', { name: 'Quitar Pago extra único del mes 1', exact: true }).click();
  await expect(summary).toContainText('COP 1.015,02');
});

test('móvil, teclado, conciliación y conversión funcionan con referencia independiente', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  const input = { version: 1, name: 'Recuperación', source: 'current', principal: '1000', startDate: '2026-01', termMonths: 12,
    payment: '100', rate: { kind: 'monthly', value: '0.01' }, insurance: [{ id: 'i', name: 'Vida', kind: 'fixed', value: '5', endsAtPayoff: true }],
    events: [{ id: 'm', kind: 'missed', month: 1 }, { id: 'c', kind: 'catchup', month: 2, amount: '105' }] };
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles({ name: 'recovery.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(input)) });
  await page.getByText('Detalle mes a mes', { exact: false }).click();
  const secondMonth = page.getByRole('row').filter({ hasText: 'feb de 2026' });
  await expect(secondMonth).toContainText('COP 210,00');
  await expect(secondMonth).toContainText('COP 820,00');
  await expect(page.getByLabel('Capital pendiente (COP)', { exact: true })).toHaveValue('1000');
  await page.getByLabel('Capital pendiente (COP)', { exact: true }).fill('1,001');
  await page.getByRole('button', { name: 'Aplicar cambios', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('última simulación válida');
  await expect(secondMonth).toContainText('COP 820,00');
  const rates = page.getByRole('button', { name: 'Convertir tasas', exact: true });
  await rates.focus(); await rates.press('Enter');
  await expect(page.getByText('12,6825030132 %', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Comparar extracto', exact: true }).click();
  await page.getByLabel('Interés cobrado (COP)', { exact: true }).fill('11,25');
  await page.getByRole('button', { name: 'Comparar fila', exact: true }).click();
  await expect(page.getByRole('table')).toContainText('COP 1,25');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('moneda seleccionada conserva importes, eventos y persistencia sin conversión', async ({ page }) => {
  await page.goto('/');
  const input = { version: 1, name: 'Monedas', source: 'original', principal: '1000', startDate: '2026-01', termMonths: 2,
    rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [{ id: 'e', kind: 'extra', month: 1, amount: '100' }] };
  const file = (data: unknown) => ({ name: 'currency.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(data)) });
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles(file(input));
  const summary = page.getByRole('region', { name: 'Resumen del préstamo' });
  await expect(page.getByLabel('Moneda', { exact: true })).toHaveValue('COP');
  for (const currency of ['ARS', 'USD', 'EUR', 'COP']) {
    await page.getByLabel('Moneda', { exact: true }).selectOption(currency);
    await page.getByRole('button', { name: 'Aplicar cambios', exact: true }).click();
    await expect(summary).toContainText(`${currency} 1.014,02`);
    await expect(page.getByLabel(`Capital inicial (${currency})`, { exact: true })).toHaveValue('1000');
    await expect(page.getByLabel(`Importe ofrecido (${currency})`, { exact: true })).toBeVisible();
    await expect(page.locator('figcaption').first()).toContainText(currency);
    await page.getByRole('button', { name: 'Guardar escenario', exact: true }).click();
    await page.reload();
    await expect(page.getByLabel('Moneda', { exact: true })).toHaveValue(currency);
    await expect(summary).toContainText(`${currency} 1.014,02`);
  }
  await page.getByLabel('Moneda', { exact: true }).selectOption('EUR');
  await page.getByRole('button', { name: 'Aplicar cambios', exact: true }).click();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar', exact: true }).click();
  const download = await downloadPromise;
  const exported = JSON.parse(await readFile((await download.path())!, 'utf8'));
  expect(exported).toMatchObject({ currency: 'EUR', principal: '1000', events: input.events });
  await page.getByRole('button', { name: 'Usar ejemplo', exact: true }).click();
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles(file(exported));
  await expect(summary).toContainText('EUR 1.014,02');
  await page.getByLabel('Archivo de escenario', { exact: true }).setInputFiles(file({ ...exported, currency: 'GBP' }));
  await expect(page.getByRole('alert')).toContainText('datos inválidos');
  await expect(summary).toContainText('EUR 1.014,02');
  await page.getByText('Detalle mes a mes', { exact: false }).click();
  await expect(page.getByRole('table')).toContainText('EUR 402,49');
  await page.getByRole('button', { name: 'Comparar extracto', exact: true }).click();
  await page.getByLabel('Interés cobrado (EUR)', { exact: true }).fill('11');
  await page.getByRole('button', { name: 'Comparar fila', exact: true }).click();
  await expect(page.getByRole('table')).toContainText('EUR 1,00');
});

