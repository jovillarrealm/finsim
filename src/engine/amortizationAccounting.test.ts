import { describe, expect, it } from 'vitest';
import { D } from '../domain/scenario';
import {
  AmortizationAccount,
  ArgentinaPrepaymentAdapter,
  StandardPrepaymentAdapter,
} from './amortizationAccounting';

describe('AmortizationAccount (deep in-process accounting module)', () => {
  it('aplica orden de prelación en cascada: seguro -> interés -> capital', () => {
    const account = AmortizationAccount.open({
      principal: new D('1000.00'),
      monthlyRate: new D('0.01'),
      basePayment: new D('100.00'),
      termMonths: 12,
      isPaymentExplicit: true,
    });

    // Mes 1: omitido -> acumula 5.00 seguro, 10.00 interés, 90.00 vencido
    const m1 = account.processMonth({
      month: 1,
      isMissed: true,
      activeInsurance: [{ id: 'ins', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }],
    });

    expect(m1.arrears).toBe('105.00');
    expect(m1.debt).toBe('1015.00');
    expect(m1.pendingInsurance).toBe('5.00');
    expect(m1.pendingInterest).toBe('10.00');
    expect(m1.overduePrincipal).toBe('90.00');

    // Mes 2: pago parcial de 7.00 ofrecido como puesta al día
    // Debe aplicarse: 5.00 seguro pendiente mes 1 + 2.00 seguro corriente mes 2.
    // Intereses y capital no reciben nada.
    const m2 = account.processMonth({
      month: 2,
      isMissed: true,
      catchupOffer: new D('7.00'),
      activeInsurance: [{ id: 'ins', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }],
    });

    expect(m2.catchup.applied).toBe('7.00');
    expect(m2.catchup.insurance).toBe('7.00');
    expect(m2.catchup.interest).toBe('0.00');
    expect(m2.catchup.principal).toBe('0.00');
    expect(m2.pendingInsurance).toBe('3.00'); // 10 total generado - 7 pagado
    expect(m2.pendingInterest).toBe('20.00'); // 10 mes 1 + 10 mes 2
    expect(m2.overduePrincipal).toBe('180.00'); // 90 mes 1 + 90 mes 2
  });

  it('recupera atrasos y reduce capital vencido antes de capital ordinario', () => {
    const account = AmortizationAccount.open({
      principal: new D('1000.00'),
      monthlyRate: new D('0.01'),
      basePayment: new D('100.00'),
      termMonths: 12,
      isPaymentExplicit: true,
    });

    // Mes 1: omitido
    account.processMonth({
      month: 1,
      isMissed: true,
      activeInsurance: [{ id: 'ins', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }],
    });

    // Mes 2: pago regular 105.00 + recuperación 105.00 = 210.00
    const m2 = account.processMonth({
      month: 2,
      isMissed: false,
      catchupOffer: new D('105.00'),
      activeInsurance: [{ id: 'ins', name: 'Vida', kind: 'fixed', value: '5.00', endsAtPayoff: true }],
    });

    expect(m2.cashPaid).toBe('210.00');
    expect(m2.closingBalance).toBe('820.00');
    expect(m2.overduePrincipal).toBe('0.00');
    expect(m2.pendingInterest).toBe('0.00');
    expect(m2.pendingInsurance).toBe('0.00');
    expect(m2.arrears).toBe('0.00');
    expect(m2.debt).toBe('820.00');
  });

  it('evalúa reglas de precancelación regional (perfil Argentina) en la costura de prepago', () => {
    const argAdapter = new ArgentinaPrepaymentAdapter(12, '0.03', '0.21');
    const account = AmortizationAccount.open({
      principal: new D('10000.00'),
      monthlyRate: new D('0.01'),
      basePayment: new D('1000.00'),
      termMonths: 12,
      prepaymentRule: argAdapter,
    });

    // Umbral gratuito: (12 + 3) / 4 = mes 3.
    // Mes 1: antes del umbral -> aplica comisión 3% e IVA 21%
    const m1 = account.processMonth({
      month: 1,
      extraOffer: new D('1000.00'),
    });
    // Extra de 1000 aplicado a capital
    expect(m1.extra.principal).toBe('1000.00');
    expect(m1.commissionPaid).toBe('30.00'); // 3% de 1000.00
    expect(m1.vatPaid).toBe('6.30'); // 21% de 30.00

    // Mes 2: antes del umbral
    const m2 = account.processMonth({
      month: 2,
      extraOffer: new D('500.00'),
    });
    expect(m2.commissionPaid).toBe('15.00');
    expect(m2.vatPaid).toBe('3.15');

    // Mes 3: en el umbral gratuito (mes 3 >= 3) -> sin comisión
    const m3 = account.processMonth({
      month: 3,
      extraOffer: new D('2000.00'),
    });
    expect(m3.commissionPaid).toBe('0.00');
    expect(m3.vatPaid).toBe('0.00');
  });

  it('verifica invariantes de ordenamiento y secuencia temporal', () => {
    const account = AmortizationAccount.open({
      principal: new D('1000.00'),
      monthlyRate: new D('0.01'),
      basePayment: new D('100.00'),
      termMonths: 12,
    });

    account.processMonth({ month: 1 });
    // Salto de mes inválido (mes 3 en lugar de mes 2) debe lanzar error
    expect(() => account.processMonth({ month: 3 })).toThrow(/Invalid month sequence/);
  });

  it('conserva el capital y los cargos sin residuos ocultos', () => {
    const account = AmortizationAccount.open({
      principal: new D('300.00'),
      monthlyRate: new D('0'),
      termMonths: 3,
    });

    const m1 = account.processMonth({ month: 1 });
    const m2 = account.processMonth({ month: 2 });
    const m3 = account.processMonth({ month: 3 });

    expect(m1.scheduled.principal).toBe('100.00');
    expect(m2.scheduled.principal).toBe('100.00');
    expect(m3.scheduled.principal).toBe('100.00');
    expect(m3.closingBalance).toBe('0.00');
    expect(account.isPaidOff()).toBe(true);
  });
});
