import { expect, test } from 'vitest';
import { exampleScenario } from '../../domain/scenario';
import { parseSpanishNumber, readLoanForm } from './LoanForm';

test('lee importes colombianos exactos y porcentajes sin aceptar formatos ambiguos', () => {
  expect(parseSpanishNumber('20.000.000,01')).toBe('20000000.01');
  expect(parseSpanishNumber('1000000000000000,00')).toBe('1000000000000000.00');
  expect(parseSpanishNumber('0,05', true)).toBe('0.0005');
  expect(parseSpanishNumber('12,6825030131969720661201', true)).toBe('0.126825030131969720661201');
  for (const value of ['', 'Infinity', '-1', '1.23', '1,2,3', '1e3']) expect(parseSpanishNumber(value)).toBe('');
});

test('valida el formulario completo y conserva eventos sin reemplazar entradas inválidas', () => {
  const scenario = { ...exampleScenario, events: [{ id: 'e', kind: 'extra' as const, month: 1, amount: '100' }] };
  const data = new FormData();
  Object.entries({ name: 'Mi préstamo', source: 'current', principal: '1.000,00', startDate: '2026-10',
    termMonths: '12', payment: '', 'rate.kind': 'nominal', 'rate.value': '12', 'rate.frequency': '12',
    'insurance.0.name': 'Vida', 'insurance.0.value': '0,05', 'insurance.0.endMonth': '15',
  }).forEach(([key, value]) => data.set(key, value));
  const insurance = [{ ...scenario.insurance[0], endsAtPayoff: false }];
  const result = readLoanForm(data, scenario, insurance);
  expect(result.success).toBe(true);
  if (!result.success) throw result.error;
  expect(result.data.principal).toBe('1000.00');
  expect(result.data.rate).toEqual({ kind: 'nominal', value: '0.12', frequency: 12 });
  expect(result.data.insurance[0]).toMatchObject({ value: '0.0005', endMonth: 15, endsAtPayoff: false });
  expect(result.data.events).toEqual(scenario.events);
  data.set('principal', '1,001');
  expect(readLoanForm(data, scenario, insurance).success).toBe(false);
  data.set('principal', '1.000'); data.set('termMonths', '1201');
  expect(readLoanForm(data, scenario, insurance).success).toBe(false);
  expect(scenario.principal).toBe('20000000.00');
});
