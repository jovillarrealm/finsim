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
import com.finsim.domain.*

/**
 * Adaptador visual presentacional para las condiciones y parámetros del préstamo.
 *
 * El módulo profundo [LoanConditionsDraft] encapsula el análisis de cadenas, la
 * validación rigurosa de invariantes de dominio, la conversión automática de tasas
 * y el seguimiento de suciedad (isDirty). Este composable actúa puramente como
 * adaptador de presentación en el seam visual: enlaza el estado del borrador a
 * los controles de formulario y delega las transiciones de estado a través de
 * la interfaz del módulo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanForm(
    scenario: Scenario,
    onApplyScenario: (Scenario) -> Boolean,
    modifier: Modifier = Modifier
) {
    var draft by remember(scenario) { mutableStateOf(LoanConditionsDraft.from(scenario)) }
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
                value = draft.state.name,
                onValueChange = {
                    draft = draft.updateName(it)
                    errorMessage = ""
                },
                label = { Text("Nombre del escenario") },
                isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.Name),
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
                        value = draft.state.currency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Moneda") },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        Currency.entries.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr.name) },
                                onClick = {
                                    draft = draft.updateCurrency(curr)
                                    errorMessage = ""
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
                        value = draft.state.paymentOrigin.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origen") },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(PaymentOrigin.Original.displayName) },
                            onClick = {
                                draft = draft.updatePaymentOrigin(PaymentOrigin.Original)
                                errorMessage = ""
                                sourceExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(PaymentOrigin.Current.displayName) },
                            onClick = {
                                draft = draft.updatePaymentOrigin(PaymentOrigin.Current)
                                errorMessage = ""
                                sourceExpanded = false
                            }
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
                    value = draft.state.principalStr,
                    onValueChange = {
                        draft = draft.updatePrincipal(it)
                        errorMessage = ""
                    },
                    label = { Text("Capital (${draft.state.currency})") },
                    isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.Principal),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.state.startDate,
                    onValueChange = {
                        draft = draft.updateStartDate(it)
                        errorMessage = ""
                    },
                    label = { Text("Primer pago (AAAA-MM)") },
                    isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.StartDate),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.state.termMonthsStr,
                    onValueChange = {
                        draft = draft.updateTermMonths(it)
                        errorMessage = ""
                    },
                    label = { Text("Plazo (meses)") },
                    isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.TermMonths),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = draft.state.ratePercentageStr,
                    onValueChange = {
                        draft = draft.updateRatePercentage(it)
                        errorMessage = ""
                    },
                    label = { Text("Tasa de interés (%)") },
                    isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.RatePercentage),
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
                        value = draft.state.rateKind.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de tasa") },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = rateExpanded,
                        onDismissRequest = { rateExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(RateKind.Monthly.displayName) },
                            onClick = {
                                draft = draft.updateRateKind(RateKind.Monthly)
                                errorMessage = ""
                                rateExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(RateKind.Annual.displayName) },
                            onClick = {
                                draft = draft.updateRateKind(RateKind.Annual)
                                errorMessage = ""
                                rateExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(RateKind.Nominal.displayName) },
                            onClick = {
                                draft = draft.updateRateKind(RateKind.Nominal)
                                errorMessage = ""
                                rateExpanded = false
                            }
                        )
                    }
                }

                if (draft.state.rateKind == RateKind.Nominal) {
                    OutlinedTextField(
                        value = draft.state.nominalFrequencyStr,
                        onValueChange = {
                            draft = draft.updateNominalFrequency(it)
                            errorMessage = ""
                        },
                        label = { Text("Capitalizaciones/año") },
                        isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.NominalFrequency),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = draft.state.paymentStr,
                        onValueChange = {
                            draft = draft.updatePayment(it)
                            errorMessage = ""
                        },
                        label = { Text("Cuota pactada (opcional)") },
                        isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.Payment),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    draft.validateAndCommit().fold(
                        onSuccess = { newScenario ->
                            val ok = onApplyScenario(newScenario)
                            if (ok) errorMessage = ""
                        },
                        onFailure = { err ->
                            errorMessage = err.message ?: "Verifica los datos del préstamo."
                        }
                    )
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
