package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Condiciones del préstamo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandDark
            )
            Text(
                text = "Configura el importe, plazo, tasas y amortización base.",
                style = MaterialTheme.typography.bodySmall,
                color = TextForestMuted,
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

            // Section 1: Identificación
            Text(
                text = "DATOS GENERALES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandForest,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del escenario") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        shape = RoundedCornerShape(8.dp),
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
                        shape = RoundedCornerShape(8.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Capital y Fechas
            Text(
                text = "CAPITAL Y PLAZO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandForest,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Capital ($currency)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Primer pago (AAAA-MM)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = termMonths,
                    onValueChange = { termMonths = it },
                    label = { Text("Plazo (meses)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ratePercent,
                    onValueChange = { ratePercent = it },
                    label = { Text("Tasa de interés (%)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Tasas y Cuota
            Text(
                text = "TIPO DE TASA Y CUOTA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BrandForest,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Rate Kind dropdown
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
                        shape = RoundedCornerShape(8.dp),
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = payment,
                        onValueChange = { payment = it },
                        label = { Text("Cuota pactada (opcional)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    try {
                        val rateDecValue = (Decimal.parse(ratePercent) / Decimal.parse("100")).toString()
                        val newRate = when (rateKind) {
                            "monthly" -> Rate.Monthly(rateDecValue)
                            "annual" -> Rate.Annual(rateDecValue)
                            "nominal" -> Rate.Nominal(rateDecValue, nominalFreq.toIntOrNull() ?: 12)
                            else -> error("Tipo de tasa inválido")
                        }
                        val newScenario = scenario.copy(
                            name = name.trim().ifBlank { "Mi préstamo" },
                            currency = currency,
                            source = source,
                            principal = principal.trim(),
                            startDate = startDate.trim(),
                            termMonths = termMonths.trim().toIntOrNull() ?: error("Indica un plazo en meses válido."),
                            rate = newRate,
                            payment = payment.trim().ifBlank { null }
                        )
                        val ok = onApplyScenario(newScenario)
                        if (ok) errorMessage = ""
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Verifica los datos del préstamo."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White)
            ) {
                Text("Actualizar condiciones del préstamo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
