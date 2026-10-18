package com.finsim.app.ui.components

import androidx.compose.runtime.Composable
import com.finsim.domain.LoanResult

fun generateScheduleCsv(result: LoanResult, scenarioName: String, delimiter: Char = ';'): String {
    val sb = StringBuilder()
    sb.append('\uFEFF') // UTF-8 BOM
    val headers = listOf(
        "Mes", "Fecha", "Saldo inicial", "Capital habitual", "Capital adicional",
        "Interés generado", "Seguro generado", "Intereses pagados", "Seguros pagados",
        "Otros cargos", "Pago total", "Saldo final", "Interés pendiente", "Seguro pendiente",
        "Capital vencido", "Mora", "Deuda", "No aplicado"
    )
    sb.append(headers.joinToString(delimiter.toString()))
    sb.append("\r\n")
    result.rows.forEach { row ->
        val paid = paidComponents(row)
        val unapplied = sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied))
        val values = listOf(
            "M${row.month}",
            row.date,
            row.openingBalance.replace('.', ','),
            paid[0].replace('.', ','),
            paid[1].replace('.', ','),
            row.interestCharged.replace('.', ','),
            row.insuranceCharged.replace('.', ','),
            paid[2].replace('.', ','),
            paid[3].replace('.', ','),
            paid[4].replace('.', ','),
            row.cashPaid.replace('.', ','),
            row.closingBalance.replace('.', ','),
            row.pendingInterest.replace('.', ','),
            row.pendingInsurance.replace('.', ','),
            row.overduePrincipal.replace('.', ','),
            row.arrears.replace('.', ','),
            row.debt.replace('.', ','),
            unapplied.replace('.', ',')
        )
        sb.append(values.joinToString(delimiter.toString()))
        sb.append("\r\n")
    }
    return sb.toString()
}

fun serializeScheduleToJson(result: LoanResult): String {
    val items = result.rows.map { row ->
        val paid = paidComponents(row)
        val unapplied = sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied))
        """{"month":${row.month},"date":"${row.date}","openingBalance":"${row.openingBalance}","regularCapital":"${paid[0]}","extraCapital":"${paid[1]}","interestCharged":"${row.interestCharged}","insuranceCharged":"${row.insuranceCharged}","interestPaid":"${paid[2]}","insurancePaid":"${paid[3]}","otherChargesPaid":"${paid[4]}","cashPaid":"${row.cashPaid}","closingBalance":"${row.closingBalance}","pendingInterest":"${row.pendingInterest}","pendingInsurance":"${row.pendingInsurance}","overduePrincipal":"${row.overduePrincipal}","arrears":"${row.arrears}","debt":"${row.debt}","unapplied":"$unapplied"}"""
    }
    return "[${items.joinToString(",")}]"
}

interface ScheduleDownloader {
    fun downloadCsv(fileName: String, csvContent: String, onMessage: (String) -> Unit)
    fun downloadArrow(fileName: String, result: LoanResult, onMessage: (String) -> Unit)
}

@Composable
expect fun rememberScheduleDownloader(): ScheduleDownloader
