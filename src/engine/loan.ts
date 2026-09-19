import type Decimal from 'decimal.js';
import { D, MAX_MONTHS, money, scenarioSchema, type Allocation, type LoanResult, type Scenario } from '../domain/scenario';
import { toMonthlyRate } from './rates';

export function simulateLoan(scenario: Scenario): LoanResult {
  const input = scenarioSchema.parse(scenario);
  const rate = toMonthlyRate(input.rate);
  let balance = new D(input.principal);
  // Sum discounted periods: equivalent annuity formula without subtracting nearly equal numbers.
  let discount = new D(1), annuity = new D(0);
  if (!input.payment) for (let period = 0; period < input.termMonths; period++) {
    discount = discount.div(rate.plus(1));
    annuity = annuity.plus(discount);
  }
  const base = new D(input.payment ?? money(balance.div(annuity)));
  let referenceBalance = balance;
  let interest = new D(0), insurance = new D(0), overdue = new D(0);
  const result: LoanResult = {
    rows: [], basePayment: money(base), payoffMonth: null, payoffDate: null,
    lastInsuranceMonth: null, complete: false, remainingDebt: '0.00',
    totals: { principal: '0.00', interest: '0.00', insurance: '0.00', cashPaid: '0.00', unapplied: '0.00' },
    unappliedEvents: [],
  };
  const debt = () => balance.plus(interest).plus(insurance);
  // Category balances are sufficient: oldest-first has no differing rate or penalty within a category.
  function allocate(offered: Decimal): Allocation {
    const paidInsurance = D.min(offered, insurance);
    const paidInterest = D.min(offered.minus(paidInsurance), interest);
    const paidPrincipal = D.min(offered.minus(paidInsurance).minus(paidInterest), balance);
    insurance = insurance.minus(paidInsurance);
    interest = interest.minus(paidInterest);
    balance = balance.minus(paidPrincipal);
    overdue = D.max(0, overdue.minus(paidPrincipal));
    const applied = paidInsurance.plus(paidInterest).plus(paidPrincipal);
    return { offered: money(offered), applied: money(applied), insurance: money(paidInsurance),
      interest: money(paidInterest), principal: money(paidPrincipal), unapplied: money(offered.minus(applied)) };
  }
  const [year, firstMonth] = input.startDate.split('-').map(Number);
  for (let month = 1; month <= MAX_MONTHS; month++) {
    const events = input.events.filter(event => event.kind === 'recurring'
      ? month >= event.month && month <= event.endMonth && (month - event.month) % event.every === 0
      : event.month === month);
    const paidOff = result.payoffMonth !== null;
    if (paidOff) for (const event of events) {
      const amount = event.kind === 'missed' ? '0.00' : money(event.amount);
      result.unappliedEvents.push({ eventId: event.id, month, amount, reason: 'Posterior a la liquidación del préstamo.' });
      result.totals.unapplied = money(new D(result.totals.unapplied).plus(amount));
    }
    const activeInsurance = input.insurance.filter(item => (!item.endsAtPayoff || !paidOff)
      && (item.endMonth === undefined || month <= item.endMonth));
    // Continue scanning events after payoff, without inventing empty schedule months.
    if (paidOff && activeInsurance.length === 0 && insurance.isZero()) continue;
    const opening = balance;
    const currentInterest = new D(money(opening.times(rate)));
    const currentInsurance = activeInsurance.reduce((sum, item) => sum.plus(money(item.kind === 'fixed'
      ? item.value : opening.times(item.value))), new D(0));
    if (activeInsurance.length > 0) result.lastInsuranceMonth = month;
    const wasCurrent = interest.isZero() && insurance.isZero() && overdue.isZero();
    interest = interest.plus(currentInterest);
    insurance = insurance.plus(currentInsurance);
    overdue = overdue.plus(D.min(D.max(0, base.minus(currentInterest)), balance.minus(overdue)));
    const referenceDue = referenceBalance.plus(money(referenceBalance.times(rate)));
    referenceBalance = D.max(0, referenceDue.minus(base));
    let scheduledOffer = paidOff ? currentInsurance : D.min(base.plus(currentInsurance), debt());
    // Only the independently rounded base schedule's residual can increase the last installment.
    if (!input.payment && month === input.termMonths && wasCurrent && !paidOff
      && debt().lte(D.max(base, referenceDue).plus(currentInsurance))) scheduledOffer = debt();
    const scheduled = allocate(!paidOff && events.some(event => event.kind === 'missed') ? new D(0) : scheduledOffer);
    const amountFor = (kind: 'catchup' | 'extra') => events.reduce((sum, event) =>
      !paidOff && (event.kind === kind || (kind === 'extra' && event.kind === 'recurring'))
        ? sum.plus('amount' in event ? event.amount : 0) : sum, new D(0));
    const catchup = allocate(amountFor('catchup'));
    const extra = allocate(amountFor('extra'));
    const payments = [scheduled, catchup, extra];
    const cashPaid = money(payments.reduce((sum, payment) => sum.plus(payment.applied), new D(0)));
    const dateIndex = year * 12 + firstMonth - 1 + month - 1;
    const date = `${String(Math.floor(dateIndex / 12)).padStart(4, '0')}-${String(dateIndex % 12 + 1).padStart(2, '0')}`;
    result.rows.push({ month, date, openingBalance: money(opening), interestCharged: money(currentInterest),
      insuranceCharged: money(currentInsurance), scheduled, catchup, extra, closingBalance: money(balance),
      overduePrincipal: money(overdue), pendingInterest: money(interest), pendingInsurance: money(insurance),
      arrears: money(overdue.plus(interest).plus(insurance)), debt: money(debt()), cashPaid });
    for (const key of ['principal', 'interest', 'insurance', 'unapplied'] as const)
      result.totals[key] = money(payments.reduce((sum, payment) => sum.plus(payment[key]), new D(result.totals[key])));
    result.totals.cashPaid = money(new D(result.totals.cashPaid).plus(cashPaid));
    if (result.payoffMonth === null && debt().isZero()) {
      result.payoffMonth = month;
      result.payoffDate = date;
    }
  }
  result.remainingDebt = money(debt());
  result.complete = result.payoffMonth !== null && debt().isZero();
  return result;
}
