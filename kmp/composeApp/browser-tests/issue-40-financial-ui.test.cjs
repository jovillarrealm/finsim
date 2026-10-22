const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');

const url = process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4176/';
function trace(message) {
  if (!process.env.FINSIM_WEB_DIAGNOSTICS) return;
  fs.appendFileSync(path.join(process.env.FINSIM_WEB_DIAGNOSTICS, 'trace.log'), `${new Date().toISOString()} ${message}\n`);
}

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

const tableWheelPoints = new WeakMap();

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
    } else if (focus === 'form') {
      await reveal(page, page.getByLabel('Nombre del escenario', { exact: true }));
    }
    return { browser, context, page };
  } catch (error) {
    await browser.close();
    throw error;
  }
}

async function reveal(page, target) {
  const startedAt = Date.now();
  trace(`reveal:start ${target}`);
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
  if (alreadyVisible) {
    trace(`reveal:visible ${target} via=current ${Date.now() - startedAt}ms`);
    return alreadyVisible;
  }

  const startingBox = await target.count() ? await target.boundingBox() : null;
  if (startingBox?.width > 0 && startingBox.y >= viewport.height - 4) {
    for (let i = 0; i < 40; i++) {
      const box = await stableBox(target);
      if (box) {
        trace(`reveal:visible ${target} via=down-from-current step=${i} ${Date.now() - startedAt}ms`);
        return box;
      }
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
    if (box) {
      trace(`reveal:visible ${target} via=origin-sweep step=${i} ${Date.now() - startedAt}ms`);
      return box;
    }
    const current = await target.count() ? await target.boundingBox() : null;
    positions.push(current);
    const delta = current?.width > 0 && current.y < 56 ? -75 : 150;
    await page.mouse.wheel(0, delta);
    await page.waitForTimeout(200);
  }
  trace(`reveal:fail ${target} positions=${JSON.stringify(positions)} ${Date.now() - startedAt}ms`);
  if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\reveal-failure.png` });
  throw new Error(`Control sin geometría visible tras barrido desde el inicio: ${target} ${JSON.stringify(positions)}`);
}

async function revealHorizontally(page, target) {
  const startedAt = Date.now();
  const viewport = page.viewportSize();
  const anchors = [
    'Mes', 'Fecha', 'Saldo inicial', 'Capital habitual', 'Capital adicional',
    'Interés generado', 'Seguro generado', 'Intereses pagados', 'Seguros pagados',
    'Otros cargos', 'Pago total', 'Saldo final', 'saldo de capital', 'Interés pendiente',
    'Seguro pendiente', 'Capital vencido', 'Mora', 'Deuda', 'No aplicado'
  ];
  trace(`reveal-horizontal:start ${target}`);
  let lastWheelPoint = tableWheelPoints.get(page) ?? null;
  if (lastWheelPoint) trace(`reveal-horizontal:resume-table-pointer point=${lastWheelPoint.x},${lastWheelPoint.y}`);
  for (let i = 0; i < 12; i++) {
    const targetBox = await target.count() ? await target.boundingBox() : null;
    if (targetBox?.width > 0 && targetBox.x >= 0 && targetBox.x + targetBox.width <= viewport.width) {
      trace(`reveal-horizontal:visible ${target} step=${i} ${Date.now() - startedAt}ms`);
      return targetBox;
    }

    let anchorBox = null;
    let anchorLabel = '';
    for (const label of anchors) {
      const candidate = page.getByText(label, { exact: true }).first();
      const box = await candidate.count() ? await candidate.boundingBox() : null;
      if (box?.width > 0 && box.x < viewport.width && box.x + box.width > 0 && box.y >= 56 && box.y + box.height <= viewport.height) {
        anchorBox = box;
        anchorLabel = label;
        break;
      }
    }
    if (!anchorBox && !lastWheelPoint) {
      trace(`reveal-horizontal:no-visible-anchor step=${i} target=${target} ${Date.now() - startedAt}ms`);
      break;
    }
    const point = anchorBox
      ? {
          x: (Math.max(0, anchorBox.x) + Math.min(viewport.width, anchorBox.x + anchorBox.width)) / 2,
          y: anchorBox.y + anchorBox.height / 2
        }
      : lastWheelPoint;
    if (anchorBox) {
      lastWheelPoint = point;
      tableWheelPoints.set(page, point);
    }
    trace(`reveal-horizontal:scroll step=${i} ${anchorBox ? `anchor=${anchorLabel} box=${JSON.stringify(anchorBox)}` : 'at-last-table-pointer'} point=${point.x},${point.y}`);
    await page.mouse.move(point.x, point.y);
    await page.mouse.wheel(220, 0);
    await page.waitForTimeout(200);
  }
  trace(`reveal-horizontal:fail ${target} ${Date.now() - startedAt}ms`);
  if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\horizontal-reveal-failure.png` });
  throw new Error(`Control sin geometría horizontal visible tras desplazar la tabla: ${target}`);
}

async function activate(page, target, touch = false) {
  trace(`activate:start touch=${touch} ${target}`);
  const box = await reveal(page, target);
  trace(`activate:revealed touch=${touch} ${target}`);
  if (touch) await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2);
  else await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(150);
}

async function enter(page, target, value, touch = false) {
  trace(`enter:start touch=${touch} ${target}`);
  await activate(page, target, touch);
  trace(`enter:activated ${target}`);
  await page.keyboard.press('Control+A');
  await page.waitForTimeout(150);
  await page.keyboard.press('Backspace');
  const afterDelete = (await target.textContent())?.trim() ?? '';
  if (afterDelete === '') await expect(target).toHaveText('');
  else {
    await page.keyboard.press('Control+A');
    await page.waitForTimeout(150);
  }
  await page.keyboard.insertText(String(value));
  await expect(target).toHaveText(String(value));
  trace(`enter:complete ${target}`);
}

async function choose(page, label, option, index = 0, touch = false) {
  trace(`choose:start label=${label} option=${option} index=${index} touch=${touch}`);
  // All choice fields exercised by WEB-40 use ComparisonChoice. Its button
  // may leave the Wasm accessibility tree while clipped; reveal it before
  // relying on the locator instead of falling back to an obsolete label.
  const inlineChoice = page.getByRole('button', {
    name: new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}: `)
  }).nth(index);
  const field = inlineChoice;
  await activate(page, field, touch);
  trace(`choose:field-open label=${label} index=${index}`);
  const optionTarget = page.getByRole('button', { name: option, exact: true }).last();
  await reveal(page, optionTarget);
  await expect(optionTarget).toBeVisible();
  await activate(page, optionTarget, touch);
  trace(`choose:selected label=${label} option=${option} index=${index}`);
  await page.waitForTimeout(1000);
}

test('[WEB-40-ORIGIN-A11Y] Keep the form tree while selecting payment origin by keyboard after scrolling', { timeout: 120_000 }, async () => {
  const { browser, page } = await open(twoMonthArs, { focus: 'none' });
  try {
    const origin = page.getByRole('button', { name: 'Origen: Préstamo original', exact: true });
    await page.mouse.move(5, 880);
    // Traverse from page entry with real Tab presses; the shadow-tree proxy
    // divs are not focusable DOM nodes.
    for (let i = 0; i < 49; i++) {
      await page.keyboard.press('Tab');
      await page.waitForTimeout(150);
    }
    // Real wheel input moves the focused row away from the viewport edge so
    // the inline options below it can be seen and selected.
    await page.mouse.move(640, 450);
    for (let i = 0; i < 4; i++) {
      await page.mouse.wheel(0, 150);
      await page.waitForTimeout(150);
    }
    await page.mouse.move(5, 880);
    await expect(origin).toBeVisible();
    await page.keyboard.press('Enter');

    const original = page.getByRole('button', { name: 'Préstamo original', exact: true }).last();
    const current = page.getByRole('button', { name: 'Saldo actual', exact: true }).last();
    const rateType = page.getByRole('button', { name: /^Tipo de tasa: / });
    await expect(original).toBeVisible();
    await expect(current).toBeVisible();
    await expect(rateType).toHaveCount(1);
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    const selectedOrigin = page.getByRole('button', { name: 'Origen: Saldo actual', exact: true });
    await expect(selectedOrigin).toBeVisible();
    await expect(rateType).toHaveCount(1);
    await expect(original).toHaveCount(0);
    await expect(current).toHaveCount(0);

    await page.keyboard.press('Space');
    await expect(original).toBeVisible();
    await expect(current).toBeVisible();
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('button', { name: 'Origen: Préstamo original', exact: true })).toBeVisible();
    await expect(rateType).toHaveCount(1);
    await expect(original).toHaveCount(0);
    await expect(current).toHaveCount(0);
  } finally {
    await browser.close();
  }
});

async function selectedMonth(page, month, touch = false) {
  const input = page.getByRole('textbox', { name: /^Mes seleccionado/ });
  await enter(page, input, month, touch);
  await expect(page.locator('#cmp_a11y_root')).toContainText(`Detalle del mes ${month}`);
}

const copied = (page, name) => page.getByRole('button', { name: `Copiar ${name}`, exact: true });

async function assertTableCell(page, column, month, value) {
  const monthRow = page.getByRole('button', { name: new RegExp(`^Seleccionar M${month}(?: |$)`) });
  const monthRowBox = await monthRow.count() ? await monthRow.boundingBox() : null;
  const viewport = page.viewportSize();
  if (monthRowBox?.width > 0 && monthRowBox.x >= 0 && monthRowBox.x + monthRowBox.width <= viewport.width) {
    await reveal(page, monthRow);
  }
  const cell = page.getByRole('button', {
    name: `Copiar tabla ${column} Modificado mes ${month}`, exact: true
  });
  await revealHorizontally(page, cell);
  await expect(cell).toBeVisible();
  await expect(cell).toContainText(value);
}

test('[WEB-40-CONDITIONS] Configure current balance, nominal rate, agreed payment, insurance and ARS profile through the real form', { timeout: 180_000 }, async () => {
  const { browser, page } = await open(twoMonthArs);
  try {
    await choose(page, 'Origen', 'Saldo actual');
    await choose(page, 'Tipo de tasa', 'Nominal anual');
    await enter(page, page.getByLabel('Tasa de interés (%)', { exact: true }), '12');
    await enter(page, page.getByLabel('Capitalizaciones/año', { exact: true }), '12');
    await enter(page, page.getByLabel('Cuota pactada (opcional)', { exact: true }), '507.51');

    await activate(page, page.getByRole('button', { name: '+ Agregar seguro', exact: true }));
    await enter(page, page.getByLabel('Nombre del seguro', { exact: true }), 'Seguro fijo');
    await choose(page, 'Modalidad', '% del saldo mensual');
    const percentageField = page.getByLabel('Tasa mensual (% saldo)', { exact: true });
    await reveal(page, percentageField);
    await expect(percentageField).toBeVisible();
    await enter(page, page.getByLabel('Tasa mensual (% saldo)', { exact: true }), '0.5');
    await choose(page, 'Modalidad', 'Monto fijo mensual');
    await enter(page, page.getByLabel('Monto mensual (ARS)', { exact: true }), '5');
    await choose(page, 'Vigencia', 'Hasta mes específico');
    await enter(page, page.getByLabel('Último mes de cobertura (1..1200)', { exact: true }), '1');

    const profileToggle = page.getByLabel('Régimen de precancelación argentino', { exact: true });
    await reveal(page, profileToggle);
    await expect(profileToggle).toBeVisible();
    await activate(page, profileToggle);
    await enter(page, page.getByLabel('Fecha originación (AAAA-MM)', { exact: true }), '2025-01');
    await enter(page, page.getByLabel('Plazo original (meses)', { exact: true }), '36');
    await enter(page, page.getByLabel('Comisión prepago (%)', { exact: true }), '3');
    await enter(page, page.getByLabel('Alícuota IVA (%)', { exact: true }), '21');

    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Condiciones actualizadas.');
    await activate(page, page.getByRole('button', { name: 'Guardar', exact: true }));
    await expect(tree).toContainText('Escenario guardado en este dispositivo.');
    const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('finsim.scenario')));
    assert.equal(saved.currency, 'ARS');
    assert.equal(saved.source, 'current');
    assert.deepEqual(saved.rate, { kind: 'nominal', value: '0.12', frequency: 12 });
    assert.equal(saved.payment, '507.51');
    assert.equal(saved.insurance.length, 1);
    assert.equal(saved.insurance[0].kind, 'fixed');
    assert.equal(saved.insurance[0].value, '5');
    assert.equal(saved.insurance[0].endsAtPayoff, false);
    assert.equal(saved.insurance[0].endMonth, 1);
    assert.equal(saved.argentinaProfile.originationDate, '2025-01');
    assert.equal(saved.argentinaProfile.originalTermMonths, 36);
    assert.equal(saved.argentinaProfile.commissionRate, '0.03');
    assert.equal(saved.argentinaProfile.vatRate, '0.21');

    await expect(copied(page, 'Cuota base Modificado')).toContainText('ARS 507,51');
    await expect(copied(page, 'Seguro generado Modificado mes 1')).toContainText('ARS 5,00');
    await expect(copied(page, 'total pagado Modificado mes 1')).toContainText('ARS 512,51');
    await activate(page, page.getByRole('button', { name: 'Ver tabla', exact: true }));
    await assertTableCell(page, 'seguro generado', 2, 'ARS 0,00');
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
    await reveal(page, profileToggle);
    await expect(profileToggle).toBeVisible();
    await activate(page, profileToggle);
    await enter(page, page.getByLabel('Fecha originación (AAAA-MM)', { exact: true }), '2025-01');
    await enter(page, page.getByLabel('Plazo original (meses)', { exact: true }), '36');

    await choose(page, 'Moneda', 'COP');
    await expect(profileToggle).toHaveCount(0);
    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Condiciones actualizadas.');
    await activate(page, page.getByRole('button', { name: 'Guardar', exact: true }));
    await expect(tree).toContainText('Escenario guardado en este dispositivo.');
    const saved = await page.evaluate(() => JSON.parse(localStorage.getItem('finsim.scenario')));
    assert.equal(saved.currency, 'COP');
    assert.ok(saved.argentinaProfile == null, 'El perfil argentino debe quedar ausente o nulo fuera de ARS.');
  } finally {
    await browser.close();
  }
});

test('[WEB-40-EVENT-BATCH] Discard and apply a missed payment, recovery and recurring event from touch controls', { timeout: 240_000 }, async () => {
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
    const recoveryAmount = page.getByLabel('Importe (ARS)', { exact: true }).nth(0);
    const recoveryAmountCount = await recoveryAmount.count();
    trace(`event:recovery-amount-count=${recoveryAmountCount}`);
    trace(`event:recovery-amount-box=${JSON.stringify(await recoveryAmount.boundingBox())}`);
    await enter(page, recoveryAmount, '105', true);
    await selectedMonth(page, 3, true);
    await activate(page, page.getByRole('button', { name: 'Añadir mes', exact: true }), true);
    await choose(page, 'Tipo', 'Abono recurrente', 2, true);
    await enter(page, page.getByLabel('Importe (ARS)', { exact: true }).nth(1), '10', true);
    await enter(page, page.getByLabel('Hasta mes', { exact: true }), '3', true);
    await enter(page, page.getByLabel('Cada N meses', { exact: true }), '1', true);
    await activate(page, page.getByRole('button', { name: 'Aplicar cambios', exact: true }), true);

    await expect(tree).toContainText('Mes seleccionado: 3.');
    await activate(page, page.getByRole('button', { name: 'Ver tabla', exact: true }), true);
    // The event editor extends below the initial viewport. Reveal the real
    // table's first header vertically before scanning its horizontal columns.
    const tableStartHeader = page.getByText('Mes', { exact: true }).first();
    await reveal(page, tableStartHeader);
    await expect(tableStartHeader).toBeVisible();
    await assertTableCell(page, 'interés generado', 1, 'ARS 10,00');
    await assertTableCell(page, 'seguro generado', 1, 'ARS 5,00');
    await assertTableCell(page, 'pago total', 1, 'ARS 0,00');
    await assertTableCell(page, 'pago total', 2, 'ARS 210,00');
    await assertTableCell(page, 'pago total', 3, 'ARS 115,00');
    await assertTableCell(page, 'saldo de capital', 2, 'ARS 820,00');
    await assertTableCell(page, 'saldo de capital', 3, 'ARS 718,20');
  } finally {
    await browser.close();
  }
});

test('[WEB-40-MONTH-SYNC] Touch month selection stays synchronized across chart, summary and table at 360px', { timeout: 120_000 }, async t => {
  const { browser, page } = await open({ ...twoMonthArs, termMonths: 12, payment: '100.00', rate: { kind: 'monthly', value: '0' } }, { mobile: true, focus: 'chart' });
  try {
    const tree = page.locator('#cmp_a11y_root');
    const chart = page.getByLabel('Saldo de capital al cierre; selecciona el mes con el control Mes seleccionado', { exact: true });
    await expect(chart).toBeVisible();
    const before = await chart.boundingBox();
    assert.ok(before && before.x >= 0 && before.y >= 64 && before.width > 0 && before.height > 0);

    await selectedMonth(page, 6, true);
    await expect(tree).toContainText('Detalle del mes 6');
    // A pointer over the chart previews its own month independently of the
    // selected-month detail (observed as chart M4 while selection stayed M6).
    // Move off both canvases before asserting the selected-month chart state.
    await reveal(page, chart);
    await page.mouse.move(5, page.viewportSize().height - 5);
    await page.waitForTimeout(250);
    t.diagnostic('Chart hover preview is separate from selected month; after pointer exit the chart returns to the selected month.');
    await expect(tree).toContainText('Mes seleccionado: 6.');
    await expect(tree).toContainText('Mes M6');
    await expect(tree).toContainText('Saldo: Orig ARS 400,00 / Mod ARS 400,00');
    await expect(tree).toContainText('Capital: ARS 100,00');
    await expect(tree).toContainText('Intereses: ARS 0,00');
    await expect(tree).toContainText('Seguros: ARS 0,00');
    await activate(page, page.getByRole('button', { name: 'Ver tabla', exact: true }), true);
    const tableStartHeader = page.getByText('Mes', { exact: true }).first();
    await reveal(page, tableStartHeader);
    await expect(tableStartHeader).toBeVisible();
    await activate(page, page.getByRole('button', { name: 'Seleccionar M3', exact: true }), true);
    await expect(tree).toContainText('Detalle del mes 3');
    await expect(tree).toContainText('Mes seleccionado: 3.');
    await expect(tree).toContainText('Mes M3');
    await expect(tree).toContainText('Saldo: Orig ARS 700,00 / Mod ARS 700,00');
    await expect(tree).toContainText('Capital: ARS 100,00');
    await expect(tree).toContainText('Intereses: ARS 0,00');
    await expect(tree).toContainText('Seguros: ARS 0,00');
    await expect(page.getByText('No aplicado', { exact: true })).toHaveCount(1);
    await assertTableCell(page, 'Capital habitual', 3, 'ARS 100,00');
    await assertTableCell(page, 'saldo de capital', 3, 'ARS 700,00');

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

test('[WEB-40-TNUM-UI] Equal-length financial values keep the visible result control width stable', { timeout: 120_000 }, async t => {
  const scenario = { ...twoMonthArs, termMonths: 12, payment: '111.11', rate: { kind: 'monthly', value: '0' } };
  const { browser, page } = await open(scenario);
  try {
    const result = copied(page, 'Cuota base Modificado');
    await reveal(page, result);
    await expect(result).toContainText('ARS 111,11');
    await page.mouse.move(5, page.viewportSize().height - 5);
    await page.waitForTimeout(250);
    const first = await result.boundingBox();
    assert.ok(first?.width > 0);
    if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\tnum-111.png` });

    await enter(page, page.getByLabel('Cuota pactada (opcional)', { exact: true }), '888.88');
    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    const tree = page.locator('#cmp_a11y_root');
    await expect(tree).toContainText('Condiciones actualizadas.');
    await reveal(page, result);
    await expect(result).toContainText('ARS 888,88');
    await page.mouse.move(5, page.viewportSize().height - 5);
    await page.waitForTimeout(250);
    const second = await result.boundingBox();
    assert.ok(second?.width > 0);
    if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\tnum-888.png` });

    await enter(page, page.getByLabel('Cuota pactada (opcional)', { exact: true }), '88.88');
    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    await expect(tree).toContainText('Condiciones actualizadas.');
    await reveal(page, result);
    await expect(result).toContainText('ARS 88,88');
    await page.mouse.move(5, page.viewportSize().height - 5);
    await page.waitForTimeout(250);
    const shorter = await result.boundingBox();
    assert.ok(shorter?.width > 0);
    if (process.env.FINSIM_WEB_DIAGNOSTICS) await page.screenshot({ path: `${process.env.FINSIM_WEB_DIAGNOSTICS}\\tnum-88.png` });
    t.diagnostic(`Anchos UI visibles (111,11 / 888,88 / 88,88): ${first.width} / ${second.width} / ${shorter.width}px.`);
    if (Math.abs(first.width - shorter.width) <= 1) {
      t.skip('La caja visible no distingue longitudes de texto; tabularidad queda NOT_RUN.');
      return;
    }
    assert.ok(Math.abs(first.width - second.width) <= 1, `El ancho visible cambió para cifras de igual longitud: ${first.width} → ${second.width}px`);
  } finally {
    await browser.close();
  }
});
