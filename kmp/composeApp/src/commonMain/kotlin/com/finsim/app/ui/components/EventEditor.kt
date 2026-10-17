package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * Vista presentacional para edición de lotes de eventos financieros.
 *
 * Delega la preparación del borrador, el seguimiento de cambios en O(1),
 * la asignación determinista de identificadores y la validación de dominio
 * al módulo profundo [EventDraftBatch].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditor(
    currency: Currency,
    selectedMonth: Int,
    currentEvents: List<LoanEvent>,
    onApplyEvents: (List<LoanEvent>) -> Boolean,
    onPendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var batch by remember(currentEvents) {
        mutableStateOf(EventDraftBatch.from(currentEvents))
    }

    var errorMessage by remember { mutableStateOf("") }

    // Seguimiento O(1) provisto por la interfaz del módulo EventDraftBatch
    val hasPending = batch.isDirty

    LaunchedEffect(hasPending) {
        onPendingChange(hasPending)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Editar meses · Lote de eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Text(
                        text = "Modifica varios meses antes de aplicar. Mes seleccionado: $selectedMonth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = selectedMonth > 0,
                    onClick = {
                        batch = batch.add(month = selectedMonth)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandLime, contentColor = BrandForest)
                ) {
                    Text("Añadir mes", fontWeight = FontWeight.SemiBold)
                }
            }

            if (selectedMonth == 0) Text("M0 es el capital inicial; selecciona un mes desde 1 para añadir eventos.")
            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (batch.isEmpty) {
                Text(
                    text = "No hay eventos programados en este escenario.",
                    color = TextForestMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                batch.items.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAF7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cambio ${index + 1} · Mes ${item.month}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = BrandDark
                                )
                                TextButton(
                                    onClick = {
                                        batch = batch.remove(item.id)
                                    }
                                ) {
                                    Text("Quitar", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Selector de tipo
                                var kindMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1.5f)) {
                                    OutlinedTextField(
                                        value = item.kind.displayName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { kindMenuExpanded = true }
                                    )
                                    DropdownMenu(
                                        expanded = kindMenuExpanded,
                                        onDismissRequest = { kindMenuExpanded = false }
                                    ) {
                                        EventDraftKind.entries.forEach { kindOption ->
                                            DropdownMenuItem(
                                                text = { Text(kindOption.displayName) },
                                                onClick = {
                                                    batch = batch.updateKind(item.id, kindOption)
                                                    kindMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = item.month.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let { m ->
                                            batch = batch.updateMonth(item.id, m)
                                        }
                                    },
                                    label = { Text("Mes") },
                                    modifier = Modifier.weight(0.8f)
                                )

                                if (item.kind != EventDraftKind.Missed) {
                                    OutlinedTextField(
                                        value = item.amount,
                                        onValueChange = { newVal ->
                                            batch = batch.updateAmount(item.id, newVal)
                                        },
                                        label = { Text("Importe ($currency)") },
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }

                            if (item.kind == EventDraftKind.Recurring) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = item.endMonth.toString(),
                                        onValueChange = { newVal ->
                                            newVal.toIntOrNull()?.let { em ->
                                                batch = batch.updateRecurring(item.id, endMonth = em, every = item.every)
                                            }
                                        },
                                        label = { Text("Hasta mes") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = item.every.toString(),
                                        onValueChange = { newVal ->
                                            newVal.toIntOrNull()?.let { ev ->
                                                batch = batch.updateRecurring(item.id, endMonth = item.endMonth, every = ev)
                                            }
                                        },
                                        label = { Text("Cada N meses") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        when (val commitResult = batch.commit()) {
                            is BatchCommitResult.Success -> {
                                val success = onApplyEvents(commitResult.events)
                                if (success) {
                                    errorMessage = ""
                                } else {
                                    errorMessage = "No se pudieron aplicar los eventos. Revisa los valores."
                                }
                            }
                            is BatchCommitResult.Failure -> {
                                errorMessage = commitResult.summaryMessage
                            }
                        }
                    },
                    enabled = hasPending,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White)
                ) {
                    Text("Aplicar cambios")
                }

                OutlinedButton(
                    onClick = {
                        batch = batch.discard()
                        errorMessage = ""
                    },
                    enabled = hasPending,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandForest)
                ) {
                    Text("Descartar cambios")
                }
            }
        }
    }
}
