const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');

const url = process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4176/';
const twoMonthArs = {
  version: 1,
  name: 'WEB-40 condiciones',
  source: 'original',
  principal: '1000.00',
  startDate: '2026-01',
  termMonths: 2,
  rate: { kind: 'monthly', value: '0.01' },
  currency: 'ARS',
  payment: null,
  insurance: [],
  events: []
};

async function open(scenario, { mobile = false, focus = 'form' } = {}) {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  try {
    const context = await browser.newContext({
      permissions: ['clipboard-read', 'clipboard-write'],
      viewport: { width: mobile ? 360 : 1280, height: mobile ? 800 : 900 },
      isMobile: mobile,
      hasTouch: mobile
    });
    await context.addInitScript(value => {
      localStorage.setItem('finsim.scenario', JSON.stringify(value));
    }, scenario);
    const page = await context.newPage();
    await page.goto(url);
    const loanTab = page.getByRole('button', { name: 'Préstamo', exact: true });
    await expect(loanTab).toBeVisible();
    if (focus === 'chart') {
      const chart = page.getByLabel('Saldo de capital al cierre; selecciona el mes con el control Mes seleccionado', { exact: true });
      await reveal(page, chart);
    } else {
      await reveal(page, page.getByLabel('Nombre del escenario', { exact: true }));
    }
    return { browser, context, page };
  } catch (error) {
    await browser.close();
    throw error;
  }
}

async function reveal(page, target) {
  const viewport = page.viewportSize();
  const topTab = page.getByRole('button', { name: 'Préstamo', exact: true });
  const inViewport = box => box?.width > 0 && box.height > 0 && box.x >= 0 && box.y >= 56 && box.y + box.height <= viewport.height - 4;
  const stableBox = async locator => {
    if (!await locator.count()) return null;
    const first = await locator.boundingBox();
    if (!inViewport(first)) return null;
    await page.waitForTimeout(100);
    const second = await locator.boundingBox();
    if (!inViewport(second)) return null;
    return Math.abs(first.x - second.x) <= 2 && Math.abs(first.y - second.y) <= 2 &&
      Math.abs(first.width - second.width) <= 2 && Math.abs(first.height - second.height) <= 2
      ? second
      : null;
  };

  await page.mouse.move(viewport.width / 2, viewport.height / 2);
  const alreadyVisible = await stableBox(target);
  if (alreadyVisible) return alreadyVisible;

  const startingBox = await target.count() ? await target.boundingBox() : null;
  if (startingBox?.width > 0 && startingBox.y >= viewport.height - 4) {
    for (let i = 0; i < 40; i++) {
      const box = await stableBox(target);
      if (box) return box;
      await page.mouse.wheel(0, 150);
      await page.waitForTimeout(200);
    }
  }

  // Restore an observable origin first. A zero-sized target may be above the
  // current scroll position; always scanning downward from there can miss it.
  for (let i = 0; i < 20; i++) {
    const tabCount = await topTab.count();
    const tabBox = tabCount ? await topTab.boundingBox() : null;
    if (tabBox?.width > 0 && tabBox.y >= 56 && tabBox.y + tabBox.height < viewport.height) {
      await page.waitForTimeout(100);
      const stableTab = await topTab.count() ? await topTab.boundingBox() : null;
      if (stableTab?.width > 0 && Math.abs(stableTab.y - tabBox.y) <= 2) {
        break;
      }
    }
    await page.mouse.wheel(0, -600);
    await page.waitForTimeout(200);
  }
  // Compose Web removes clipped tabs from its accessibility tree during scroll;
  // a bounded upward sweep reaches the visible origin even when the sentinel
  // cannot be queried at intermediate frames.

  const positions = [];
  for (let i = 0; i < 56; i++) {
    const box = await stableBox(target);
    if (box) return box;
    const current = await target.count() ? await target.boundingBox() : null;
    positions.push(current);
    const delta = current?.width > 0 && current.y < 56 ? -75 : 150;
    await page.mouse.wheel(0, delta);
    await page.waitForTimeout(200);
  }
  if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\reveal-failure.png` });
  throw new Error(`Control sin geometría visible tras barrido desde el inicio: ${target} ${JSON.stringify(positions)}`);
}

async function activate(page, target, touch = false) {
  const box = await reveal(page, target);
  if (touch) await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
  else await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(150);
}

async function enter(page, target, value, touch = false) {
  const box = await reveal(page, target);
  const inset = Math.min(40, Math.max(12, box.width / 8));
  if (touch) await page.touchscreen.tap(box.x + inset, box.y + box.height / 2);
  else await page.mouse.click(box.x + inset, box.y + box.height / 2);
  await page.waitForTimeout(100);
  await page.keyboard.press('Control+A');
  await page.keyboard.press('Backspace');
  await expect(target).toHaveText('');
  await page.keyboard.insertText(String(value));
  await expect(target).toHaveText(String(value));
}

async function choose(page, label, option, index = 0, touch = false) {
  const field = page.getByLabel(label, { exact: true }).nth(index);
  await activate(page, field, touch);
  const optionTarget = page.getByRole('button', { name: option, exact: true }).last();
  await expect(optionTarget).toBeVisible();
  await activate(page, optionTarget, touch);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(1000);
}

async function selectedMonth(page, month, touch = false) {
  const input = page.getByRole('textbox', { name: /^Mes seleccionado/ });
  await enter(page, input, month, touch);
  await expect(page.locator('#cmp_a11y_root')).toContainText(`Detalle del mes ${month}`);
}

const copied = (page, name) => page.getByRole('button', { name: `Copiar ${name}`, exact: true });

test('[WEB-40-CONDITIONS] Configure current balance, nominal rate, agreed payment, insurance and ARS profile through the real form', { timeout: 120_000 }, async () => {
  const { browser, page } = await open(twoMonthArs);
  try {
    await choose(page, 'Origen', 'Saldo actual');
    await choose(page, 'Tipo de tasa', 'Nominal anual');
    await enter(page, page.getByLabel('Tasa de interés (%)', { exact: true }), '12');
    await enter(page, page.getByLabel('Capitalizaciones/año', { exact: true }), '12');
    await enter(page, page.getByLabel('Cuota pactada (opcional)', { exact: true }), '507.51');

    await activate(page, page.getByRole('button', { name: '+ Agregar seguro', exact: true }));
    await enter(page, page.getByLabel('Nombre del seguro', { exact: true }), 'Seguro fijo');
    await enter(page, page.getByLabel('Monto mensual ($ARS)', { exact: true }), '5');

    const profileToggle = page.getByLabel('Régimen de precancelación argentino', { exact: true });
    await expect(profileToggle).toBeVisible();
    await activate(page, profileToggle);
    await enter(page, page.getByLabel('Fecha originación (AAAA-MM)', { exact: true }), '2025-01');
    await enter(page, page.getByLabel('Plazo original (meses)', { exact: true }), '36');
    await enter(page, page.getByLabel('Comisión prepago (%)', { exact: true }), '3');
    await enter(page, page.getByLabel('Alícuota IVA (%)', { exact: true }), '21');

    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('finsim.scenario')));
    assert.equal(saved.currency, 'ARS');
    assert.equal(saved.source, 'current');
    assert.deepEqual(saved.rate, { kind: 'nominal', value: '0.12', frequency: 12 });
    assert.equal(saved.payment, '507.51');
    assert.equal(saved.insurance.length, 1);
    assert.equal(saved.insurance[0].kind, 'fixed');
    assert.equal(saved.insurance[0].value, '5');
    assert.equal(saved.argentinaProfile.originationDate, '2025-01');
    assert.equal(saved.argentinaProfile.originalTermMonths, 36);

    await expect(copied(page, 'Cuota base Modificado')).toContainText('ARS 507,51');
    await expect(copied(page, 'Seguro generado Modificado mes 1')).toContainText('ARS 5,00');
    await expect(copied(page, 'total pagado Modificado mes 1')).toContainText('ARS 512,51');
  } finally {
    await browser.close();
  }
});

test('[WEB-40-ARS-GATE] The Argentine prepayment profile is unavailable outside ARS and cannot persist after switching away', { timeout: 120_000 }, async () => {
  const { browser, page } = await open({ ...twoMonthArs, currency: 'COP' });
  try {
    await expect(page.getByLabel('Régimen de precancelación argentino', { exact: true })).toHaveCount(0);
    await expect(page.getByText('Régimen de precancelación argentino', { exact: true })).toHaveCount(0);

    await choose(page, 'Moneda', 'ARS');
    const profileToggle = page.getByLabel('Régimen de precancelación argentino', { exact: true });
    await expect(profileToggle).toBeVisible();
    await activate(page, profileToggle);
    await enter(page, page.getByLabel('Fecha originación (AAAA-MM)', { exact: true }), '2025-01');
    await enter(page, page.getByLabel('Plazo original (meses)', { exact: true }), '36');

    await choose(page, 'Moneda', 'COP');
    await expect(profileToggle).toHaveCount(0);
    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('finsim.scenario')));
    assert.equal(saved.currency, 'COP');
    assert.equal(saved.argentinaProfile, null);
  } finally {
    await browser.close();
  }
});

test('[WEB-40-EVENT-BATCH] Discard and apply a missed payment, recovery and recurring event from touch controls', { timeout: 120_000 }, async () => {
  const scenario = {
    ...twoMonthArs,
    name: 'WEB-40 eventos',
    termMonths: 3,
    payment: '100.00',
    insurance: [{ id: 'seguro', name: 'Seguro', kind: 'fixed', value: '5.00' }]
  };
  const { browser, page } = await open(scenario, { mobile: true });
  try {
    const tree = page.locator('#cmp_a11y_root');
    await activate(page, page.getByRole('button', { name: 'Añadir mes', exact: true }), true);
    await choose(page, 'Tipo', 'Omitir cuota', 0, true);
    await activate(page, page.getByRole('button', { name: 'Descartar cambios', exact: true }), true);
    await expect(tree).toContainText('No hay eventos programados en este escenario.');
    await expect(copied(page, 'total pagado Modificado mes 1')).toContainText('ARS 105,00');

    await activate(page, page.getByRole('button', { name: 'Añadir mes', exact: true }), true);
    await choose(page, 'Tipo', 'Omitir cuota', 0, true);
    await selectedMonth(page, 2, true);
    await activate(page, page.getByRole('button', { name: 'Añadir mes', exact: true }), true);
    await choose(page, 'Tipo', 'Recuperación', 1, true);
    await enter(page, page.getByLabel('Importe ($ARS)', { exact: true }).nth(0), '105', true);
    await selectedMonth(page, 3, true);
    await activate(page, page.getByRole('button', { name: 'Añadir mes', exact: true }), true);
    await choose(page, 'Tipo', 'Abono recurrente', 2, true);
    await enter(page, page.getByLabel('Importe ($ARS)', { exact: true }).nth(1), '10', true);
    await enter(page, page.getByLabel('Hasta mes', { exact: true }), '3', true);
    await enter(page, page.getByLabel('Cada N meses', { exact: true }), '1', true);
    await activate(page, page.getByRole('button', { name: 'Aplicar cambios', exact: true }), true);

    await expect(tree).toContainText('Mes seleccionado: 3.');
    await expect(copied(page, 'total pagado Modificado mes 1')).toContainText('ARS 0,00');
    await expect(copied(page, 'Interés generado Modificado mes 1')).toContainText('ARS 10,00');
    await expect(copied(page, 'Seguro generado Modificado mes 1')).toContainText('ARS 5,00');
    await expect(copied(page, 'total pagado Modificado mes 2')).toContainText('ARS 210,00');
    await expect(copied(page, 'saldo de capital Modificado mes 2')).toContainText('ARS 820,00');
    await expect(copied(page, 'total pagado Modificado mes 3')).toContainText('ARS 115,00');
    await expect(copied(page, 'saldo de capital Modificado mes 3')).toContainText('ARS 718,20');
  } finally {
    await browser.close();
  }
});

test('[WEB-40-MONTH-SYNC] Touch month selection stays synchronized across chart, summary and table at 360px', { timeout: 120_000 }, async () => {
  const { browser, page } = await open({ ...twoMonthArs, termMonths: 12, payment: '100.00', rate: { kind: 'monthly', value: '0' } }, { mobile: true, focus: 'chart' });
  try {
    const tree = page.locator('#cmp_a11y_root');
    const chart = page.getByLabel('Saldo de capital al cierre; selecciona el mes con el control Mes seleccionado', { exact: true });
    await expect(chart).toBeVisible();
    const before = await chart.boundingBox();
    assert.ok(before && before.x >= 0 && before.y >= 64 && before.width > 0 && before.height > 0);

    await selectedMonth(page, 6, true);
    await expect(tree).toContainText('Mes seleccionado: 6.');
    await activate(page, page.getByRole('button', { name: 'Ver tabla', exact: true }), true);
    await expect(page.getByText('Capital habitual', { exact: true }).first()).toBeVisible();
    await activate(page, page.getByRole('button', { name: 'Seleccionar M3', exact: true }), true);
    await expect(tree).toContainText('Detalle del mes 3');
    await expect(tree).toContainText('Mes seleccionado: 3.');
    await expect(page.getByText('No aplicado', { exact: true })).toHaveCount(1);
    const numericCell = page.getByRole('button', { name: 'Copiar tabla Capital habitual Modificado mes 3', exact: true });
    await reveal(page, numericCell);
    const numericFeatures = await numericCell.evaluate(element => [element, ...element.querySelectorAll('*')]
      .map(node => `${getComputedStyle(node).fontFeatureSettings} ${getComputedStyle(node).fontVariantNumeric}`)
      .join(' '));
    assert.match(numericFeatures, /tnum|tabular-nums/i);

    await reveal(page, chart);
    const after = await chart.boundingBox();
    assert.ok(after);
    assert.equal(after.x, before.x);
    assert.equal(after.width, before.width);
    assert.equal(after.height, before.height);
  } finally {
    await browser.close();
  }
});
