package com.finsim.reconciliation

import com.finsim.core.Decimal
import com.finsim.core.FinsimLog
import com.finsim.domain.MonthRow

data class StatementRow(
    val month: Int,
    val interest: String? = null,
    val insurance: String? = null,
    val principal: String? = null,
    val balance: String? = null
) {
    fun validate() {
        require(interest != null || insurance != null || principal != null || balance != null) {
            "Introduce al menos un importe del extracto."
        }
    }
}

data class ReconcileDiff(
    val field: String,
    val label: String,
    val actual: String,
    val simulated: String,
    val difference: String
)

val fieldLabels = mapOf(
    "interest" to "Interés cobrado",
    "insurance" to "Seguro cobrado",
    "principal" to "Capital pagado",
    "balance" to "Saldo final"
)

fun reconcileRow(input: StatementRow, rows: List<MonthRow>): List<ReconcileDiff> {
    input.validate()
    val row = rows.find { it.month == input.month }
        ?: error("Ese mes no existe en el cronograma seleccionado.")

    val schedP = Decimal.parse(row.scheduled.principal)
    val catchP = Decimal.parse(row.catchup.principal)
    val extraP = Decimal.parse(row.extra.principal)
    val totalPrincipal = (schedP + catchP + extraP).toFixed(2)

    val simulated = mapOf(
        "interest" to row.interestCharged,
        "insurance" to row.insuranceCharged,
        "principal" to totalPrincipal,
        "balance" to row.closingBalance
    )

    val fields = listOf("interest", "insurance", "principal", "balance")
    val diffs = fields.mapNotNull { field ->
        val actual = when (field) {
            "interest" -> input.interest
            "insurance" -> input.insurance
            "principal" -> input.principal
            "balance" -> input.balance
            else -> null
        }
        if (actual != null) {
            val actDec = Decimal.parse(actual)
            val simDec = Decimal.parse(simulated[field]!!)
            val diff = (actDec - simDec).toFixed(2)
            ReconcileDiff(
                field = field,
                label = fieldLabels[field] ?: field,
                actual = actual,
                simulated = simulated[field]!!,
                difference = diff
            )
        } else null
    }
    val nonZeroDiffs = diffs.filter { it.difference != "0.00" }
    if (nonZeroDiffs.isNotEmpty()) {
        FinsimLog.reconcile.w { "Mes ${input.month}: se detectaron ${nonZeroDiffs.size} discrepancia(s) entre el extracto y la simulación: ${nonZeroDiffs.map { "${it.label}=${it.difference}" }}" }
    } else {
        FinsimLog.reconcile.i { "Mes ${input.month}: conciliación exacta sin discrepancias con el extracto bancario." }
    }
    return diffs
}
