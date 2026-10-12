package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.core.FinsimLog
import com.finsim.domain.*
import com.finsim.domain.types.TypedEngine
import com.finsim.domain.types.TypedLoanResult
import com.finsim.domain.types.TypedScenario

private fun money(value: Decimal): String = value.toFixed(2)
private fun money(value: String): String = Decimal.parse(value).toFixed(2)

fun simulateLoan(scenario: Scenario): LoanResult {
    scenario.validate()
    FinsimLog.engine.i { "simulateLoan start: principal=${scenario.principal}, rate=${scenario.rate.value}, term=${scenario.termMonths}, events=${scenario.events.size}" }
    val rate = toMonthlyRate(scenario.rate)

    val prepaymentRule = if (scenario.argentinaProfile != null) {
        val arg = scenario.argentinaProfile
        ArgentinaPrepaymentAdapter(
            originalTermMonths = arg.originalTermMonths ?: scenario.termMonths,
            commissionRate = Decimal.parse(arg.commissionRate),
            vatRate = Decimal.parse(arg.vatRate)
        )
    } else {
        StandardPrepaymentAdapter()
    }

    val account = AmortizationAccount.open(
        AmortizationTerms(
            principal = Decimal.parse(scenario.principal),
            monthlyRate = rate,
            termMonths = scenario.termMonths,
            basePayment = scenario.payment?.let { Decimal.parse(it) },
            isPaymentExplicit = scenario.payment != null,
            prepaymentRule = prepaymentRule
        )
    )

    val resultRows = mutableListOf<MonthRow>()
    var payoffMonth: Int? = null
    var payoffDate: String? = null
    var lastInsuranceMonth: Int? = null
    val totals = Totals()
    val unappliedEvents = mutableListOf<UnappliedEvent>()

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

        if (paidOff && activeInsurance.isEmpty() && account.pendingInsurance.isZero) {
            continue
        }

        if (activeInsurance.isNotEmpty()) {
            lastInsuranceMonth = month
        }

        val hasMissed = !paidOff && events.any { it is LoanEvent.Missed }
        if (hasMissed) {
            FinsimLog.engine.w { "Month $month: cuota omitida por evento. Atraso acumulado: ${account.arrears()}" }
        }

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

        val settlement = account.processMonth(
            MonthPaymentInput(
                month = month,
                isMissed = hasMissed,
                catchupOffer = catchupOffer,
                extraOffer = extraOffer,
                activeInsurance = activeInsurance
            )
        )

        val payments = listOf(settlement.scheduled, settlement.catchup, settlement.extra)

        val dateIndex = year * 12 + firstMonth - 1 + month - 1
        val dateYear = dateIndex / 12
        val dateMonth = dateIndex % 12 + 1
        val dateStr = "${dateYear.toString().padStart(4, '0')}-${dateMonth.toString().padStart(2, '0')}"

        resultRows.add(
            MonthRow(
                month = month,
                date = dateStr,
                openingBalance = settlement.openingBalance,
                interestCharged = settlement.interestCharged,
                insuranceCharged = settlement.insuranceCharged,
                scheduled = settlement.scheduled,
                catchup = settlement.catchup,
                extra = settlement.extra,
                closingBalance = settlement.closingBalance,
                overduePrincipal = settlement.overduePrincipal,
                pendingInterest = settlement.pendingInterest,
                pendingInsurance = settlement.pendingInsurance,
                arrears = settlement.arrears,
                debt = settlement.debt,
                cashPaid = settlement.cashPaid,
                commissionPaid = settlement.commissionPaid,
                vatPaid = settlement.vatPaid
            )
        )

        totals.principal = money(Decimal.parse(totals.principal) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.principal) })
        totals.interest = money(Decimal.parse(totals.interest) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.interest) })
        totals.insurance = money(Decimal.parse(totals.insurance) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.insurance) })
        totals.unapplied = money(Decimal.parse(totals.unapplied) + payments.fold(Decimal.ZERO) { s, p -> s + Decimal.parse(p.unapplied) })
        totals.commissionPaid = money(Decimal.parse(totals.commissionPaid) + Decimal.parse(settlement.commissionPaid))
        totals.vatPaid = money(Decimal.parse(totals.vatPaid) + Decimal.parse(settlement.vatPaid))
        totals.cashPaid = money(Decimal.parse(totals.cashPaid) + Decimal.parse(settlement.cashPaid))

        if (payoffMonth == null && account.isPaidOff()) {
            payoffMonth = month
            payoffDate = dateStr
            FinsimLog.engine.i { "Month $month: liquidación total del préstamo alcanzada (deuda = 0)" }
        }
    }

    val complete = payoffMonth != null && account.isPaidOff()
    if (!complete) {
        FinsimLog.engine.w { "Simulación concluida sin liquidar totalmente el préstamo. Meses simulados: ${resultRows.size}, deuda remanente: ${account.totalDebt()}" }
    } else {
        FinsimLog.engine.i { "Simulación completada con éxito en mes $payoffMonth. Total intereses: ${totals.interest}, total seguros: ${totals.insurance}, total desembolsado: ${totals.cashPaid}" }
    }
    return LoanResult(
        rows = resultRows,
        basePayment = account.basePayment.toFixed(2),
        payoffMonth = payoffMonth,
        payoffDate = payoffDate,
        lastInsuranceMonth = lastInsuranceMonth,
        complete = complete,
        totals = totals,
        remainingDebt = account.totalDebt().toFixed(2),
        unappliedEvents = unappliedEvents
    )
}

/**
 * Deep typed calculation engine seam operating on type-safe value objects.
 */
fun simulateLoan(typedScenario: TypedScenario): TypedLoanResult = TypedEngine.simulate(typedScenario)
