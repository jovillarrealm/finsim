import type Decimal from 'decimal.js';
import { D, rateSchema, type Rate } from '../domain/scenario';

export function toMonthlyRate(rate: Rate): Decimal {
  const input = rateSchema.parse(rate);
  const value = new D(input.value);
  if (input.kind === 'monthly') return value;
  if (input.kind === 'annual') return value.plus(1).pow(new D(1).div(12)).minus(1);
  return value.div(input.frequency).plus(1).pow(new D(input.frequency).div(12)).minus(1);
}

// Values are fractions, never percentages; serialize without display rounding.
export function convertRate(rate: Rate, target: 'monthly' | 'annual' | 'nominal', frequency?: number): string {
  const output = rateSchema.parse(target === 'nominal'
    ? { kind: target, value: '0', frequency }
    : { kind: target, value: '0' });
  const monthly = toMonthlyRate(rate);
  if (output.kind === 'monthly') return monthly.toFixed();
  if (output.kind === 'annual') return monthly.plus(1).pow(12).minus(1).toFixed();
  return monthly.plus(1).pow(new D(12).div(output.frequency)).minus(1).times(output.frequency).toFixed();
}
