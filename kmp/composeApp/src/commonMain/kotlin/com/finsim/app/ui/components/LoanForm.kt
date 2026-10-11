package com.finsim.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.core.Decimal
import com.finsim.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanForm(
    scenario: Scenario,
    onApplyScenario: (Scenario) -> Boolean,
    modifier: Modifier = Modifier
) {
    var name by remember(scenario) { mutableStateOf(scenario.name) }
    var currency by remember(scenario) { mutableStateOf(scenario.currency) }
    var source by remember(scenario) { mutableStateOf(scenario.source) }
    var principal by remember(scenario) { mutableStateOf(scenario.principal) }
    var startDate by remember(scenario) { mutableStateOf(scenario.startDate) }
    var termMonths by remember(scenario) { mutableStateOf(scenario.termMonths.toString()) }

    var rateKind by remember(scenario) {
        mutableStateOf(
            when (scenario.rate) {
                is Rate.Monthly -> "monthly"
                is Rate.Annual -> "annual"
                is Rate.Nominal -> "nominal"
            }
        )
    }
    // Percentage display: 0.18 -> 18
    var ratePercent by remember(scenario) {
        val dec = Decimal.parse(scenario.rate.value) * Decimal.parse("100")
        mutableStateOf(dec.toString())
    }
    var nominalFreq by remember(scenario) {
        val r = scenario.rate
        mutableStateOf(if (r is Rate.Nominal) r.frequency.toString() else "12")
    }

    var payment by remember(scenario) { mutableStateOf(scenario.payment ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Condiciones del préstamo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ingresa las características de tu préstamo o saldo actual.",
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del escenario") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Currency dropdown
                var currencyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = currency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moneda") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        Currency.entries.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr.name) },
                                onClick = {
                                    currency = curr
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Source dropdown
                var sourceExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (source == "original") "Préstamo original" else "Saldo actual",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Préstamo original") },
                            onClick = { source = "original"; sourceExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Saldo actual") },
                            onClick = { source = "current"; sourceExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Capital ($currency)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Fecha primer pago (AAAA-MM)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = termMonths,
                    onValueChange = { termMonths = it },
                    label = { Text("Plazo (meses)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ratePercent,
                    onValueChange = { ratePercent = it },
                    label = { Text("Tasa (%)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rate Kind
                var rateExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = rateExpanded,
                    onExpandedChange = { rateExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = when (rateKind) {
                            "monthly" -> "Efectiva mensual (EM)"
                            "annual" -> "Efectiva anual (EA)"
                            else -> "Nominal anual"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de tasa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = rateExpanded,
                        onDismissRequest = { rateExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Efectiva mensual (EM)") }, onClick = { rateKind = "monthly"; rateExpanded = false })
                        DropdownMenuItem(text = { Text("Efectiva anual (EA)") }, onClick = { rateKind = "annual"; rateExpanded = false })
                        DropdownMenuItem(text = { Text("Nominal anual") }, onClick = { rateKind = "nominal"; rateExpanded = false })
                    }
                }

                if (rateKind == "nominal") {
                    OutlinedTextField(
                        value = nominalFreq,
                        onValueChange = { nominalFreq = it },
                        label = { Text("Capitalizaciones/año") },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = payment,
                        onValueChange = { payment = it },
                        label = { Text("Cuota conocida (opcional)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val rateDecValue = (Decimal.parse(ratePercent) / Decimal.parse("100")).toString()
                        val newRate = when (rateKind) {
                            "monthly" -> Rate.Monthly(rateDecValue)
                            "annual" -> Rate.Annual(rateDecValue)
                            "nominal" -> Rate.Nominal(rateDecValue, nominalFreq.toIntOrNull() ?: 12)
                            else -> error("Tipo inválido")
                        }
                        val newScenario = scenario.copy(
                            name = name,
                            currency = currency,
                            source = source,
                            principal = principal,
                            startDate = startDate,
                            termMonths = termMonths.toIntOrNull() ?: 36,
                            rate = newRate,
                            payment = payment.ifBlank { null }
                        )
                        val ok = onApplyScenario(newScenario)
                        if (ok) {
                            errorMessage = ""
                        } else {
                            errorMessage = "Revisa los campos del formulario."
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Datos inválidos en el formulario."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Aplicar cambios al préstamo")
            }
        }
    }
}
