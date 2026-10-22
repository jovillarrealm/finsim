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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                    text = "PUNTO DE PARTIDA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextForestMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tu préstamo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Surface(
                        color = BrandMintLight,
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KpiMintBorder)
                    ) {
                        Text(
                            text = "${draft.state.currency.name} · tasa fija",
                            color = KpiMintText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
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
                    modifier = Modifier.fillMaxWidth().accessibleField("Nombre del escenario")
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
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).accessibleField("Moneda")
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
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).accessibleField("Origen")
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
                            modifier = fieldModifier.accessibleField("Capital (${draft.state.currency})")
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
                            modifier = fieldModifier.accessibleField("Primer pago (AAAA-MM)")
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
                            modifier = fieldModifier.accessibleField("Plazo (meses)")
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
                            modifier = fieldModifier.accessibleField("Tasa de interés (%)")
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
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).accessibleField("Tipo de tasa")
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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(8.dp),
                                modifier = fieldModifier.accessibleField("Capitalizaciones/año")
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
                                modifier = fieldModifier.accessibleField("Cuota pactada (opcional)")
                            )
                        }
                    }
                )

                if (draft.state.rateKind == RateKind.Nominal) {
                    Spacer(modifier = Modifier.height(10.dp))
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
                        modifier = Modifier.fillMaxWidth().accessibleField("Cuota pactada (opcional)")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Seguros del Préstamo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEGUROS DEL PRÉSTAMO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandForest,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedButton(
                        onClick = {
                            draft = draft.addInsurance()
                            errorMessage = ""
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+ Agregar seguro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandForest)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (draft.state.insurances.isEmpty()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No hay seguros configurados. Haz clic en '+ Agregar seguro' para incluir seguros de vida, incendio o desempleo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextForestMuted,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        draft.state.insurances.forEachIndexed { index, insuranceItem ->
                            InsuranceItemCard(
                                item = insuranceItem,
                                index = index,
                                currency = draft.state.currency,
                                isCompact = isCompact,
                                onUpdate = { updated ->
                                    draft = draft.updateInsurance(updated)
                                    errorMessage = ""
                                },
                                onRemove = {
                                    draft = draft.removeInsurance(insuranceItem.id)
                                    errorMessage = ""
                                }
                            )
                        }
                    }
                }

                if (draft.state.currency == Currency.ARS) {
                    Spacer(modifier = Modifier.height(16.dp))

                // Section 5: Régimen Argentino / Prepago
                Text(
                    text = "PREPAGO Y RÉGIMEN ARGENTINO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Régimen de precancelación argentino",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandDark
                                )
                                Text(
                                    text = "Estima comisión e IVA con las tasas y fechas que configures.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextForestMuted
                                )
                            }
                            Switch(
                                checked = draft.state.argentinaProfile.enabled,
                                onCheckedChange = {
                                    draft = draft.toggleArgentinaProfile(it)
                                    errorMessage = ""
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Régimen de precancelación argentino"
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandForest
                                )
                            )
                        }

                        if (draft.state.argentinaProfile.enabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            AdaptiveFieldRow(
                                isCompact = isCompact,
                                first = { fieldModifier ->
                                    OutlinedTextField(
                                        value = draft.state.argentinaProfile.originationDate,
                                        onValueChange = {
                                            draft = draft.updateArgentinaProfile(originationDate = it)
                                            errorMessage = ""
                                        },
                                        label = { Text("Fecha originación (AAAA-MM)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = fieldModifier.accessibleField("Fecha originación (AAAA-MM)")
                                    )
                                },
                                second = { fieldModifier ->
                                    OutlinedTextField(
                                        value = draft.state.argentinaProfile.originalTermMonthsStr,
                                        onValueChange = {
                                            draft = draft.updateArgentinaProfile(originalTermMonthsStr = it)
                                            errorMessage = ""
                                        },
                                        label = { Text("Plazo original (meses)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = fieldModifier.accessibleField("Plazo original (meses)")
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AdaptiveFieldRow(
                                isCompact = isCompact,
                                first = { fieldModifier ->
                                    OutlinedTextField(
                                        value = draft.state.argentinaProfile.commissionPercentStr,
                                        onValueChange = {
                                            draft = draft.updateArgentinaProfile(commissionPercentStr = it)
                                            errorMessage = ""
                                        },
                                        label = { Text("Comisión prepago (%)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = fieldModifier.accessibleField("Comisión prepago (%)")
                                    )
                                },
                                second = { fieldModifier ->
                                    OutlinedTextField(
                                        value = draft.state.argentinaProfile.vatPercentStr,
                                        onValueChange = {
                                            draft = draft.updateArgentinaProfile(vatPercentStr = it)
                                            errorMessage = ""
                                        },
                                        label = { Text("Alícuota IVA (%)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = fieldModifier.accessibleField("Alícuota IVA (%)")
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = draft.state.argentinaProfile.contractualProfile,
                                onValueChange = {
                                    draft = draft.updateArgentinaProfile(contractualProfile = it)
                                    errorMessage = ""
                                },
                                label = { Text("Perfil contractual (opcional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().accessibleField("Perfil contractual (opcional)")
                            )
                        }
                    }
                }
                }

                if (draft.state.currency != Currency.ARS) {
                    Text(
                        text = "El régimen argentino solo está disponible con moneda ARS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted,
                        modifier = Modifier.padding(top = 16.dp)
                    )
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
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsuranceItemCard(
    item: InsuranceDraftItem,
    index: Int,
    currency: Currency,
    isCompact: Boolean,
    onUpdate: (InsuranceDraftItem) -> Unit,
    onRemove: () -> Unit
) {
    var kindExpanded by remember { mutableStateOf(false) }
    var vigenciaExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandPlumLight,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPlum.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Seguro #${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPlum,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                TextButton(
                    onClick = onRemove,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdaptiveFieldRow(
                isCompact = isCompact,
                first = { fieldModifier ->
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { onUpdate(item.copy(name = it)) },
                        label = { Text("Nombre del seguro") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(8.dp),
                        modifier = fieldModifier.accessibleField("Nombre del seguro")
                    )
                },
                second = { fieldModifier ->
                    ExposedDropdownMenuBox(
                        expanded = kindExpanded,
                        onExpandedChange = { kindExpanded = it },
                        modifier = fieldModifier
                    ) {
                        OutlinedTextField(
                            value = if (item.kind == "percentage") "% del saldo" else "Monto fijo mensual",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Modalidad") },
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).accessibleField("Modalidad")
                        )
                        ExposedDropdownMenu(
                            expanded = kindExpanded,
                            onDismissRequest = { kindExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Monto fijo mensual") },
                                onClick = {
                                    onUpdate(item.copy(kind = "fixed"))
                                    kindExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("% del saldo mensual") },
                                onClick = {
                                    onUpdate(item.copy(kind = "percentage"))
                                    kindExpanded = false
                                }
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AdaptiveFieldRow(
                isCompact = isCompact,
                first = { fieldModifier ->
                    val valueLabel = if (item.kind == "percentage") "Tasa mensual (% saldo)" else "Monto mensual ($currency)"
                    OutlinedTextField(
                        value = item.valueStr,
                        onValueChange = { onUpdate(item.copy(valueStr = it)) },
                        label = { Text(valueLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(8.dp),
                        modifier = fieldModifier.accessibleField(valueLabel)
                    )
                },
                second = { fieldModifier ->
                    ExposedDropdownMenuBox(
                        expanded = vigenciaExpanded,
                        onExpandedChange = { vigenciaExpanded = it },
                        modifier = fieldModifier
                    ) {
                        OutlinedTextField(
                            value = if (item.endsAtPayoff) "Hasta liquidar préstamo" else "Hasta mes específico",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Vigencia") },
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vigenciaExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).accessibleField("Vigencia")
                        )
                        ExposedDropdownMenu(
                            expanded = vigenciaExpanded,
                            onDismissRequest = { vigenciaExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hasta liquidar préstamo") },
                                onClick = {
                                    onUpdate(item.copy(endsAtPayoff = true))
                                    vigenciaExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hasta mes específico") },
                                onClick = {
                                    onUpdate(item.copy(endsAtPayoff = false))
                                    vigenciaExpanded = false
                                }
                            )
                        }
                    }
                }
            )

            if (!item.endsAtPayoff) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = item.endMonthStr,
                    onValueChange = { onUpdate(item.copy(endMonthStr = it)) },
                    label = { Text("Último mes de cobertura (1..1200)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().accessibleField("Último mes de cobertura (1..1200)")
                )
            }
        }
    }
}

private fun Modifier.accessibleField(label: String): Modifier = semantics {
    contentDescription = label
}
