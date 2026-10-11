package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.core.FinsimLog
import com.finsim.domain.*

private fun money(value: Decimal): String = value.toFixed(2)
private fun money(value: String): String = Decimal.parse(value).toFixed(2)

fun simulateLoan(scenario: Scenario): LoanResult {
    scenario.validate()
    FinsimLog.engine.i { "simulateLoan start: principal=${scenario.principal}, rate=${scenario.rate.value}, term=${scenario.termMonths}, events=${scenario.events.size}" }
    val rate = toMonthlyRate(scenario.rate)
    var balance = Decimal.parse(scenario.principal)

    // Base installment calculation
    val base: Decimal = if (scenario.payment != null) {
        Decimal.parse(scenario.payment)
    } else if (rate.isZero) {
        Decimal.parse((balance / Decimal.fromInt(scenario.termMonths)).toFixed(2))
    } else {
        var discount = Decimal.ONE
        var annuity = Decimal.ZERO
        for (period in 0 until scenario.termMonths) {
            discount = discount / (rate + Decimal.ONE)
            annuity = annuity + discount
        }
        Decimal.parse((balance / annuity).toFixed(2))
    }

    var referenceBalance = balance
    var interest = Decimal.ZERO
    var insurance = Decimal.ZERO
    var overdue = Decimal.ZERO

    val resultRows = mutableListOf<MonthRow>()
    var payoffMonth: Int? = null
    var payoffDate: String? = null
    var lastInsuranceMonth: Int? = null
    val totals = Totals()
    val unappliedEvents = mutableListOf<UnappliedEvent>()

    fun debt(): Decimal = balance + interest + insurance

    fun allocate(offered: Decimal): Allocation {
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
            offered = money(offered),
            applied = money(applied),
            insurance = money(paidInsurance),
            interest = money(paidInterest),
            principal = money(paidPrincipal),
            unapplied = money(unapplied)
        )
    }

    val dateParts = scenario.startDate.split("-").map { it.toInt() }
    val year = dateParts[0]
    val firstMonth = dateParts[1]

    for (month in 1..MAX_MONTHS) {
        val events = scenario.events.filter { event ->
            when (event) {
                is LoanEvent.Recurring -> {
                    month in event.month..event.endMonth && (month - event.month) % event.every == 0
                }
                else -> event.month == month
            }
        }

        val paidOff = payoffMonth != null

        if (paidOff) {
            for (event in events) {
                val amount = when (event) {
                    is LoanEvent.Missed -> "0.00"
                    is LoanEvent.Extra -> money(event.amount)
                    is LoanEvent.Recurring -> money(event.amount)
                    is LoanEvent.Catchup -> money(event.amount)
                }
                unappliedEvents.add(
                    UnappliedEvent(
                        eventId = event.id,
                        month = month,
                        amount = amount,
                        reason = "Posterior a la liquidación del préstamo."
                    )
                )
                totals.unapplied = money(Decimal.parse(totals.unapplied) + Decimal.parse(amount))
            }
        }

        val activeInsurance = scenario.insurance.filter { item ->
            (!item.endsAtPayoff || !paidOff) && (item.endMonth == null || month <= item.endMonth)
        }

        if (paidOff && activeInsurance.isEmpty() && insurance.isZero) {
            continue
        }

        val opening = balance
        val currentInterest = Decimal.parse(money(opening * rate))
        var currentInsurance = Decimal.ZERO
        for (item in activeInsurance) {
            val charge = if (item.kind == "fixed") {
                Decimal.parse(item.value)
            } else {
                Decimal.parse(money(opening * Decimal.parse(item.value)))
            }
            currentInsurance = currentInsurance + charge
        }
        if (activeInsurance.isNotEmpty()) {
            lastInsuranceMonth = month
        }

        val wasCurrent = interest.isZero && insurance.isZero && overdue.isZero
        interest = interest + currentInterest
        insurance = insurance + currentInsurance

        val schedCapitalDue = Decimal.min(Decimal.max(Decimal.ZERO, base - currentInterest), balance - overdue)
        overdue = overdue + schedCapitalDue

        val refInterest = Decimal.parse(money(referenceBalance * rate))
        val referenceDue = referenceBalance + refInterest
        referenceBalance = Decimal.max(Decimal.ZERO, referenceDue - base)

        var scheduledOffer = if (paidOff) currentInsurance else Decimal.min(base + currentInsurance, debt())
        if (scenario.payment == null && month == scenario.termMonths && wasCurrent && !paidOff &&
            debt() <= (Decimal.max(base, referenceDue) + currentInsurance)
        ) {
            scheduledOffer = debt()
        }

        val hasMissed = !paidOff && events.any { it is LoanEvent.Missed }
        if (hasMissed) {
            FinsimLog.engine.w { "Month $month: cuota omitida por evento. Atraso acumulado: ${debt()}" }
        }
        val scheduled = allocate(if (hasMissed) Decimal.ZERO else scheduledOffer)

        var catchupOffer = Decimal.ZERO
        var extraOffer = Decimal.ZERO
        if (!paidOff) {
            for (event in events) {
                when (event) {
                    is LoanEvent.Catchup -> catchupOffer = catchupOffer + Decimal.parse(event.amount)
                    is LoanEvent.Extra -> extraOffer = extraOffer + Decimal.parse(event.amount)
                    is LoanEvent.Recurring -> extraOffer = extraOffer + Decimal.parse(event.amount)
                    is LoanEvent.Missed -> {}
                }
            }
        }

        val catchup = allocate(catchupOffer)
        val extra = allocate(extraOffer)
        if (extraOffer.isPositive) {
            FinsimLog.engine.d { "Month $month: abono extraordinario ofrecido $extraOffer, aplicado: capital=${extra.principal}, intereses=${extra.interest}, seguros=${extra.insurance}, excedente=${extra.unapplied}" }
        }
        if (catchupOffer.isPositive) {
            FinsimLog.engine.d { "Month $month: puesta al día ofrecida $catchupOffer, aplicada: capital=${catchup.principal}, intereses=${catchup.interest}, seguros=${catchup.insurance}" }
        }

        // Argentina prepayment commission and VAT calculation if profile is present
        var commissionDec = Decimal.ZERO
        var vatDec = Decimal.ZERO
        val totalPrepaidCapital = Decimal.parse(extra.principal)
        if (scenario.argentinaProfile != null && totalPrepaidCapital.isPositive) {
            val argProfile = scenario.argentinaProfile
            val origTerm = argProfile.originalTermMonths ?: scenario.termMonths
            val freeThresholdMonth = (origTerm + 3) / 4 // 25% of term rounded up
            val isFree = month >= freeThresholdMonth
            if (!isFree) {
                val commRate = Decimal.parse(argProfile.commissionRate)
                val vRate = Decimal.parse(argProfile.vatRate)
                commissionDec = Decimal.parse(money(totalPrepaidCapital * commRate))
                vatDec = Decimal.parse(money(commissionDec * vRate))
                FinsimLog.engine.i { "Month $month: precancelación antes del umbral (mes $freeThresholdMonth). Comisión: $commissionDec, IVA: $vatDec sobre capital prepagado $totalPrepaidCapital" }
            } else {
                FinsimLog.engine.d { "Month $month: precancelación gratuita argentina (mes $month >= umbral $freeThresholdMonth)" }
            }
        }

        val payments = listOf(scheduled, catchup, extra)
        val directCash = payments.fold(Decimal.ZERO) { sum, p -> sum + Decimal.parse(p.applied) }
        val monthTotalCash = directCash + commissionDec + vatDec
        val cashPaidStr = money(monthTotalCash)

        val dateIndex = year * 12 + firstMonth - 1 + month - 1
        val dateYear = dateIndex / 12
        val dateMonth = dateIndex % 12 + 1
        val dateStr = "${dateYear.toString().padStart(4, '0')}-${dateMonth.toString().padStart(2, '0')}"

        resultRows.add(
            MonthRow(
                month = month,
                date = dateStr,
                openingBalance = money(opening),
                interestCharged = money(currentInterest),
                insuranceCharged = money(currentInsurance),
                scheduled = scheduled,
                catchup = catchup,
                extra = extra,
                closingBalance = money(balance),
                overduePrincipal = money(overdue),
                pendingInterest = money(interest),
                pendingInsurance = money(insurance),
                arrears = money(overdue + interest + insurance),
                debt = money(debt()),
                cashPaid = cashPaidStr,
                commissionPaid = money(commissionDec),
                vatPaid = money(vatDec)
            )
        )

        totals.principal = money(Decimal.parse(totals.principal) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.principal) })
        totals.interest = money(Decimal.parse(totals.interest) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.interest) })
        totals.insurance = money(Decimal.parse(totals.insurance) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.insurance) })
        totals.unapplied = money(Decimal.parse(totals.unapplied) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.unapplied) })
        totals.commissionPaid = money(Decimal.parse(totals.commissionPaid) + commissionDec)
        totals.vatPaid = money(Decimal.parse(totals.vatPaid) + vatDec)
        totals.cashPaid = money(Decimal.parse(totals.cashPaid) + monthTotalCash)

        if (payoffMonth == null && debt().isZero) {
            payoffMonth = month
            payoffDate = dateStr
            FinsimLog.engine.i { "Month $month: liquidación total del préstamo alcanzada (deuda = 0)" }
        }
    }

    val complete = payoffMonth != null && debt().isZero
    if (!complete) {
        FinsimLog.engine.w { "Simulación concluida sin liquidar totalmente el préstamo. Meses simulados: ${resultRows.size}, deuda remanente: ${debt()}" }
    } else {
        FinsimLog.engine.i { "Simulación completada con éxito en mes $payoffMonth. Total intereses: ${totals.interest}, total seguros: ${totals.insurance}, total desembolsado: ${totals.cashPaid}" }
    }
    return LoanResult(
        rows = resultRows,
        basePayment = money(base),
        payoffMonth = payoffMonth,
        payoffDate = payoffDate,
        lastInsuranceMonth = lastInsuranceMonth,
        complete = complete,
        totals = totals,
        remainingDebt = money(debt()),
        unappliedEvents = unappliedEvents
    )
}
