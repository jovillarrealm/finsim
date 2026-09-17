import { expect, test } from 'vitest';
import { formatMoney, formatMonth } from './format';

test('formatea COP sin perder centavos y meses sin desbordamiento', () => {
  expect(formatMoney('1000000000000000.01')).toBe('$ 1.000.000.000.000.000,01');
  expect(formatMoney('-0.01')).toBe('−$ 0,01');
  expect(formatMonth('2026-01')).toContain('2026');
});
