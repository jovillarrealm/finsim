import Decimal from 'decimal.js';
import { D, money, type Allocation, type Insurance } from '../domain/scenario';

export interface PrepaymentFee {
  readonly commission: Decimal;
  readonly vat: Decimal;
}

/**
 * Seam for regional prepayment policies (e.g. Argentina Prepayment Profile).
 */
export interface PrepaymentRule {
  evaluatePrepayment(month: number, prepaidCapital: Decimal): PrepaymentFee;
}

export class StandardPrepaymentAdapter implements PrepaymentRule {
  evaluatePrepayment(_month: number, _prepaidCapital: Decimal): PrepaymentFee {
    return { commission: new D(0), vat: new D(0) };
  }
}

export class ArgentinaPrepaymentAdapter implements PrepaymentRule {
  private readonly commissionRate: Decimal;
  private readonly vatRate: Decimal;

  constructor(
    private readonly originalTermMonths: number,
    commissionRate: string | Decimal = '0.03',
    vatRate: string | Decimal = '0.21'
  ) {
    this.commissionRate = new D(commissionRate);
    this.vatRate = new D(vatRate);
  }

  evaluatePrepayment(month: number, prepaidCapital: Decimal): PrepaymentFee {
    if (prepaidCapital.lte(0)) {
      return { commission: new D(0), vat: new D(0) };
    }
    const freeThresholdMonth = Math.floor((this.originalTermMonths + 3) / 4);
    if (month >= freeThresholdMonth) {
      return { commission: new D(0), vat: new D(0) };
    }
    const commission = new D(money(prepaidCapital.times(this.commissionRate)));
    const vat = new D(money(commission.times(this.vatRate)));
    return { commission, vat };
  }
}

export interface AmortizationTerms {
  readonly principal: Decimal;
  readonly monthlyRate: Decimal;
  readonly termMonths: number;
  readonly basePayment?: Decimal;
  readonly isPaymentExplicit?: boolean;
  readonly prepaymentRule?: PrepaymentRule;
}

export interface MonthPaymentInput {
  readonly month: number;
  readonly isMissed?: boolean;
  readonly catchupOffer?: Decimal;
  readonly extraOffer?: Decimal;
  readonly activeInsurance?: Insurance[];
}

export interface MonthSettlement {
  readonly month: number;
  readonly openingBalance: string;
  readonly interestCharged: string;
  readonly insuranceCharged: string;
  readonly scheduled: Allocation;
  readonly catchup: Allocation;
  readonly extra: Allocation;
  readonly closingBalance: string;
  readonly overduePrincipal: string;
  readonly pendingInterest: string;
  readonly pendingInsurance: string;
  readonly arrears: string;
  readonly debt: string;
  readonly cashPaid: string;
  readonly commissionPaid: string;
  readonly vatPaid: string;
  readonly isPaidOff: boolean;
}

/**
 * Deep module encapsulating monthly charge calculation, payment priority waterfall,
 * arrears accumulation, reference amortization, and regional prepayment policies.
 */
export class AmortizationAccount {
  readonly basePayment: Decimal;
  private readonly monthlyRate: Decimal;
  private readonly termMonths: number;
  private readonly isPaymentExplicit: boolean;
  private readonly prepaymentRule: PrepaymentRule;

  private balance: Decimal;
  private interest: Decimal;
  private insurance: Decimal;
  private overdue: Decimal;
  private referenceBalance: Decimal;
  private lastProcessedMonth: number = 0;

  private constructor(terms: AmortizationTerms) {
    if (terms.principal.lte(0)) {
      throw new Error('Principal must be positive.');
    }
    if (terms.termMonths <= 0) {
      throw new Error('Term months must be positive.');
    }
    if (terms.monthlyRate.lt(0)) {
      throw new Error('Monthly rate cannot be negative.');
    }

    this.balance = new D(terms.principal);
    this.referenceBalance = new D(terms.principal);
    this.interest = new D(0);
    this.insurance = new D(0);
    this.overdue = new D(0);
    this.monthlyRate = new D(terms.monthlyRate);
    this.termMonths = terms.termMonths;
    this.isPaymentExplicit = terms.isPaymentExplicit ?? (terms.basePayment !== undefined);
    this.prepaymentRule = terms.prepaymentRule ?? new StandardPrepaymentAdapter();

    if (terms.basePayment !== undefined) {
      this.basePayment = new D(terms.basePayment);
    } else {
      this.basePayment = AmortizationAccount.calculateAnnuityBase(terms.principal, terms.monthlyRate, terms.termMonths);
    }
  }

  static open(terms: AmortizationTerms): AmortizationAccount {
    return new AmortizationAccount(terms);
  }

  static calculateAnnuityBase(principal: Decimal, rate: Decimal, termMonths: number): Decimal {
    if (rate.isZero()) {
      return new D(money(principal.div(termMonths)));
    }
    let discount = new D(1);
    let annuity = new D(0);
    for (let period = 0; period < termMonths; period++) {
      discount = discount.div(rate.plus(1));
      annuity = annuity.plus(discount);
    }
    return new D(money(principal.div(annuity)));
  }

  get currentBalance(): Decimal {
    return this.balance;
  }

  get pendingInterest(): Decimal {
    return this.interest;
  }

  get pendingInsurance(): Decimal {
    return this.insurance;
  }

  get overduePrincipal(): Decimal {
    return this.overdue;
  }

  totalDebt(): Decimal {
    return this.balance.plus(this.interest).plus(this.insurance);
  }

  arrears(): Decimal {
    return this.overdue.plus(this.interest).plus(this.insurance);
  }

  isPaidOff(): boolean {
    return this.totalDebt().isZero();
  }

  processMonth(input: MonthPaymentInput): MonthSettlement {
    if (input.month !== this.lastProcessedMonth + 1) {
      throw new Error(`Invalid month sequence: expected ${this.lastProcessedMonth + 1}, got ${input.month}`);
    }

    const paidOff = this.isPaidOff();
    const opening = this.balance;
    const currentInterest = new D(money(opening.times(this.monthlyRate)));

    const activeInsurance = input.activeInsurance ?? [];
    const currentInsurance = activeInsurance.reduce((sum, item) => {
      const charge = item.kind === 'fixed'
        ? new D(item.value)
        : new D(money(opening.times(item.value)));
      return sum.plus(charge);
    }, new D(0));

    const wasCurrent = this.interest.isZero() && this.insurance.isZero() && this.overdue.isZero();

    // 1. Accrue period charges
    this.interest = this.interest.plus(currentInterest);
    this.insurance = this.insurance.plus(currentInsurance);

    // 2. Accrue scheduled capital due into overdue capital
    const schedCapitalDue = D.min(
      D.max(0, this.basePayment.minus(currentInterest)),
      this.balance.minus(this.overdue)
    );
    this.overdue = this.overdue.plus(schedCapitalDue);

    // 3. Advance virtual reference balance for maturity residual absorption
    const refInterest = new D(money(this.referenceBalance.times(this.monthlyRate)));
    const referenceDue = this.referenceBalance.plus(refInterest);
    this.referenceBalance = D.max(0, referenceDue.minus(this.basePayment));

    // 4. Calculate scheduled payment offer
    let scheduledOffer = paidOff
      ? currentInsurance
      : D.min(this.basePayment.plus(currentInsurance), this.totalDebt());

    // Absorb rounding residual at maturity if loan was current and payment was not user-fixed
    if (!this.isPaymentExplicit && input.month === this.termMonths && wasCurrent && !paidOff
      && this.totalDebt().lte(D.max(this.basePayment, referenceDue).plus(currentInsurance))) {
      scheduledOffer = this.totalDebt();
    }

    if (!paidOff && input.isMissed) {
      scheduledOffer = new D(0);
    }

    // 5. Payment priority waterfall (insurance -> interest -> capital)
    const scheduled = this.allocate(scheduledOffer);
    const catchup = this.allocate(!paidOff ? (input.catchupOffer ?? new D(0)) : new D(0));
    const extra = this.allocate(!paidOff ? (input.extraOffer ?? new D(0)) : new D(0));

    // 6. Regional Prepayment Fee evaluation
    const prepaidCapital = new D(extra.principal);
    const prepaymentFee = this.prepaymentRule.evaluatePrepayment(input.month, prepaidCapital);

    // 7. Cash accounting
    const directCash = new D(scheduled.applied).plus(catchup.applied).plus(extra.applied);
    const totalCashPaid = directCash.plus(prepaymentFee.commission).plus(prepaymentFee.vat);

    // 8. Invariant verification across the seam
    if (this.balance.lt(0) || this.interest.lt(0) || this.insurance.lt(0) || this.overdue.lt(0)) {
      throw new Error('Financial invariant violated: negative balance detected.');
    }
    if (this.overdue.gt(this.balance)) {
      throw new Error('Financial invariant violated: overdue capital exceeds total balance.');
    }

    this.lastProcessedMonth = input.month;

    return {
      month: input.month,
      openingBalance: money(opening),
      interestCharged: money(currentInterest),
      insuranceCharged: money(currentInsurance),
      scheduled,
      catchup,
      extra,
      closingBalance: money(this.balance),
      overduePrincipal: money(this.overdue),
      pendingInterest: money(this.interest),
      pendingInsurance: money(this.insurance),
      arrears: money(this.arrears()),
      debt: money(this.totalDebt()),
      cashPaid: money(totalCashPaid),
      commissionPaid: money(prepaymentFee.commission),
      vatPaid: money(prepaymentFee.vat),
      isPaidOff: this.isPaidOff(),
    };
  }

  private allocate(offered: Decimal): Allocation {
    const paidInsurance = D.min(offered, this.insurance);
    const rem1 = offered.minus(paidInsurance);
    const paidInterest = D.min(rem1, this.interest);
    const rem2 = rem1.minus(paidInterest);
    const paidPrincipal = D.min(rem2, this.balance);

    this.insurance = this.insurance.minus(paidInsurance);
    this.interest = this.interest.minus(paidInterest);
    this.balance = this.balance.minus(paidPrincipal);
    this.overdue = D.max(0, this.overdue.minus(paidPrincipal));

    const applied = paidInsurance.plus(paidInterest).plus(paidPrincipal);
    const unapplied = offered.minus(applied);

    return {
      offered: money(offered),
      applied: money(applied),
      insurance: money(paidInsurance),
      interest: money(paidInterest),
      principal: money(paidPrincipal),
      unapplied: money(unapplied),
    };
  }
}
