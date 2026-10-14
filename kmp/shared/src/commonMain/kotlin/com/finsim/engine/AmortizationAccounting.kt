package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.core.FinsimLog
import com.finsim.domain.*

data class PrepaymentFee(
    val commission: Decimal,
    val vat: Decimal
)

/**
 * Seam for regional prepayment policies (e.g. Argentina Prepayment Profile).
 */
interface PrepaymentRule {
    fun evaluatePrepayment(month: Int, prepaidCapital: Decimal): PrepaymentFee
}

class StandardPrepaymentAdapter : PrepaymentRule {
    override fun evaluatePrepayment(month: Int, prepaidCapital: Decimal): PrepaymentFee {
        return PrepaymentFee(Decimal.ZERO, Decimal.ZERO)
    }
}

class ArgentinaPrepaymentAdapter(
    val originalTermMonths: Int,
    val commissionRate: Decimal = Decimal.parse("0.03"),
    val vatRate: Decimal = Decimal.parse("0.21")
) : PrepaymentRule {
    override fun evaluatePrepayment(month: Int, prepaidCapital: Decimal): PrepaymentFee {
        if (!prepaidCapital.isPositive) {
            return PrepaymentFee(Decimal.ZERO, Decimal.ZERO)
        }
        val freeThresholdMonth = (originalTermMonths + 3) / 4
        if (month >= freeThresholdMonth) {
            return PrepaymentFee(Decimal.ZERO, Decimal.ZERO)
        }
        val commission = Decimal.parse((prepaidCapital * commissionRate).toFixed(2))
        val vat = Decimal.parse((commission * vatRate).toFixed(2))
        return PrepaymentFee(commission, vat)
    }
}

data class AmortizationTerms(
    val principal: Decimal,
    val monthlyRate: Decimal,
    val termMonths: Int,
    val basePayment: Decimal? = null,
    val isPaymentExplicit: Boolean = false,
    val prepaymentRule: PrepaymentRule = StandardPrepaymentAdapter()
)

data class MonthPaymentInput(
    val month: Int,
    val isMissed: Boolean = false,
    val catchupOffer: Decimal = Decimal.ZERO,
    val extraOffer: Decimal = Decimal.ZERO,
    val activeInsurance: List<Insurance> = emptyList()
)

data class MonthSettlement(
    val month: Int,
    val openingBalance: String,
    val interestCharged: String,
    val insuranceCharged: String,
    val scheduled: Allocation,
    val catchup: Allocation,
    val extra: Allocation,
    val closingBalance: String,
    val overduePrincipal: String,
    val pendingInterest: String,
    val pendingInsurance: String,
    val arrears: String,
    val debt: String,
    val cashPaid: String,
    val commissionPaid: String,
    val vatPaid: String,
    val isPaidOff: Boolean
)

/**
 * Deep module encapsulating monthly charge calculation, payment priority waterfall,
 * arrears accumulation, reference amortization, and regional prepayment policies.
 */
class AmortizationAccount private constructor(
    val terms: AmortizationTerms
) {
    val basePayment: Decimal
    private var balance: Decimal
    private var referenceBalance: Decimal
    private var interest: Decimal = Decimal.ZERO
    private var insurance: Decimal = Decimal.ZERO
    private var overdue: Decimal = Decimal.ZERO
    private var lastProcessedMonth: Int = 0

    init {
        require(terms.principal.isPositive) { "Principal must be positive." }
        require(terms.termMonths > 0) { "Term months must be positive." }
        require(!terms.monthlyRate.isNegative) { "Monthly rate cannot be negative." }

        balance = terms.principal
        referenceBalance = terms.principal

        basePayment = if (terms.basePayment != null) {
            terms.basePayment
        } else {
            calculateAnnuityBase(terms.principal, terms.monthlyRate, terms.termMonths)
        }
    }

    companion object {
        fun open(terms: AmortizationTerms): AmortizationAccount = AmortizationAccount(terms)

        fun calculateAnnuityBase(principal: Decimal, rate: Decimal, termMonths: Int): Decimal {
            return if (rate.isZero) {
                Decimal.parse((principal / Decimal.fromInt(termMonths)).toFixed(2))
            } else {
                var discount = Decimal.ONE
                var annuity = Decimal.ZERO
                for (period in 0 until termMonths) {
                    discount = discount / (rate + Decimal.ONE)
                    annuity = annuity + discount
                }
                Decimal.parse((principal / annuity).toFixed(2))
            }
        }
    }

    val currentBalance: Decimal get() = balance
    val pendingInterest: Decimal get() = interest
    val pendingInsurance: Decimal get() = insurance
    val overduePrincipal: Decimal get() = overdue

    fun totalDebt(): Decimal = balance + interest + insurance
    fun arrears(): Decimal = overdue + interest + insurance
    fun isPaidOff(): Boolean = totalDebt().isZero

    fun processMonth(input: MonthPaymentInput): MonthSettlement {
        require(input.month == lastProcessedMonth + 1) {
            "Invalid month sequence: expected ${lastProcessedMonth + 1}, got ${input.month}"
        }

        val paidOff = isPaidOff()
        val opening = balance
        val currentInterest = Decimal.parse((opening * terms.monthlyRate).toFixed(2))

        var currentInsurance = Decimal.ZERO
        for (item in input.activeInsurance) {
            val charge = if (item.kind == "fixed") {
                Decimal.parse(item.value)
            } else {
                Decimal.parse((opening * Decimal.parse(item.value)).toFixed(2))
            }
            currentInsurance = currentInsurance + charge
        }

        val wasCurrent = interest.isZero && insurance.isZero && overdue.isZero

        // 1. Accrue period charges
        interest = interest + currentInterest
        insurance = insurance + currentInsurance

        // 2. Accrue scheduled capital due into overdue capital
        val schedCapitalDue = Decimal.min(
            Decimal.max(Decimal.ZERO, basePayment - currentInterest),
            balance - overdue
        )
        overdue = overdue + schedCapitalDue

        // 3. Advance virtual reference balance for maturity residual absorption
        val refInterest = Decimal.parse((referenceBalance * terms.monthlyRate).toFixed(2))
        val referenceDue = referenceBalance + refInterest
        referenceBalance = Decimal.max(Decimal.ZERO, referenceDue - basePayment)

        // 4. Calculate scheduled payment offer
        var scheduledOffer = if (paidOff) currentInsurance else Decimal.min(basePayment + currentInsurance, totalDebt())
        if (!terms.isPaymentExplicit && input.month == terms.termMonths && wasCurrent && !paidOff &&
            totalDebt() <= (Decimal.max(basePayment, referenceDue) + currentInsurance)
        ) {
            scheduledOffer = totalDebt()
        }
        if (!paidOff && input.isMissed) {
            scheduledOffer = Decimal.ZERO
        }

        // 5. Payment priority waterfall (insurance -> interest -> capital)
        val scheduled = allocate(scheduledOffer)
        val catchup = allocate(if (!paidOff) input.catchupOffer else Decimal.ZERO)
        val extra = allocate(if (!paidOff) input.extraOffer else Decimal.ZERO)

        // 6. Regional Prepayment Fee evaluation
        val prepaidCapital = Decimal.parse(extra.principal)
        val fee = terms.prepaymentRule.evaluatePrepayment(input.month, prepaidCapital)

        // 7. Cash accounting
        val directCash = Decimal.parse(scheduled.applied) + Decimal.parse(catchup.applied) + Decimal.parse(extra.applied)
        val monthTotalCash = directCash + fee.commission + fee.vat

        // 8. Invariant verification across the seam
        check(!balance.isNegative && !interest.isNegative && !insurance.isNegative && !overdue.isNegative) {
            "Financial invariant violated: negative balance"
        }
        check(overdue <= balance) {
            "Financial invariant violated: overdue principal ($overdue) exceeds balance ($balance)"
        }

        lastProcessedMonth = input.month

        return MonthSettlement(
            month = input.month,
            openingBalance = opening.toFixed(2),
            interestCharged = currentInterest.toFixed(2),
            insuranceCharged = currentInsurance.toFixed(2),
            scheduled = scheduled,
            catchup = catchup,
            extra = extra,
            closingBalance = balance.toFixed(2),
            overduePrincipal = overdue.toFixed(2),
            pendingInterest = interest.toFixed(2),
            pendingInsurance = insurance.toFixed(2),
            arrears = arrears().toFixed(2),
            debt = totalDebt().toFixed(2),
            cashPaid = monthTotalCash.toFixed(2),
            commissionPaid = fee.commission.toFixed(2),
            vatPaid = fee.vat.toFixed(2),
            isPaidOff = isPaidOff()
        )
    }

    private fun allocate(offered: Decimal): Allocation {
        val paidInsurance = Decimal.min(offered, insurance)
        val rem1 = offered - paidInsurance
        val paidInterest = Decimal.min(rem1, interest)
        val rem2 = rem1 - paidInterest
        val paidPrincipal = Decimal.min(rem2, balance)

        insurance = insurance - paidInsurance
        interest = interest - paidInterest
        balance = balance - paidPrincipal
        overdue = Decimal.max(Decimal.ZERO, overdue - paidPrincipal)

        val applied = paidInsurance + paidInterest + paidPrincipal
        val unapplied = offered - applied
        return Allocation(
            offered = offered.toFixed(2),
            applied = applied.toFixed(2),
            insurance = paidInsurance.toFixed(2),
            interest = paidInterest.toFixed(2),
            principal = paidPrincipal.toFixed(2),
            unapplied = unapplied.toFixed(2)
        )
    }
}
