package com.finsim.app.ui.components

import com.finsim.core.Decimal
import com.finsim.domain.LoanResult
import com.finsim.domain.MonthRow

internal fun sumMoney(values: List<String>) = values.fold(Decimal.ZERO) { sum, value -> sum + Decimal.parse(value) }.toFixed(2)
internal fun difference(a: String, b: String) = (Decimal.parse(a) - Decimal.parse(b)).toFixed(2)

internal fun paidComponents(row: MonthRow): List<String> = listOf(
    Decimal.parse(row.scheduled.principal).toFixed(2),
    sumMoney(listOf(row.catchup.principal, row.extra.principal)),
    sumMoney(listOf(row.scheduled.interest, row.catchup.interest, row.extra.interest)),
    sumMoney(listOf(row.scheduled.insurance, row.catchup.insurance, row.extra.insurance)),
    sumMoney(listOf(row.commissionPaid, row.vatPaid))
)

internal fun resultBalance(result: LoanResult, month: Int): String? =
    if (month == 0) result.rows.firstOrNull()?.openingBalance
    else result.rows.find { it.month == month }?.closingBalance
        ?: if (result.complete && month > (result.rows.lastOrNull()?.month ?: 0)) "0.00" else null

internal fun monthWindow(month: Int, horizon: Int): IntRange {
    val selected = month.coerceIn(0, horizon)
    val start = if (selected <= 12) 0 else ((selected - 1) / 12) * 12 + 1
    return start..minOf(if (start == 0) 12 else start + 11, horizon)
}
