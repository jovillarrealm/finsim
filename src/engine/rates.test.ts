import { expect, test } from 'vitest';
import { D, type Rate } from '../domain/scenario';
import { convertRate, toMonthlyRate } from './rates';

test('coincide con referencias literales y conserva la precisión interna', () => {
  expect(convertRate({ kind: 'monthly', value: '0.01' }, 'annual')).toBe('0.126825030131969720661201');
  expect(toMonthlyRate({ kind: 'nominal', value: '0.12', frequency: 12 }).toFixed()).toBe('0.01');
  expect(toMonthlyRate({ kind: 'monthly', value: '0.012345678901234567890123456789' }).toFixed())
    .toBe('0.012345678901234567890123456789');
  expect(toMonthlyRate({ kind: 'annual', value: '0.126825030131969720661201' }).minus('0.01').abs().lte('1e-12')).toBe(true);
});

test('ida y vuelta EA, EM y nominal con distintas frecuencias y tasa cero', () => {
  for (const value of ['0', '0.01', '0.1823456789012345']) {
    const source: Rate = { kind: 'monthly', value };
    const annual = convertRate(source, 'annual');
    expect(toMonthlyRate({ kind: 'annual', value: annual }).minus(value).abs().lte('1e-12')).toBe(true);
    for (const frequency of [1, 2, 3, 4, 6, 12, 24, 52, 365]) {
      const nominal = convertRate(source, 'nominal', frequency);
      expect(toMonthlyRate({ kind: 'nominal', value: nominal, frequency }).minus(value).abs().lte('1e-12')).toBe(true);
      const original: Rate = { kind: 'nominal', value, frequency };
      expect(new D(convertRate({ kind: 'monthly', value: toMonthlyRate(original).toFixed() }, 'nominal', frequency))
        .minus(value).abs().lte('1e-12')).toBe(true);
      if (value === '0') expect(nominal).toBe('0');
    }
    if (value === '0') expect(annual).toBe('0');
  }
});

test('valida entradas y exige frecuencia nominal entera positiva', () => {
  for (const value of ['-0.01', 'NaN', 'Infinity', '1e3', '']) {
    expect(() => toMonthlyRate({ kind: 'monthly', value })).toThrow();
  }
  for (const frequency of [0, -1, 1.5, NaN, Infinity]) {
    expect(() => toMonthlyRate({ kind: 'nominal', value: '0.12', frequency })).toThrow();
    expect(() => convertRate({ kind: 'monthly', value: '0.01' }, 'nominal', frequency)).toThrow();
  }
  expect(() => convertRate({ kind: 'monthly', value: '0.01' }, 'nominal')).toThrow();
});
