package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
 *
 * Se adapta de forma fluida al factor de forma: en pantallas compactas (< 600dp, móviles),
 * apila los campos en 1 sola columna de ancho completo; en pantallas mayores (>= 600dp),
 * organiza los campos en pares de 2 columnas horizontales.
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
    var currencyExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var rateExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 600.dp

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                AdaptiveFieldRow(
                    isCompact = isCompact,
                    first = { fieldModifier ->
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = it },
                            modifier = fieldModifier
                        ) {
                            OutlinedTextField(
                                value = draft.state.currency.name,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Moneda") },
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                    },
                    second = { fieldModifier ->
                        ExposedDropdownMenuBox(
                            expanded = sourceExpanded,
                            onExpandedChange = { sourceExpanded = it },
                            modifier = fieldModifier
                        ) {
                            OutlinedTextField(
                                value = draft.state.paymentOrigin.displayName,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Origen") },
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                )

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

                AdaptiveFieldRow(
                    isCompact = isCompact,
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = draft.state.principalStr,
                            onValueChange = {
                                draft = draft.updatePrincipal(it)
                                errorMessage = ""
                            },
                            label = { Text("Capital (${draft.state.currency})") },
                            isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.Principal),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(8.dp),
                            modifier = fieldModifier
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = draft.state.startDate,
                            onValueChange = {
                                draft = draft.updateStartDate(it)
                                errorMessage = ""
                            },
                            label = { Text("Primer pago (AAAA-MM)") },
                            isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.StartDate),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(8.dp),
                            modifier = fieldModifier
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AdaptiveFieldRow(
                    isCompact = isCompact,
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = draft.state.termMonthsStr,
                            onValueChange = {
                                draft = draft.updateTermMonths(it)
                                errorMessage = ""
                            },
                            label = { Text("Plazo (meses)") },
                            isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.TermMonths),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(8.dp),
                            modifier = fieldModifier
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = draft.state.ratePercentageStr,
                            onValueChange = {
                                draft = draft.updateRatePercentage(it)
                                errorMessage = ""
                            },
                            label = { Text("Tasa de interés (%)") },
                            isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.RatePercentage),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(8.dp),
                            modifier = fieldModifier
                        )
                    }
                )

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

                AdaptiveFieldRow(
                    isCompact = isCompact,
                    first = { fieldModifier ->
                        ExposedDropdownMenuBox(
                            expanded = rateExpanded,
                            onExpandedChange = { rateExpanded = it },
                            modifier = fieldModifier
                        ) {
                            OutlinedTextField(
                                value = draft.state.rateKind.displayName,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Tipo de tasa") },
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                    },
                    second = { fieldModifier ->
                        if (draft.state.rateKind == RateKind.Nominal) {
                            OutlinedTextField(
                                value = draft.state.nominalFrequencyStr,
                                onValueChange = {
                                    draft = draft.updateNominalFrequency(it)
                                    errorMessage = ""
                                },
                                label = { Text("Capitalizaciones/año") },
                                isError = errorMessage.isNotBlank() && draft.errors.containsKey(LoanField.NominalFrequency),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(8.dp),
                                modifier = fieldModifier
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
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(8.dp),
                                modifier = fieldModifier
                            )
                        }
                    }
                )

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
}

/**
 * Contenedor adaptativo que renderiza campos de formulario en 1 columna vertical (fillMaxWidth)
 * en pantallas compactas (< 600dp, móviles) o en 2 columnas horizontales (weight 1f) en pantallas
 * más amplias (>= 600dp, tablets y escritorios).
 */
@Composable
private fun AdaptiveFieldRow(
    isCompact: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isCompact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
