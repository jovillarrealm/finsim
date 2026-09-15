import Decimal from 'decimal.js';
import { z } from 'zod';

export const D = Decimal.clone({ precision: 40, rounding: Decimal.ROUND_HALF_UP });
export const MAX_MONTHS = 1200;
export const money = (value: Decimal.Value): string => new D(value).toFixed(2);

// Decimal strings use a dot and rates are fractions: 0.01 means 1%.
export const decimalSchema = z.string().regex(/^\d+(\.\d+)?$/, 'Usa un número no negativo con punto decimal.')
  .refine(value => { try { return new D(value).isFinite(); } catch { return false; } }, 'El número debe ser finito.');
export const moneySchema = decimalSchema.refine(value => !value.includes('.') || value.split('.')[1].length <= 2,
  'Usa como máximo dos decimales.');
const positiveMoney = moneySchema.refine(value => { try { return new D(value).gt(0); } catch { return false; } }, 'El importe debe ser mayor que cero.');
export const monthSchema = z.number().int().min(1).max(MAX_MONTHS);
export const rateSchema = z.discriminatedUnion('kind', [
  z.object({ kind: z.literal('monthly'), value: decimalSchema }).strict(),
  z.object({ kind: z.literal('annual'), value: decimalSchema }).strict(),
  z.object({ kind: z.literal('nominal'), value: decimalSchema, frequency: z.number().int().positive() }).strict(),
]);
export type Rate = z.infer<typeof rateSchema>;

const insuranceSchema = z.object({
  id: z.string().min(1), name: z.string().min(1),
  kind: z.enum(['fixed', 'percentage']), value: decimalSchema,
  endsAtPayoff: z.boolean(), endMonth: monthSchema.optional(),
}).strict().superRefine((insurance, ctx) => {
  if (!insurance.endsAtPayoff && insurance.endMonth === undefined)
    ctx.addIssue({ code: 'custom', path: ['endMonth'], message: 'Indica el último mes del seguro.' });
  if (insurance.kind === 'fixed' && !moneySchema.safeParse(insurance.value).success)
    ctx.addIssue({ code: 'custom', path: ['value'], message: 'Usa como máximo dos decimales.' });
});
export type Insurance = z.infer<typeof insuranceSchema>;

export const eventSchema = z.discriminatedUnion('kind', [
  z.object({ id: z.string().min(1), kind: z.literal('extra'), month: monthSchema, amount: positiveMoney }).strict(),
  z.object({ id: z.string().min(1), kind: z.literal('recurring'), month: monthSchema, endMonth: monthSchema,
    every: z.number().int().min(1).max(MAX_MONTHS), amount: positiveMoney }).strict(),
  z.object({ id: z.string().min(1), kind: z.literal('missed'), month: monthSchema }).strict(),
  z.object({ id: z.string().min(1), kind: z.literal('catchup'), month: monthSchema, amount: positiveMoney }).strict(),
]).superRefine((event, ctx) => {
  if (event.kind === 'recurring' && event.endMonth < event.month)
    ctx.addIssue({ code: 'custom', path: ['endMonth'], message: 'El mes final debe ser igual o posterior al inicial.' });
});
export type LoanEvent = z.infer<typeof eventSchema>;

export const scenarioSchema = z.object({
  version: z.literal(1), name: z.string().min(1).max(120),
  source: z.enum(['original', 'current']),
  principal: positiveMoney,
  // The first payment month is YYYY-MM; month indexes are one-based.
  startDate: z.string().regex(/^\d{4}-(0[1-9]|1[0-2])$/, 'Usa año-mes válido.'),
  termMonths: monthSchema, rate: rateSchema,
  payment: positiveMoney.optional(),
  insurance: z.array(insuranceSchema).max(30),
  events: z.array(eventSchema).max(2400),
}).strict().superRefine((scenario, ctx) => {
  for (const key of ['insurance', 'events'] as const) {
    if (new Set(scenario[key].map(item => item.id)).size !== scenario[key].length)
      ctx.addIssue({ code: 'custom', path: [key], message: 'Los identificadores no deben repetirse.' });
  }
});
export type Scenario = z.infer<typeof scenarioSchema>;

export interface Allocation {
  offered: string;
  applied: string;
  insurance: string;
  interest: string;
  principal: string;
  unapplied: string;
}

export interface MonthRow {
  month: number;
  date: string;
  openingBalance: string;
  interestCharged: string;
  insuranceCharged: string;
  scheduled: Allocation;
  catchup: Allocation;
  extra: Allocation;
  closingBalance: string;
  overduePrincipal: string;
  pendingInterest: string;
  pendingInsurance: string;
  arrears: string;
  debt: string;
  cashPaid: string;
}

export interface LoanResult {
  rows: MonthRow[];
  basePayment: string;
  payoffMonth: number | null;
  payoffDate: string | null;
  lastInsuranceMonth: number | null;
  complete: boolean;
  totals: { principal: string; interest: string; insurance: string; cashPaid: string; unapplied: string };
  remainingDebt: string;
  unappliedEvents: { eventId: string; month: number; amount: string; reason: string }[];
}

export const exampleScenario: Scenario = {
  version: 1, name: 'Ejemplo · Crédito de libre inversión', source: 'original',
  principal: '20000000.00', startDate: '2026-10', termMonths: 36,
  rate: { kind: 'annual', value: '0.18' },
  insurance: [{ id: 'life', name: 'Seguro de vida', kind: 'percentage', value: '0.0005', endsAtPayoff: true }],
  events: [],
};
