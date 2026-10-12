import Decimal from 'decimal.js';
import { D, MAX_MONTHS, money, scenarioSchema, type LoanResult, type Scenario } from '../domain/scenario';
import { AmortizationAccount, StandardPrepaymentAdapter } from './amortizationAccounting';
import { toMonthlyRate } from './rates';

export function simulateLoan(scenario: Scenario): LoanResult {
  const input = scenarioSchema.parse(scenario);
  const rate = toMonthlyRate(input.rate);
  const account = AmortizationAccount.open({
    principal: new D(input.principal),
    monthlyRate: rate,
    termMonths: input.termMonths,
    basePayment: input.payment ? new D(input.payment) : undefined,
    isPaymentExplicit: input.payment !== undefined,
    prepaymentRule: new StandardPrepaymentAdapter(),
  });

  const result: LoanResult = {
    rows: [],
    basePayment: money(account.basePayment),
    payoffMonth: null,
    payoffDate: null,
    lastInsuranceMonth: null,
    complete: false,
    remainingDebt: '0.00',
    totals: { principal: '0.00', interest: '0.00', insurance: '0.00', cashPaid: '0.00', unapplied: '0.00' },
    unappliedEvents: [],
  };

  const [year, firstMonth] = input.startDate.split('-').map(Number);

  for (let month = 1; month <= MAX_MONTHS; month++) {
    const events = input.events.filter(event => event.kind === 'recurring'
      ? month >= event.month && month <= event.endMonth && (month - event.month) % event.every === 0
      : event.month === month);

    const paidOff = result.payoffMonth !== null;
    if (paidOff) {
      for (const event of events) {
        const amount = event.kind === 'missed' ? '0.00' : money(event.amount);
        result.unappliedEvents.push({
          eventId: event.id,
          month,
          amount,
          reason: 'Posterior a la liquidación del préstamo.',
        });
        result.totals.unapplied = money(new D(result.totals.unapplied).plus(amount));
      }
    }

    const activeInsurance = input.insurance.filter(item => (!item.endsAtPayoff || !paidOff)
      && (item.endMonth === undefined || month <= item.endMonth));

    // Continue scanning events after payoff, without inventing empty schedule months.
    if (paidOff && activeInsurance.length === 0 && account.pendingInsurance.isZero()) {
      continue;
    }

    if (activeInsurance.length > 0) {
      result.lastInsuranceMonth = month;
    }

    const isMissed = !paidOff && events.some(event => event.kind === 'missed');
    const catchupOffer = events.reduce((sum, event) =>
      !paidOff && event.kind === 'catchup' ? sum.plus(event.amount) : sum, new D(0));
    const extraOffer = events.reduce((sum, event) =>
      !paidOff && (event.kind === 'extra' || event.kind === 'recurring') ? sum.plus(event.amount) : sum, new D(0));

    const settlement = account.processMonth({
      month,
      isMissed,
      catchupOffer,
      extraOffer,
      activeInsurance,
    });

    const dateIndex = year * 12 + firstMonth - 1 + month - 1;
    const date = `${String(Math.floor(dateIndex / 12)).padStart(4, '0')}-${String(dateIndex % 12 + 1).padStart(2, '0')}`;

    result.rows.push({
      month,
      date,
      openingBalance: settlement.openingBalance,
      interestCharged: settlement.interestCharged,
      insuranceCharged: settlement.insuranceCharged,
      scheduled: settlement.scheduled,
      catchup: settlement.catchup,
      extra: settlement.extra,
      closingBalance: settlement.closingBalance,
      overduePrincipal: settlement.overduePrincipal,
      pendingInterest: settlement.pendingInterest,
      pendingInsurance: settlement.pendingInsurance,
      arrears: settlement.arrears,
      debt: settlement.debt,
      cashPaid: settlement.cashPaid,
    });

    for (const key of ['principal', 'interest', 'insurance', 'unapplied'] as const) {
      result.totals[key] = money(
        new D(result.totals[key])
          .plus(settlement.scheduled[key])
          .plus(settlement.catchup[key])
          .plus(settlement.extra[key])
      );
    }
    result.totals.cashPaid = money(new D(result.totals.cashPaid).plus(settlement.cashPaid));

    if (result.payoffMonth === null && account.isPaidOff()) {
      result.payoffMonth = month;
      result.payoffDate = date;
    }
  }

  result.remainingDebt = money(account.totalDebt());
  result.complete = result.payoffMonth !== null && account.isPaidOff();
  return result;
}
