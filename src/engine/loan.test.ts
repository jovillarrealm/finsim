import { describe, expect, it } from 'vitest';
import { D, money, type Scenario } from '../domain/scenario';
import { simulateLoan } from './loan';

const scenario = (changes: Partial<Scenario> = {}): Scenario => ({ version: 1, name: 'Prueba', source: 'original',
  principal: '1000.00', startDate: '2026-01', termMonths: 12, payment: '100.00',
  rate: { kind: 'monthly', value: '0.01' }, insurance: [], events: [], ...changes });
const fixed = { id: 'fixed', name: 'Seguro', kind: 'fixed' as const, value: '5.00', endsAtPayoff: true };

describe('referencias financieras literales', () => {
  it('redondea mitad de centavo hacia arriba', () => {
    expect(simulateLoan(scenario({ principal: '100.50' })).rows[0].interestCharged).toBe('1.01');
  });
  it('absorbe residuo de cuota calculada sin crear otro mes', () => {
    const result = simulateLoan(scenario({ principal: '100', rate: { kind: 'monthly', value: '0' }, termMonths: 3, payment: undefined }));
    expect(result.rows.map(row => row.cashPaid)).toEqual(['33.33', '33.33', '33.34']);
    expect(result.totals.principal).toBe('100.00');
    expect(result.remainingDebt).toBe('0.00');
  });
  it('calcula cuota con interés', () => {
    const result = simulateLoan(scenario({ payment: undefined, termMonths: 2 }));
    expect(result.basePayment).toBe('507.51');
    expect(result.rows[0]).toMatchObject({ interestCharged: '10.00', closingBalance: '502.49', scheduled: { principal: '497.51' } });
    expect(result.rows[1]).toMatchObject({ interestCharged: '5.02', cashPaid: '507.51', closingBalance: '0.00' });
  });
  it('seguro usa saldo inicial y extra no anticipa obligaciones', () => {
    const result = simulateLoan(scenario({ rate: { kind: 'monthly', value: '0' },
      insurance: [{ ...fixed, kind: 'percentage', value: '0.001' }], events: [{ id: 'extra', kind: 'extra', month: 1, amount: '1000' }] }));
    expect(result.rows).toHaveLength(1);
    expect(result.rows[0]).toMatchObject({ insuranceCharged: '1.00', scheduled: { applied: '101.00' }, extra: { applied: '900.00', unapplied: '100.00' }, cashPaid: '1001.00' });
  });
  it('continúa seguro fijo hasta mes final inclusivo', () => {
    const result = simulateLoan(scenario({ principal: '100', rate: { kind: 'monthly', value: '0' },
      insurance: [{ ...fixed, value: '10', endsAtPayoff: false, endMonth: 3 }] }));
    expect(result.payoffMonth).toBe(1);
    expect(result.lastInsuranceMonth).toBe(3);
    expect(result.rows.map(row => row.cashPaid)).toEqual(['110.00', '10.00', '10.00']);
    expect(result.totals.cashPaid).toBe('130.00');
  });
  it('redondea cada seguro antes de sumar', () => {
    expect(simulateLoan(scenario({ principal: '100.50', insurance: [
      { ...fixed, kind: 'percentage', value: '0.01' }, { ...fixed, id: 'second', kind: 'percentage', value: '0.01' },
    ] })).rows[0].insuranceCharged).toBe('2.02');
  });
});

describe('eventos y límites', () => {
  it.each([
    ['105', false, '210.00', '820.00', '0.00', '0.00', '0.00', '820.00'],
    [null, false, '105.00', '925.00', '105.00', '0.00', '0.00', '925.00'],
    ['20', false, '125.00', '905.00', '85.00', '0.00', '0.00', '905.00'],
    ['7', true, '7.00', '1000.00', '180.00', '20.00', '3.00', '1023.00'],
  ])('omisión recuperación %s; segunda omisión %s', (amount, missed, cashPaid, closingBalance, overduePrincipal, pendingInterest, pendingInsurance, debt) => {
    const result = simulateLoan(scenario({ insurance: [fixed], events: [
      { id: 'miss1', kind: 'missed', month: 1 },
      ...(amount ? [{ id: 'recovery', kind: 'catchup' as const, month: 2, amount }] : []),
      ...(missed ? [{ id: 'miss2', kind: 'missed' as const, month: 2 }] : []),
    ] }));
    expect(result.rows[0]).toMatchObject({ arrears: '105.00', debt: '1015.00', overduePrincipal: '90.00' });
    expect(result.rows[1]).toMatchObject({ cashPaid, closingBalance, overduePrincipal, pendingInterest, pendingInsurance, debt, interestCharged: '10.00' });
    if (amount === '105') expect(result.rows[1].catchup).toMatchObject({ principal: '105.00', insurance: '0.00', interest: '0.00' });
  });
  it('permite extra en mes omitido y conserva eventos posteriores como no aplicados', () => {
    const result = simulateLoan(scenario({ insurance: [{ ...fixed, endsAtPayoff: false, endMonth: 3 }], events: [
      { id: 'miss', kind: 'missed', month: 1 }, { id: 'extra', kind: 'extra', month: 1, amount: '2000' },
      { id: 'repeat', kind: 'recurring', month: 2, endMonth: 4, every: 2, amount: '10' },
      { id: 'lateMiss', kind: 'missed', month: 3 }, { id: 'lateRecovery', kind: 'catchup', month: 5, amount: '7' },
    ] }));
    expect(result.rows[0].extra).toMatchObject({ principal: '1000.00', insurance: '5.00', interest: '10.00', unapplied: '985.00' });
    expect(result.rows.map(row => row.cashPaid)).toEqual(['1015.00', '5.00', '5.00']);
    expect(result.unappliedEvents.map(event => [event.eventId, event.month, event.amount])).toEqual([
      ['repeat', 2, '10.00'], ['lateMiss', 3, '0.00'], ['repeat', 4, '10.00'], ['lateRecovery', 5, '7.00'],
    ]);
    expect(result.totals.unapplied).toBe('1012.00');
  });
  it('no fuerza cuota conocida al plazo y conserva deuda al límite', () => {
    const known = simulateLoan(scenario({ principal: '100', termMonths: 3, payment: '33.33', rate: { kind: 'monthly', value: '0' } }));
    expect(known.rows.map(row => row.cashPaid)).toEqual(['33.33', '33.33', '33.33', '0.01']);
    const result = simulateLoan(scenario({ payment: '1' }));
    expect(result.complete).toBe(false);
    expect(result.rows).toHaveLength(1200);
    expect(result.payoffMonth).toBeNull();
    expect(result.remainingDebt).toBe('11800.00');
  });
  it('redondeo a plazo largo y atrasos reales no inventan recuperación final', () => {
    const input = scenario({ principal: '100000', termMonths: 360, payment: undefined, rate: { kind: 'monthly', value: '0.005' } });
    expect(simulateLoan(input).payoffMonth).toBe(360);
    const late = simulateLoan({ ...input, events: [{ id: 'miss', kind: 'missed', month: 359 }] });
    expect(late.payoffMonth).toBeGreaterThan(360);
    expect(late.rows[359].scheduled.offered).toBe(late.basePayment);
    expect(simulateLoan(input)).toEqual(simulateLoan({ ...input, source: 'current', events: [] }));
  });
  it('seguros porcentuales posteriores son cero y mes final limita una extensión', () => {
    const result = simulateLoan(scenario({ principal: '100', payment: '100', insurance: [
      { ...fixed, kind: 'percentage', value: '0.01', endsAtPayoff: false, endMonth: 4 },
    ] }));
    expect(result.rows[2].insuranceCharged).toBe('0.00');
    expect(result.lastInsuranceMonth).toBe(4);
    const extended = simulateLoan(scenario({ insurance: [{ ...fixed, endsAtPayoff: false, endMonth: 2 }] }));
    expect(extended.rows[2].insuranceCharged).toBe('0.00');
  });
  it('conserva capital, cargos y caja en cada fila sin saldos negativos', () => {
    const result = simulateLoan(scenario({ insurance: [fixed], events: [
      { id: 'miss', kind: 'missed', month: 1 }, { id: 'miss2', kind: 'missed', month: 2 },
      { id: 'rec', kind: 'recurring', month: 2, endMonth: 24, every: 3, amount: '110' },
    ] }));
    let pendingInterest = new D(0), pendingInsurance = new D(0);
    for (const row of result.rows) {
      const payments = [row.scheduled, row.catchup, row.extra];
      const sum = (key: 'principal' | 'interest' | 'insurance' | 'applied') => payments.reduce((total, item) => total.plus(item[key]), new D(0));
      expect(money(sum('principal').plus(row.closingBalance))).toBe(row.openingBalance);
      expect(money(sum('interest').plus(row.pendingInterest))).toBe(money(pendingInterest.plus(row.interestCharged)));
      expect(money(sum('insurance').plus(row.pendingInsurance))).toBe(money(pendingInsurance.plus(row.insuranceCharged)));
      expect(money(sum('principal').plus(sum('interest')).plus(sum('insurance')))).toBe(row.cashPaid);
      expect(new D(row.overduePrincipal).lte(row.closingBalance)).toBe(true);
      for (const value of [row.closingBalance, row.pendingInterest, row.pendingInsurance, row.overduePrincipal]) expect(new D(value).gte(0)).toBe(true);
      pendingInterest = new D(row.pendingInterest); pendingInsurance = new D(row.pendingInsurance);
    }
    expect(result.totals.principal).toBe('1000.00');
    expect(result.complete).toBe(true);
  });
  it('valida entrada antes de calcular', () => {
    expect(() => simulateLoan(scenario({ principal: '-1' }))).toThrow();
  });
});
