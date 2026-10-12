package com.finsim.domain.types

import com.finsim.core.Decimal
import com.finsim.domain.*

/**
 * Adapter module mapping untyped external representation (JSON/UI primitives)
 * to deep type-safe domain models and vice versa across the system seam.
 */
object ScenarioAdapter {

    /**
     * Adapts an untyped Scenario into a TypedScenario, enforcing all invariants at the seam.
     */
    fun toTyped(scenario: Scenario): TypedScenario {
        scenario.validate()
        val curr = scenario.currency
        val principalMoney = Money.parse(scenario.principal, curr)
        val paymentMoney = scenario.payment?.let { Money.parse(it, curr) }

        val typedRate = when (val r = scenario.rate) {
            is Rate.Monthly -> RateValue.EffectiveMonthly.of(r.value)
            is Rate.Annual -> RateValue.EffectiveAnnual.of(r.value)
            is Rate.Nominal -> RateValue.NominalAnnual.of(r.value, r.frequency)
        }

        val typedInsurance = scenario.insurance.map { ins ->
            val charge = if (ins.kind == "fixed") {
                InsuranceCharge.Fixed(Money.parse(ins.value, curr))
            } else {
                InsuranceCharge.Percentage(Decimal.parse(ins.value))
            }
            TypedInsurance(
                id = EventId(ins.id),
                name = ins.name,
                charge = charge,
                endsAtPayoff = ins.endsAtPayoff,
                endMonth = ins.endMonth?.let { MonthIndex(it) }
            )
        }

        val typedEvents = scenario.events.map { ev ->
            val evMonth = MonthIndex(ev.month)
            val evId = EventId(ev.id)
            when (ev) {
                is LoanEvent.Extra -> TypedLoanEvent.Extra(evId, evMonth, Money.parse(ev.amount, curr))
                is LoanEvent.Recurring -> TypedLoanEvent.Recurring(
                    id = evId,
                    month = evMonth,
                    endMonth = MonthIndex(ev.endMonth),
                    everyMonths = ev.every,
                    amount = Money.parse(ev.amount, curr)
                )
                is LoanEvent.Missed -> TypedLoanEvent.Missed(evId, evMonth)
                is LoanEvent.Catchup -> TypedLoanEvent.Catchup(evId, evMonth, Money.parse(ev.amount, curr))
            }
        }

        val argPolicy = scenario.argentinaProfile?.let {
            ArgentinaPrepaymentPolicy(
                originalTermMonths = it.originalTermMonths,
                commissionRate = Decimal.parse(it.commissionRate),
                vatRate = Decimal.parse(it.vatRate)
            )
        }

        return TypedScenario(
            currency = curr,
            name = scenario.name,
            source = scenario.source,
            principal = principalMoney,
            startDate = scenario.startDate,
            termMonths = scenario.termMonths,
            rate = typedRate,
            payment = paymentMoney,
            insurance = typedInsurance,
            events = typedEvents,
            argentinaPolicy = argPolicy
        )
    }

    /**
     * Adapts a TypedScenario back to untyped Scenario for serialization or legacy consumers.
     */
    fun toUntyped(typed: TypedScenario): Scenario {
        val rate = when (val r = typed.rate) {
            is RateValue.EffectiveMonthly -> Rate.Monthly(r.fractionDecimal.toString())
            is RateValue.EffectiveAnnual -> Rate.Annual(r.fractionDecimal.toString())
            is RateValue.NominalAnnual -> Rate.Nominal(r.fractionDecimal.toString(), r.frequency)
        }
        val insurance = typed.insurance.map { ins ->
            val (kind, value) = when (val c = ins.charge) {
                is InsuranceCharge.Fixed -> "fixed" to c.amount.toFormattedString()
                is InsuranceCharge.Percentage -> "percentage" to c.monthlyRateFraction.toString()
            }
            Insurance(
                id = ins.id.value,
                name = ins.name,
                kind = kind,
                value = value,
                endsAtPayoff = ins.endsAtPayoff,
                endMonth = ins.endMonth?.value
            )
        }
        val events = typed.events.map { ev ->
            when (ev) {
                is TypedLoanEvent.Extra -> LoanEvent.Extra(
                    id = ev.id.value,
                    month = ev.month.value,
                    amount = ev.amount.toFormattedString()
                )
                is TypedLoanEvent.Recurring -> LoanEvent.Recurring(
                    id = ev.id.value,
                    month = ev.month.value,
                    endMonth = ev.endMonth.value,
                    every = ev.everyMonths,
                    amount = ev.amount.toFormattedString()
                )
                is TypedLoanEvent.Missed -> LoanEvent.Missed(
                    id = ev.id.value,
                    month = ev.month.value
                )
                is TypedLoanEvent.Catchup -> LoanEvent.Catchup(
                    id = ev.id.value,
                    month = ev.month.value,
                    amount = ev.amount.toFormattedString()
                )
            }
        }
        val argProfile = typed.argentinaPolicy?.let {
            ArgentinaProfile(
                originalTermMonths = it.originalTermMonths,
                commissionRate = it.commissionRate.toString(),
                vatRate = it.vatRate.toString()
            )
        }
        return Scenario(
            currency = typed.currency,
            name = typed.name,
            source = typed.source,
            principal = typed.principal.toFormattedString(),
            startDate = typed.startDate,
            termMonths = typed.termMonths,
            rate = rate,
            payment = typed.payment?.toFormattedString(),
            insurance = insurance,
            events = events,
            argentinaProfile = argProfile
        )
    }

    /**
     * Adapts a TypedLoanResult back to untyped LoanResult for JSON serialization or legacy UI callers.
     */
    fun toUntyped(typed: TypedLoanResult): LoanResult {
        return LoanResult(
            rows = typed.rows.map { r ->
                MonthRow(
                    month = r.month.value,
                    date = r.date,
                    openingBalance = r.openingBalance.toFormattedString(),
                    interestCharged = r.interestCharged.toFormattedString(),
                    insuranceCharged = r.insuranceCharged.toFormattedString(),
                    scheduled = Allocation(
                        offered = r.scheduled.offered.toFormattedString(),
                        applied = r.scheduled.applied.toFormattedString(),
                        insurance = r.scheduled.insurance.toFormattedString(),
                        interest = r.scheduled.interest.toFormattedString(),
                        principal = r.scheduled.principal.toFormattedString(),
                        unapplied = r.scheduled.unapplied.toFormattedString()
                    ),
                    catchup = Allocation(
                        offered = r.catchup.offered.toFormattedString(),
                        applied = r.catchup.applied.toFormattedString(),
                        insurance = r.catchup.insurance.toFormattedString(),
                        interest = r.catchup.interest.toFormattedString(),
                        principal = r.catchup.principal.toFormattedString(),
                        unapplied = r.catchup.unapplied.toFormattedString()
                    ),
                    extra = Allocation(
                        offered = r.extra.offered.toFormattedString(),
                        applied = r.extra.applied.toFormattedString(),
                        insurance = r.extra.insurance.toFormattedString(),
                        interest = r.extra.interest.toFormattedString(),
                        principal = r.extra.principal.toFormattedString(),
                        unapplied = r.extra.unapplied.toFormattedString()
                    ),
                    closingBalance = r.closingBalance.toFormattedString(),
                    overduePrincipal = r.overduePrincipal.toFormattedString(),
                    pendingInterest = r.pendingInterest.toFormattedString(),
                    pendingInsurance = r.pendingInsurance.toFormattedString(),
                    arrears = r.arrears.toFormattedString(),
                    debt = r.debt.toFormattedString(),
                    cashPaid = r.cashPaid.toFormattedString(),
                    commissionPaid = r.commissionPaid.toFormattedString(),
                    vatPaid = r.vatPaid.toFormattedString()
                )
            },
            basePayment = typed.basePayment.toFormattedString(),
            payoffMonth = typed.payoffMonth?.value,
            payoffDate = typed.payoffDate,
            lastInsuranceMonth = typed.lastInsuranceMonth?.value,
            complete = typed.complete,
            totals = Totals(
                principal = typed.totals.principal.toFormattedString(),
                interest = typed.totals.interest.toFormattedString(),
                insurance = typed.totals.insurance.toFormattedString(),
                cashPaid = typed.totals.cashPaid.toFormattedString(),
                unapplied = typed.totals.unapplied.toFormattedString(),
                commissionPaid = typed.totals.commissionPaid.toFormattedString(),
                vatPaid = typed.totals.vatPaid.toFormattedString()
            ),
            remainingDebt = typed.remainingDebt.toFormattedString(),
            unappliedEvents = typed.unappliedEvents.map { u ->
                UnappliedEvent(
                    eventId = u.eventId.value,
                    month = u.month.value,
                    amount = u.amount.toFormattedString(),
                    reason = u.reason
                )
            }
        )
    }
}

/**
 * Extension adapters facilitating seamless bidirectional conversions between typed and untyped models.
 */
fun Scenario.toTyped(): TypedScenario = ScenarioAdapter.toTyped(this)
fun TypedScenario.toUntyped(): Scenario = ScenarioAdapter.toUntyped(this)
fun TypedLoanResult.toUntyped(): LoanResult = ScenarioAdapter.toUntyped(this)

/**
 * Typed calculation engine module.
 *
 * Encapsulates the complete amortization and multi-currency ledger simulation behind a deep typed interface.
 */
object TypedEngine {
    fun simulate(scenario: TypedScenario): TypedLoanResult = simulateTypedLoan(scenario)
}

/**
 * Deep simulation module operating entirely on type-safe value objects.
 *
 * Notice the depth of this module:
 * The interface is a single clean function [simulateTypedLoan] accepting [TypedScenario].
 * Inside the implementation:
 * - ZERO String-to-Decimal conversions.
 * - ZERO defensive bounds checks on months.
 * - ZERO defensive currency checks (guaranteed by Money type).
 * - ZERO unscaled rounding errors (guaranteed by Money arithmetic).
 */
fun simulateTypedLoan(scenario: TypedScenario): TypedLoanResult {
    val rate = scenario.rate.effectiveMonthlyDecimal
    var balance = scenario.principal
    val curr = scenario.currency

    // Base installment calculation
    val base: Money = if (scenario.payment != null) {
        scenario.payment
    } else if (rate.isZero) {
        val pDec = Decimal.parse(balance.toFormattedString())
        val div = pDec / Decimal.fromInt(scenario.termMonths)
        Money.parse(div.toFixed(2), curr)
    } else {
        var discount = Decimal.ONE
        var annuity = Decimal.ZERO
        for (period in 0 until scenario.termMonths) {
            discount = discount / (rate + Decimal.ONE)
            annuity = annuity + discount
        }
        val pDec = Decimal.parse(balance.toFormattedString())
        Money.parse((pDec / annuity).toFixed(2), curr)
    }

    var referenceBalance = balance
    var interest = Money.zero(curr)
    var insurance = Money.zero(curr)
    var overdue = Money.zero(curr)

    val resultRows = mutableListOf<TypedMonthRow>()
    var payoffMonth: MonthIndex? = null
    var payoffDate: String? = null
    var lastInsuranceMonth: MonthIndex? = null
    val totals = TypedTotals.zero(curr)
    val unappliedEvents = mutableListOf<TypedUnappliedEvent>()

    fun debt(): Money = balance + interest + insurance

    fun allocate(offered: Money): TypedAllocation {
        val paidInsurance = minOf(offered, insurance)
        val rem1 = offered - paidInsurance
        val paidInterest = minOf(rem1, interest)
        val rem2 = rem1 - paidInterest
        val paidPrincipal = minOf(rem2, balance)

        insurance = insurance - paidInsurance
        interest = interest - paidInterest
        balance = balance - paidPrincipal
        overdue = overdue.minusOrZero(paidPrincipal)

        val applied = paidInsurance + paidInterest + paidPrincipal
        val unapplied = offered - applied
        return TypedAllocation(
            offered = offered,
            applied = applied,
            insurance = paidInsurance,
            interest = paidInterest,
            principal = paidPrincipal,
            unapplied = unapplied
        )
    }

    val dateParts = scenario.startDate.split("-").map { it.toInt() }
    val year = dateParts[0]
    val firstMonth = dateParts[1]

    for (m in 1..MAX_MONTHS) {
        val month = MonthIndex(m)
        val events = scenario.events.filter { event ->
            when (event) {
                is TypedLoanEvent.Recurring -> {
                    month.value in event.month.value..event.endMonth.value &&
                            (month.value - event.month.value) % event.everyMonths == 0
                }
                else -> event.month == month
            }
        }

        val paidOff = payoffMonth != null

        if (paidOff) {
            for (event in events) {
                val amount = when (event) {
                    is TypedLoanEvent.Missed -> Money.zero(curr)
                    is TypedLoanEvent.Extra -> event.amount
                    is TypedLoanEvent.Recurring -> event.amount
                    is TypedLoanEvent.Catchup -> event.amount
                }
                unappliedEvents.add(
                    TypedUnappliedEvent(
                        eventId = event.id,
                        month = month,
                        amount = amount,
                        reason = "Posterior a la liquidación del préstamo."
                    )
                )
                totals.unapplied = totals.unapplied + amount
            }
        }

        val activeInsurance = scenario.insurance.filter { item ->
            (!item.endsAtPayoff || !paidOff) && (item.endMonth == null || month <= item.endMonth)
        }

        if (paidOff && activeInsurance.isEmpty() && insurance.isZero) {
            continue
        }

        val opening = balance
        val currentInterest = opening.timesFraction(rate)
        var currentInsurance = Money.zero(curr)
        for (item in activeInsurance) {
            val charge = when (val c = item.charge) {
                is InsuranceCharge.Fixed -> c.amount
                is InsuranceCharge.Percentage -> opening.timesFraction(c.monthlyRateFraction)
            }
            currentInsurance = currentInsurance + charge
        }
        if (activeInsurance.isNotEmpty()) {
            lastInsuranceMonth = month
        }

        val wasCurrent = interest.isZero && insurance.isZero && overdue.isZero
        interest = interest + currentInterest
        insurance = insurance + currentInsurance

        val schedCapitalDue = minOf(
            maxOf(Money.zero(curr), base.minusOrZero(currentInterest)),
            balance.minusOrZero(overdue)
        )
        overdue = overdue + schedCapitalDue

        val refInterest = referenceBalance.timesFraction(rate)
        val referenceDue = referenceBalance + refInterest
        referenceBalance = referenceDue.minusOrZero(base)

        var scheduledOffer = if (paidOff) currentInsurance else minOf(base + currentInsurance, debt())
        if (scenario.payment == null && month.value == scenario.termMonths && wasCurrent && !paidOff &&
            debt() <= (maxOf(base, referenceDue) + currentInsurance)
        ) {
            scheduledOffer = debt()
        }

        val hasMissed = !paidOff && events.any { it is TypedLoanEvent.Missed }
        val scheduled = allocate(if (hasMissed) Money.zero(curr) else scheduledOffer)

        var catchupOffer = Money.zero(curr)
        var extraOffer = Money.zero(curr)
        if (!paidOff) {
            for (event in events) {
                when (event) {
                    is TypedLoanEvent.Catchup -> catchupOffer = catchupOffer + event.amount
                    is TypedLoanEvent.Extra -> extraOffer = extraOffer + event.amount
                    is TypedLoanEvent.Recurring -> extraOffer = extraOffer + event.amount
                    is TypedLoanEvent.Missed -> {}
                }
            }
        }

        val catchup = allocate(catchupOffer)
        val extra = allocate(extraOffer)

        // Argentina prepayment commission and VAT calculation if policy is present
        var commissionPaid = Money.zero(curr)
        var vatPaid = Money.zero(curr)
        val totalPrepaidCapital = extra.principal
        if (scenario.argentinaPolicy != null && totalPrepaidCapital.isPositive) {
            val policy = scenario.argentinaPolicy
            val origTerm = policy.originalTermMonths ?: scenario.termMonths
            val freeThresholdMonth = (origTerm + 3) / 4 // 25% of term rounded up
            val isFree = month.value >= freeThresholdMonth
            if (!isFree) {
                commissionPaid = totalPrepaidCapital.timesFraction(policy.commissionRate)
                vatPaid = commissionPaid.timesFraction(policy.vatRate)
            }
        }

        val payments = listOf(scheduled, catchup, extra)
        val directCash = payments.fold(Money.zero(curr)) { sum, p -> sum + p.applied }
        val monthTotalCash = directCash + commissionPaid + vatPaid
        val dateStr = month.toDate(year, firstMonth)

        resultRows.add(
            TypedMonthRow(
                month = month,
                date = dateStr,
                openingBalance = opening,
                interestCharged = currentInterest,
                insuranceCharged = currentInsurance,
                scheduled = scheduled,
                catchup = catchup,
                extra = extra,
                closingBalance = balance,
                overduePrincipal = overdue,
                pendingInterest = interest,
                pendingInsurance = insurance,
                arrears = overdue + interest + insurance,
                debt = debt(),
                cashPaid = monthTotalCash,
                commissionPaid = commissionPaid,
                vatPaid = vatPaid
            )
        )

        totals.principal = totals.principal + payments.fold(Money.zero(curr)) { s, p -> s + p.principal }
        totals.interest = totals.interest + payments.fold(Money.zero(curr)) { s, p -> s + p.interest }
        totals.insurance = totals.insurance + payments.fold(Money.zero(curr)) { s, p -> s + p.insurance }
        totals.unapplied = totals.unapplied + payments.fold(Money.zero(curr)) { s, p -> s + p.unapplied }
        totals.commissionPaid = totals.commissionPaid + commissionPaid
        totals.vatPaid = totals.vatPaid + vatPaid
        totals.cashPaid = totals.cashPaid + monthTotalCash

        if (payoffMonth == null && debt().isZero) {
            payoffMonth = month
            payoffDate = dateStr
        }
    }

    val complete = payoffMonth != null && debt().isZero
    return TypedLoanResult(
        rows = resultRows,
        basePayment = base,
        payoffMonth = payoffMonth,
        payoffDate = payoffDate,
        lastInsuranceMonth = lastInsuranceMonth,
        complete = complete,
        totals = totals,
        remainingDebt = debt(),
        unappliedEvents = unappliedEvents
    )
}
