import { expect, test } from 'vitest';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { D, exampleScenario, type Scenario } from '../../domain/scenario';
import { simulateLoan } from '../../engine/loan';
import ScenarioComparison, { chartMoney, chartRows, signedMoney } from './ScenarioComparison';
import { readEventDrafts, type EventDraft } from './EventEditor';

const base: Scenario = { ...exampleScenario, principal: '1000.00', termMonths: 12, payment: '100.00',
  rate: { kind: 'monthly', value: '0.01' },
  insurance: [{ id: 'life', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }], events: [] };

test('stacks conserve cash, include catch-up allocation and extend a common time axis', () => {
  const original = simulateLoan(base);
  const modified = simulateLoan({ ...base, events: [
    { id: 'miss', kind: 'missed', month: 1 }, { id: 'recover', kind: 'catchup', month: 2, amount: '105.00' },
    { id: 'extra', kind: 'extra', month: 2, amount: '20.00' },
  ] });
  const months = Math.max(original.rows.length, modified.rows.length) + 1;
  const rows = chartRows(modified, months);
  expect(rows[0].exact.cash).toBe('0.00');
  expect(rows[1].exact).toEqual({ scheduled: '75.00', extra: '125.00', interest: '20.00', insurance: '10.00', cash: '230.00' });
  for (const row of rows) {
    expect(new D(row.exact.scheduled).plus(row.exact.extra).plus(row.exact.interest).plus(row.exact.insurance).toFixed(2)).toBe(row.exact.cash);
  }
  expect(rows.at(-1)?.cash).toBe(0);
  expect(chartRows(original, months)).toHaveLength(rows.length);
  expect(chartRows(simulateLoan({ ...base, events: [] }), months)).toEqual(chartRows(original, months));
});

test('chart labels retain cents that Number cannot represent and differences retain signs', () => {
  const result = simulateLoan({ ...base, principal: '999999999999999.99', payment: '999999999999999.99',
    rate: { kind: 'monthly', value: '0' }, insurance: [] });
  const row = chartRows(result, 1)[0];
  expect(row.scheduled).toBe(1000000000000000);
  expect(chartMoney(row, 'scheduled')).toBe('COP 999.999.999.999.999,99');
  expect(signedMoney('20.50')).toBe('+COP 20,50');
  expect(signedMoney('-20.50')).toBe('−COP 20,50');
  expect(signedMoney('0.00')).toBe('COP 0,00');
});

test('charts include insurance after payoff and incomplete comparison stays provisional', () => {
  const result = simulateLoan({ ...base, principal: '100', payment: '100', rate: { kind: 'monthly', value: '0' },
    insurance: [{ id: 'life', name: 'Vida', kind: 'fixed', value: '5', endsAtPayoff: false, endMonth: 3 }] });
  expect(chartRows(result, 3)[2].exact).toEqual({ scheduled: '0.00', extra: '0.00', interest: '0.00', insurance: '5.00', cash: '5.00' });
  const markup = renderToStaticMarkup(createElement(ScenarioComparison, { scenario: base, original: result,
    modified: { ...result, complete: false, payoffMonth: null, payoffDate: null }, onChange: () => true }));
  expect(markup).toContain('no costos finales ni ahorros definitivos');
  expect(markup).toContain('Sin liquidación completa');
  expect(markup).toContain('(parcial)');
});


test('valida todos los eventos del lote sin modificar el escenario aplicado', () => {
  const draft: EventDraft = { id: 'e', kind: 'extra', month: '2', amount: '1.234,56', endMonth: '5', every: '2' };
  for (const kind of ['extra', 'recurring', 'missed', 'catchup'] as const) {
    const result = readEventDrafts([{ ...draft, kind }], base);
    expect(result.success).toBe(true);
    if (!result.success) throw result.error;
    expect(result.data.events[0]).toMatchObject({ id: 'e', kind, month: 2 });
    if (kind !== 'missed') expect(result.data.events[0]).toHaveProperty('amount', '1234.56');
  }
  for (const patch of [{ amount: '1.23' }, { amount: '0' }, { amount: '-1' }, { amount: '1,001' }, { amount: 'Infinity' },
    { month: '0' }, { month: '1201' }, { month: '1.5' }, { kind: 'recurring' as const, month: '6', endMonth: '5' }]) {
    expect(readEventDrafts([draft, { ...draft, id: 'invalid', ...patch }], base).success).toBe(false);
  }
  expect(readEventDrafts([], base).success).toBe(true);
  expect(base.events).toEqual([]);
});

