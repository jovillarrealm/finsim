import { expect, test } from 'vitest';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { D, exampleScenario, type Scenario } from '../../domain/scenario';
import { simulateLoan } from '../../engine/loan';
import ScenarioComparison, { chartMoney, chartRows, signedMoney } from './ScenarioComparison';
import { readEventForm } from './EventEditor';

const base: Scenario = { ...exampleScenario, principal: '1000.00', termMonths: 12, payment: '100.00',
  rate: { kind: 'monthly', value: '0.01' },
  insurance: [{ id: 'life', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }], events: [] };

const form = (values: Record<string, string>) => {
  const data = new FormData();
  Object.entries(values).forEach(([key, value]) => data.set(key, value));
  return data;
};

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
  expect(chartMoney(row, 'scheduled')).toBe('$ 999.999.999.999.999,99');
  expect(signedMoney('20.50')).toBe('+$ 20,50');
  expect(signedMoney('-20.50')).toBe('−$ 20,50');
  expect(signedMoney('0.00')).toBe('$ 0,00');
});

test('charts include insurance after payoff and incomplete comparison stays provisional', () => {
  const result = simulateLoan({ ...base, principal: '100', payment: '100', rate: { kind: 'monthly', value: '0' },
    insurance: [{ id: 'life', name: 'Vida', kind: 'fixed', value: '5', endsAtPayoff: false, endMonth: 3 }] });
  expect(chartRows(result, 3)[2].exact).toEqual({ scheduled: '0.00', extra: '0.00', interest: '0.00', insurance: '5.00', cash: '5.00' });
  const markup = renderToStaticMarkup(createElement(ScenarioComparison, { scenario: base, original: result,
    modified: { ...result, complete: false, payoffMonth: null, payoffDate: null }, onChange: () => {} }));
  expect(markup).toContain('no costos finales ni ahorros definitivos');
  expect(markup).toContain('Sin liquidación completa');
  expect(markup).toContain('(parcial)');
});

test('event form creates every kind, edits stable IDs and validates Spanish amounts and month limits', () => {
  for (const kind of ['extra', 'recurring', 'missed', 'catchup'] as const) {
    const data = form({ month: '2', amount: '1.234,56', endMonth: '5', every: '2' });
    const result = readEventForm(data, base, kind, 'event');
    expect(result.success).toBe(true);
    if (!result.success) throw result.error;
    expect(result.data.events[0]).toMatchObject({ id: 'event', kind, month: 2 });
    if (kind !== 'missed') expect(result.data.events[0]).toHaveProperty('amount', '1234.56');
    data.set('month', '3');
    const updated = readEventForm(data, result.data, kind, 'event', true);
    expect(updated.success).toBe(true);
    if (!updated.success) throw updated.error;
    expect(updated.data.events).toHaveLength(1);
    expect(updated.data.events[0].month).toBe(3);
  }
  for (const amount of ['1.23', '0', '-1', '1,001', 'Infinity']) {
    expect(readEventForm(form({ month: '2', amount }), base, 'extra', 'event').success).toBe(false);
  }
  for (const month of ['0', '1201', '1.5']) {
    expect(readEventForm(form({ month }), base, 'missed', 'event').success).toBe(false);
  }
  expect(readEventForm(form({ month: '5', amount: '10', endMonth: '4', every: '1' }), base, 'recurring', 'event').success).toBe(false);
  expect(base.events).toEqual([]);
});
