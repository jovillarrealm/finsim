package com.finsim.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import com.finsim.app.theme.SurfaceWarm

@Composable
fun MonthlyTable(
    currency: Currency,
    result: LoanResult,
    selectedMonth: Int = 1,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    scenarioName: String = "Modificado"
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    var page by remember { mutableStateOf(0) }
    val lastPage = ((result.rows.size - 1) / 12).coerceAtLeast(0)
    LaunchedEffect(selectedMonth, result.rows.size) { page = ((selectedMonth - 1).coerceAtLeast(0) / 12).coerceAtMost(lastPage) }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWarm)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Detalle mes a mes · $scenarioName", style = MaterialTheme.typography.titleMedium)
            Text("${result.rows.size} meses · Importes en ${currency.name}. Cada importe permite copiar; Seleccionar lleva al detalle y editor.")
            OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar tabla" else "Ver tabla") }
            if (expanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(enabled = page > 0, onClick = { page-- }) { Text("Tabla anterior") }
                    OutlinedButton(enabled = page < lastPage, onClick = { page++ }) { Text("Tabla siguiente") }
                }
                Text("Tabla M${page * 12 + 1}–M${minOf((page + 1) * 12, result.rows.size)} · Desliza horizontalmente para ver las columnas")
                val headers = listOf("Mes", "Fecha", "Saldo inicial", "Capital habitual", "Capital adicional", "Interés generado", "Seguro generado",
                    "Intereses pagados", "Seguros pagados", "Otros cargos", "Pago total", "Saldo final", "Interés pendiente", "Seguro pendiente", "Capital vencido", "Mora", "Deuda", "No aplicado")
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row { headers.forEach { Text(it, Modifier.width(160.dp).padding(4.dp), style = MaterialTheme.typography.labelLarge) } }
                    result.rows.drop(page * 12).take(12).forEach { row ->
                        val paid = paidComponents(row)
                        Row {
                            Column(Modifier.width(160.dp)) {
                                OutlinedButton(onClick = { onSelectMonth(row.month) }) { Text("Seleccionar M${row.month}${if (row.month == selectedMonth) " ✓" else ""}") }
                                CopyResult("mes $scenarioName mes ${row.month}", "M${row.month}")
                            }
                            CopyResult("fecha $scenarioName mes ${row.month}", formatMonth(row.date), modifier = Modifier.width(160.dp))
                            val values = listOf(row.openingBalance, paid[0], paid[1], row.interestCharged, row.insuranceCharged, paid[2], paid[3], paid[4], row.cashPaid,
                                row.closingBalance, row.pendingInterest, row.pendingInsurance, row.overduePrincipal, row.arrears, row.debt,
                                sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied)))
                            values.forEachIndexed { index, amount -> CopyMoney("tabla ${headers[index + 2]} $scenarioName mes ${row.month}", amount, currency, Modifier.width(160.dp)) }
                        }
                    }
                }
            }
        }
    }
}
