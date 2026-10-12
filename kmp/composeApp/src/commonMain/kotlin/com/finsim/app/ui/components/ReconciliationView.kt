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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Comparar extracto bancario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandNavy
            )
            Text(
                text = "Compara los importes reales cobrados por tu banco contra la amortización simulada.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            if (errorMessage.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = monthStr,
                onValueChange = { monthStr = it },
                label = { Text("Número de mes del extracto") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = interestStr,
                    onValueChange = { interestStr = it },
                    label = { Text("Interés cobrado ($currency)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = insuranceStr,
                    onValueChange = { insuranceStr = it },
                    label = { Text("Seguro cobrado ($currency)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = principalStr,
                    onValueChange = { principalStr = it },
                    label = { Text("Capital pagado ($currency)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Saldo final ($currency)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Comparar con cronograma simulado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (differences.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Diferencias detectadas (Extracto − Simulado):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                differences.forEach { diff ->
                    val sign = if (!diff.difference.startsWith("-") && diff.difference != "0.00") "+" else ""
                    val formattedDiff = "$sign${formatMoney(diff.difference, currency)}"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = diff.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
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
