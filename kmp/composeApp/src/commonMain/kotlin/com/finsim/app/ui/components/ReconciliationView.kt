package com.finsim.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import com.finsim.reconciliation.ReconcileDiff
import com.finsim.reconciliation.StatementRow
import com.finsim.reconciliation.reconcileRow

@Composable
fun ReconciliationView(
    currency: Currency,
    original: LoanResult,
    modifier: Modifier = Modifier
) {
    var monthStr by remember { mutableStateOf("1") }
    var interestStr by remember { mutableStateOf("") }
    var insuranceStr by remember { mutableStateOf("") }
    var principalStr by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("") }

    var differences by remember { mutableStateOf<List<ReconcileDiff>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparar extracto bancario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Introduce importes de un extracto real y compáralos con el préstamo simulado original.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            if (errorMessage.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = monthStr,
                onValueChange = { monthStr = it },
                label = { Text("Mes a comparar") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = interestStr,
                    onValueChange = { interestStr = it },
                    label = { Text("Interés cobrado ($currency)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = insuranceStr,
                    onValueChange = { insuranceStr = it },
                    label = { Text("Seguro cobrado ($currency)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("Capital pagado ($currency)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Saldo final ($currency)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val m = monthStr.toIntOrNull() ?: error("Indica un mes válido.")
                        val stmt = StatementRow(
                            month = m,
                            interest = interestStr.ifBlank { null },
                            insurance = insuranceStr.ifBlank { null },
                            principal = principalStr.ifBlank { null },
                            balance = balanceStr.ifBlank { null }
                        )
                        differences = reconcileRow(stmt, original.rows)
                        errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Error al comparar fila de extracto."
                        differences = emptyList()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Comparar fila")
            }

            if (differences.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Diferencias detectadas (Extracto − Simulado):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                differences.forEach { diff ->
                    val sign = if (!diff.difference.startsWith("-") && diff.difference != "0.00") "+" else ""
                    val formattedDiff = "$sign${formatMoney(diff.difference, currency)}"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = diff.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "Extracto: ${formatMoney(diff.actual, currency)} · Simulado: ${formatMoney(diff.simulated, currency)}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = formattedDiff,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (diff.difference == "0.00") BrandTeal else BrandOrange
                            )
                        }
                    }
                }
            }
        }
    }
}
