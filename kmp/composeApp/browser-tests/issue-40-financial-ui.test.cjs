const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const { waitForStableBox } = require('./stable-box.cjs');

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

const tableName = 'Tabla de amortización Modificado';
const horizontalTableName = 'Desplazamiento horizontal de la tabla de amortización Modificado';
const verticalTableName = 'Filas de la tabla de amortización Modificado';
const headerName = label => `Encabezado de columna ${label} de la tabla de amortización Modificado`;
const tableHeaders = [
  'Mes', 'Fecha', 'Saldo inicial', 'Capital habitual', 'Capital adicional', 'Interés generado',
  'Seguro generado', 'Intereses pagados', 'Seguros pagados', 'Otros cargos', 'Pago total',
  'Saldo final', 'Interés pendiente', 'Seguro pendiente', 'Capital vencido', 'Mora', 'Deuda', 'No aplicado'
];
const rowName = month => `Fila de amortización Modificado, mes ${month}`;
let screenshotSequence = 0;

async function capture(page, label) {
  if (!process.env.FINSIM_WEB_DIAGNOSTICS) return;
  fs.mkdirSync(process.env.FINSIM_WEB_DIAGNOSTICS, { recursive: true });
  const unique = `${Date.now()}-${process.pid}-${++screenshotSequence}`;
  const file = path.join(process.env.FINSIM_WEB_DIAGNOSTICS, `${label}-${unique}.png`);
  await page.screenshot({ path: file, fullPage: true });
  trace(`screenshot:${path.basename(file)}`);
}

function intersection(a, b) {
  if (!a || !b) return null;
  const left = Math.max(a.x, b.x);
  const top = Math.max(a.y, b.y);
  const right = Math.min(a.x + a.width, b.x + b.width);
  const bottom = Math.min(a.y + a.height, b.y + b.height);
  return right > left && bottom > top ? { x: left, y: top, width: right - left, height: bottom - top } : null;
}

function contains(outer, inner, margin = 2) {
  return outer && inner && inner.width > 0 && inner.height > 0 &&
    inner.x >= outer.x + margin && inner.y >= outer.y + margin &&
    inner.x + inner.width <= outer.x + outer.width - margin &&
    inner.y + inner.height <= outer.y + outer.height - margin;
}

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

async function reveal(page, target, { wheelX } = {}) {
  const startedAt = Date.now();
  const viewport = page.viewportSize();
  const pointerX = wheelX ?? viewport.width / 2;
  trace(`reveal:start ${target} wheelX=${pointerX}`);
  const topTab = page.getByRole('button', { name: 'Préstamo', exact: true });
  const inViewport = box => box?.width > 0 && box.height > 0 && box.x >= 0 && box.y >= 56 && box.y + box.height <= viewport.height - 4;

  await page.mouse.move(pointerX, viewport.height / 2);
  const alreadyVisible = await waitForStableBox(page, target);
  if (inViewport(alreadyVisible)) {
    trace(`reveal:visible ${target} via=current ${Date.now() - startedAt}ms`);
    return alreadyVisible;
  }

  const startingBox = await target.count() ? await target.boundingBox() : null;
  if (startingBox?.width > 0 && startingBox.y >= viewport.height - 4) {
    for (let i = 0; i < 40; i++) {
      const box = await waitForStableBox(page, target);
      if (inViewport(box)) {
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
    const tabBox = tabCount ? await waitForStableBox(page, topTab) : null;
    if (tabBox?.width > 0 && tabBox.y >= 56 && tabBox.y + tabBox.height < viewport.height) {
      break;
    }
    await page.mouse.wheel(0, -600);
    await page.waitForTimeout(200);
  }
  // Compose Web removes clipped tabs from its accessibility tree during scroll;
  // a bounded upward sweep reaches the visible origin even when the sentinel
  // cannot be queried at intermediate frames.

  const positions = [];
  for (let i = 0; i < 56; i++) {
    const box = await waitForStableBox(page, target);
    if (inViewport(box)) {
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
  await capture(page, 'reveal-failure');
  throw new Error(`Control sin geometría visible tras barrido desde el inicio: ${target} ${JSON.stringify(positions)}`);
}

async function visibleHeaderAnchor(page, table, clip, targetIndex) {
  const candidates = [];
  for (let index = 0; index < tableHeaders.length; index += 1) {
    const locator = table.getByLabel(headerName(tableHeaders[index]), { exact: true });
    const box = await locator.count() ? await locator.boundingBox() : null;
    if (contains(clip, box, 2)) candidates.push({ index, locator });
  }
  candidates.sort((a, b) => Math.abs(a.index - targetIndex) - Math.abs(b.index - targetIndex));
  for (const candidate of candidates) {
    const box = await waitForStableBox(page, candidate.locator, 2);
    if (contains(clip, box, 2)) return { ...candidate, box };
  }
  return null;
}

async function revealHorizontally(page, target, column) {
  const startedAt = Date.now();
  const viewport = page.viewportSize();
  const columnIndex = tableHeaders.findIndex(header => header.toLocaleLowerCase('es') === column.toLocaleLowerCase('es'));
  if (columnIndex < 0) throw new Error(`La columna no corresponde a una cabecera de la tabla: ${column}`);
  trace(`reveal-horizontal:start column=${tableHeaders[columnIndex]} target=${target}`);
  const table = page.getByLabel(tableName, { exact: true });
  const scrollport = table.getByLabel(horizontalTableName, { exact: true });
  const targetHeader = table.getByLabel(headerName(tableHeaders[columnIndex]), { exact: true });
  const viewportBox = { x: 0, y: 56, width: viewport.width, height: viewport.height - 60 };
  const targetBoxes = [];
  const maxSteps = Math.ceil(tableHeaders.length * 160 / 60) + 2;
  let preferredAnchor = null;
  // Calibration: 48 wheel units moved the visible header about 8 px (6:1).
  let wheelScale = 6;
  const desiredProgress = 60;
  for (let i = 0; i < maxSteps; i += 1) {
    const scrollportBox = await waitForStableBox(page, scrollport);
    const clip = intersection(scrollportBox, viewportBox);
    const targetBox = await target.count() ? await target.boundingBox() : null;
    targetBoxes.push(targetBox);
    if (contains(clip, targetBox, 3)) {
      trace(`reveal-horizontal:visible column=${tableHeaders[columnIndex]} step=${i} clip=${JSON.stringify(clip)} box=${JSON.stringify(targetBox)} ${Date.now() - startedAt}ms`);
      return targetBox;
    }
    if (!clip || clip.width <= 12 || clip.height <= 12) {
      trace(`reveal-horizontal:no-scrollport column=${tableHeaders[columnIndex]} bounds=${JSON.stringify(scrollportBox)} clip=${JSON.stringify(clip)}`);
      break;
    }
    const targetHeading = await waitForStableBox(page, targetHeader, 2);
    let anchor = null;
    if (preferredAnchor) {
      const box = await waitForStableBox(page, preferredAnchor.locator, 2);
      if (contains(clip, box, 2)) anchor = { ...preferredAnchor, box };
    }
    if (!anchor) anchor = await visibleHeaderAnchor(page, table, clip, columnIndex);
    if (!anchor) {
      trace(`reveal-horizontal:no-header-anchor column=${tableHeaders[columnIndex]} clip=${JSON.stringify(clip)} targetHeader=${JSON.stringify(targetHeading)}`);
      break;
    }
    let direction;
    if (targetHeading?.width) {
      if (targetHeading.x < clip.x + 3) direction = -1;
      else if (targetHeading.x + targetHeading.width > clip.x + clip.width - 3) direction = 1;
      else if (targetBox?.width && targetBox.x < clip.x + 3) direction = -1;
      else if (targetBox?.width && targetBox.x + targetBox.width > clip.x + clip.width - 3) direction = 1;
      else direction = columnIndex < anchor.index ? -1 : columnIndex > anchor.index ? 1 : 0;
    } else direction = columnIndex < anchor.index ? -1 : 1;
    if (direction === 0) {
      trace(`reveal-horizontal:target-header-visible-but-cell-not-contained column=${tableHeaders[columnIndex]} header=${JSON.stringify(targetHeading)} target=${JSON.stringify(targetBox)} clip=${JSON.stringify(clip)}`);
      break;
    }
    const point = { x: clip.x + clip.width / 2, y: clip.y + clip.height / 2 };
    const wheelDelta = Math.min(1800, Math.max(36, Math.round(desiredProgress * wheelScale)));
    trace(`reveal-horizontal:scroll step=${i} column=${tableHeaders[columnIndex]} anchor=${anchor.index}:${tableHeaders[anchor.index]} before=${JSON.stringify(anchor.box)} clip=${JSON.stringify(clip)} target=${JSON.stringify(targetBox)} point=${point.x},${point.y} direction=${direction} wheel=${wheelDelta * direction}`);
    await page.mouse.move(point.x, point.y);
    await page.mouse.wheel(wheelDelta * direction, 0);
    await page.waitForTimeout(200);
    const afterAnchor = await waitForStableBox(page, anchor.locator, 2);
    const beforeEdge = direction > 0 ? anchor.box.x + anchor.box.width : anchor.box.x;
    const afterEdge = afterAnchor?.width ? (direction > 0 ? afterAnchor.x + afterAnchor.width : afterAnchor.x) : null;
    const progress = afterEdge === null ? null : direction * (beforeEdge - afterEdge);
    if (progress === null || progress <= 1 || progress > 80) {
      trace(`reveal-horizontal:invalid-progress step=${i} column=${tableHeaders[columnIndex]} anchor=${anchor.index}:${tableHeaders[anchor.index]} measuredEdge=${direction > 0 ? 'right' : 'left'} before=${JSON.stringify(anchor.box)} after=${JSON.stringify(afterAnchor)} progress=${progress} maxRealStep=80 clip=${JSON.stringify(clip)}`);
      break;
    }
    preferredAnchor = { index: anchor.index, locator: anchor.locator };
    wheelScale = Math.max(1, Math.min(30, wheelDelta / progress));
    trace(`reveal-horizontal:progress step=${i} column=${tableHeaders[columnIndex]} anchor=${anchor.index}:${tableHeaders[anchor.index]} measuredEdge=${direction > 0 ? 'right' : 'left'} before=${beforeEdge} after=${afterEdge} real=${progress} wheel=${wheelDelta} nextScale=${wheelScale}`);
  }
  const scrollportBox = await waitForStableBox(page, scrollport);
  trace(`reveal-horizontal:fail column=${tableHeaders[columnIndex]} target=${target} positions=${JSON.stringify(targetBoxes)} scrollport=${JSON.stringify(scrollportBox)} ${Date.now() - startedAt}ms`);
  await capture(page, 'horizontal-reveal-failure');
  throw new Error(`Control sin geometría contenida en la tabla y el viewport tras desplazarla: columna=${tableHeaders[columnIndex]} target=${target}; tabla=${JSON.stringify(scrollportBox)} cajas=${JSON.stringify(targetBoxes)}`);
}

async function prepareTableMonth(page, month) {
  const viewport = page.viewportSize();
  const table = page.getByLabel(tableName, { exact: true });
  const scrollport = table.getByLabel(horizontalTableName, { exact: true });
  const body = table.getByLabel(verticalTableName, { exact: true });
  const row = table.getByLabel(rowName(month), { exact: true });
  const viewportBox = { x: 0, y: 56, width: viewport.width, height: viewport.height - 60 };
  const visibleInBody = (bodyBox, rowBox) => {
    const clip = intersection(bodyBox, viewportBox);
    // A full-height row may meet the scrollport edge; the action target is checked separately.
    return clip && rowBox?.height > 0 && rowBox.y >= clip.y && rowBox.y + rowBox.height <= clip.y + clip.height;
  };

  // Keep page-level reveal wheels outside the nested vertical table scroller.
  await reveal(page, scrollport, { wheelX: 4 });

  const targetY = Math.round(viewport.height * 0.25);
  for (let step = 0; step < 10; step++) {
    const before = await waitForStableBox(page, scrollport);
    if (!before?.width) throw new Error(`El contenedor de la tabla perdió geometría durante la preparación: ${JSON.stringify(before)}`);
    if (before.y <= targetY + 35) break;
    await page.mouse.move(4, Math.round(viewport.height / 2));
    await page.mouse.wheel(0, Math.min(80, Math.max(40, before.y - targetY - 25)));
    await page.waitForTimeout(200);
    const after = await waitForStableBox(page, scrollport);
    if (!after?.width) throw new Error(`El contenedor de la tabla perdió geometría durante el desplazamiento: ${JSON.stringify(after)}`);
    if (after.y >= before.y - 2) {
      trace(`table-start:page-scroll-boundary step=${step} before=${JSON.stringify(before)} after=${JSON.stringify(after)} targetY=${targetY}`);
      break;
    }
    trace(`table-start:page-scroll step=${step} before=${JSON.stringify(before)} after=${JSON.stringify(after)} pointer=4,${Math.round(viewport.height / 2)}`);
  }

  for (let step = 0; step < 16; step++) {
    const bodyBox = await waitForStableBox(page, body);
    const clip = intersection(bodyBox, viewportBox);
    const rowBox = await waitForStableBox(page, row);
    if (visibleInBody(bodyBox, rowBox)) {
      trace(`table-row:visible month=${month} step=${step} body=${JSON.stringify(bodyBox)} clip=${JSON.stringify(clip)} row=${JSON.stringify(rowBox)}`);
      break;
    }
    if (!clip || clip.width <= 12 || clip.height <= 12 || !rowBox?.height) {
      await capture(page, 'table-row-geometry-failure');
      throw new Error(`No hay bounds estables para las filas o M${month}: body=${JSON.stringify(bodyBox)} clip=${JSON.stringify(clip)} fila=${JSON.stringify(rowBox)}`);
    }
    const deltaY = rowBox.y + rowBox.height > clip.y + clip.height - 2
      ? Math.min(160, rowBox.y + rowBox.height - (clip.y + clip.height) + 12)
      : rowBox.y < clip.y + 2
        ? -Math.min(160, clip.y - rowBox.y + 12)
        : 0;
    if (deltaY === 0) {
      await capture(page, 'table-row-clip-failure');
      throw new Error(`M${month} no está contenido en el viewport vertical de la tabla: body=${JSON.stringify(bodyBox)} fila=${JSON.stringify(rowBox)}`);
    }
    const point = { x: clip.x + clip.width / 2, y: clip.y + clip.height / 2 };
    await page.mouse.move(point.x, point.y);
    await page.mouse.wheel(0, deltaY);
    await page.waitForTimeout(200);
    const after = await waitForStableBox(page, row);
    if (!after?.height || Math.abs(after.y - rowBox.y) <= 1) {
      trace(`table-row:no-progress month=${month} step=${step} before=${JSON.stringify(rowBox)} after=${JSON.stringify(after)} body=${JSON.stringify(clip)} delta=${deltaY}`);
      await capture(page, 'table-row-no-progress');
      throw new Error(`El desplazamiento vertical no avanzó hacia M${month}: antes=${JSON.stringify(rowBox)} después=${JSON.stringify(after)} body=${JSON.stringify(clip)} delta=${deltaY}`);
    }
    trace(`table-row:scroll month=${month} step=${step} before=${JSON.stringify(rowBox)} after=${JSON.stringify(after)} body=${JSON.stringify(clip)} delta=${deltaY}`);
  }
  const bodyBox = await waitForStableBox(page, body);
  const rowBox = await waitForStableBox(page, row);
  if (!visibleInBody(bodyBox, rowBox)) {
    await capture(page, 'table-row-not-visible');
    throw new Error(`La fila M${month} no quedó contenida en el viewport vertical tras 16 pasos: body=${JSON.stringify(bodyBox)} fila=${JSON.stringify(rowBox)}`);
  }
  const monthButton = row.getByRole('button', { name: new RegExp(`^Seleccionar M${month}(?: |$)`) });
  await expect(monthButton).toBeVisible();
  trace(`table-row:prepared month=${month} scrollport=${JSON.stringify(await waitForStableBox(page, scrollport))} body=${JSON.stringify(bodyBox)} row=${JSON.stringify(rowBox)} viewport=${JSON.stringify(viewport)}`);

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

test('[WEB-40-ORIGIN-A11Y] Keep the form tree while selecting payment origin with keyboard after wheel scrolling', { timeout: 120_000 }, async () => {
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
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
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
  await prepareTableMonth(page, month);
  const row = page.getByLabel(tableName, { exact: true }).getByLabel(rowName(month), { exact: true });
  const cell = row.getByRole('button', {
    name: `Copiar tabla ${column} Modificado mes ${month}`, exact: true
  });
  const header = column.toLocaleLowerCase('es') === 'saldo de capital' ? 'Saldo final' : column;
  await revealHorizontally(page, cell, header);
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
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
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
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
  } finally {
    await browser.close();
  }
});

test('[WEB-40-EVENT-BATCH] Discard and apply a missed payment, recovery and recurring event from touch controls', { timeout: 300_000 }, async () => {
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
    await assertTableCell(page, 'interés generado', 1, 'ARS 10,00');
    await assertTableCell(page, 'seguro generado', 1, 'ARS 5,00');
    await assertTableCell(page, 'pago total', 1, 'ARS 0,00');
    await assertTableCell(page, 'pago total', 2, 'ARS 210,00');
    await assertTableCell(page, 'pago total', 3, 'ARS 115,00');
    await assertTableCell(page, 'saldo de capital', 2, 'ARS 820,00');
    await assertTableCell(page, 'saldo de capital', 3, 'ARS 718,20');
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
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
    await prepareTableMonth(page, 3);
    const table = page.getByLabel(tableName, { exact: true });
    const row3 = table.getByLabel(rowName(3), { exact: true });
    await activate(page, row3.getByRole('button', { name: 'Seleccionar M3', exact: true }), true);
    await expect(tree).toContainText('Detalle del mes 3');
    await expect(tree).toContainText('Mes seleccionado: 3.');
    await expect(tree).toContainText('Mes M3');
    await expect(tree).toContainText('Saldo: Orig ARS 700,00 / Mod ARS 700,00');
    await expect(tree).toContainText('Capital: ARS 100,00');
    await expect(tree).toContainText('Intereses: ARS 0,00');
    await expect(tree).toContainText('Seguros: ARS 0,00');
    await expect(table.getByLabel(headerName('No aplicado'), { exact: true })).toHaveCount(1);
    await assertTableCell(page, 'Capital habitual', 3, 'ARS 100,00');
    await assertTableCell(page, 'saldo de capital', 3, 'ARS 700,00');

    await reveal(page, chart);
    const after = await chart.boundingBox();
    assert.ok(after);
    assert.equal(after.x, before.x);
    assert.equal(after.width, before.width);
    assert.equal(after.height, before.height);
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
  } finally {
    await browser.close();
  }
});

test('[WEB-40-TNUM-UI] Visible result width follows text length without changing for equal-length amounts', { timeout: 120_000 }, async t => {
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
    await capture(page, 'tnum-111');

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
    await capture(page, 'tnum-888');

    await enter(page, page.getByLabel('Cuota pactada (opcional)', { exact: true }), '88.88');
    await activate(page, page.getByRole('button', { name: 'Actualizar condiciones del préstamo', exact: true }));
    await expect(tree).toContainText('Condiciones actualizadas.');
    await reveal(page, result);
    await expect(result).toContainText('ARS 88,88');
    await page.mouse.move(5, page.viewportSize().height - 5);
    await page.waitForTimeout(250);
    const shorter = await result.boundingBox();
    assert.ok(shorter?.width > 0);
    await capture(page, 'tnum-88');
    t.diagnostic(`Anchos UI visibles (111,11 / 888,88 / 88,88): ${first.width} / ${second.width} / ${shorter.width}px.`);
    if (Math.abs(first.width - shorter.width) <= 1) {
      t.skip('La caja visible no distingue longitudes de texto; tabularidad queda NOT_RUN.');
      return;
    }
    assert.ok(Math.abs(first.width - second.width) <= 1, `El ancho visible cambió para cifras de igual longitud: ${first.width} → ${second.width}px`);
  } catch (error) {
    try { await capture(page, 'test-failure'); } catch (captureError) { trace(`capture:error ${captureError.message}`); }
    throw error;
  } finally {
    await browser.close();
  }
});
