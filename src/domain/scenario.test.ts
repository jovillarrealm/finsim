import { expect, test } from 'vitest';
import { D, exampleScenario, money, scenarioSchema } from './scenario';

test('valida contratos, límites y precisión monetaria', () => {
  expect(scenarioSchema.parse(exampleScenario)).toEqual(exampleScenario);
  expect(money('1.005')).toBe('1.01');
  expect(D.precision).toBe(40);
  for (const patch of [
    { principal: '1.001' }, { principal: '0' }, { principal: 'Infinity' },
    { termMonths: 1201 }, { termMonths: 0 }, { version: 2 },
    { rate: { kind: 'nominal', value: '0.12', frequency: 0 } },
    { events: [{ id: 'e', kind: 'extra', month: 0, amount: '1' }] },
    { events: [{ id: 'e', kind: 'extra', month: 1201, amount: '1' }] },
    { events: [{ id: 'e', kind: 'other', month: 1 }] },
    { insurance: [{ id: 'i', name: 'Seguro', kind: 'fixed', value: '1', endsAtPayoff: false }] },
  ]) expect(scenarioSchema.safeParse({ ...exampleScenario, ...patch }).success).toBe(false);
  expect(scenarioSchema.safeParse({ ...exampleScenario, rate: { kind: 'monthly', value: '0' }, termMonths: 1200 }).success).toBe(true);
});
