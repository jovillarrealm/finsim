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

data class EditableEventItem(
    val id: String,
    var kind: String, // "extra", "recurring", "missed", "catchup"
    var month: Int,
    var endMonth: Int = month,
    var every: Int = 1,
    var amount: String = "100.00"
)

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
    // Draft list of events
    var draftEvents by remember(currentEvents) {
        mutableStateOf(
            currentEvents.map { ev ->
                when (ev) {
                    is LoanEvent.Extra -> EditableEventItem(ev.id, "extra", ev.month, amount = ev.amount)
                    is LoanEvent.Recurring -> EditableEventItem(ev.id, "recurring", ev.month, ev.endMonth, ev.every, ev.amount)
                    is LoanEvent.Missed -> EditableEventItem(ev.id, "missed", ev.month, amount = "0.00")
                    is LoanEvent.Catchup -> EditableEventItem(ev.id, "catchup", ev.month, amount = ev.amount)
                }
            }
        )
    }

    var errorMessage by remember { mutableStateOf("") }

    // Check if draft differs from currentEvents
    val hasPending = remember(draftEvents, currentEvents) {
        val currentMapped = currentEvents.map { ev ->
            when (ev) {
                is LoanEvent.Extra -> EditableEventItem(ev.id, "extra", ev.month, amount = ev.amount)
                is LoanEvent.Recurring -> EditableEventItem(ev.id, "recurring", ev.month, ev.endMonth, ev.every, ev.amount)
                is LoanEvent.Missed -> EditableEventItem(ev.id, "missed", ev.month, amount = "0.00")
                is LoanEvent.Catchup -> EditableEventItem(ev.id, "catchup", ev.month, amount = ev.amount)
            }
        }
        draftEvents != currentMapped
    }

    LaunchedEffect(hasPending) {
        onPendingChange(hasPending)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Editar meses · Lote de eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Modifica varios meses antes de aplicar. Mes seleccionado: $selectedMonth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Button(
                    onClick = {
                        val newId = "ev_${draftEvents.size + 1}_${selectedMonth}"
                        draftEvents = draftEvents + EditableEventItem(
                            id = newId,
                            kind = "extra",
                            month = selectedMonth,
                            endMonth = selectedMonth,
                            amount = "100.00"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Añadir mes")
                }
            }

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

            if (draftEvents.isEmpty()) {
                Text(
                    text = "No hay eventos programados en este escenario.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                draftEvents.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = CardDefaults.outlinedCardBorder()
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
                                    fontSize = 13.sp
                                )
                                TextButton(
                                    onClick = {
                                        draftEvents = draftEvents.filterIndexed { i, _ -> i != index }
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
                                // Kind selector
                                OutlinedTextField(
                                    value = when (item.kind) {
                                        "extra" -> "Abono a capital"
                                        "recurring" -> "Abono recurrente"
                                        "missed" -> "Omitir cuota"
                                        "catchup" -> "Recuperación"
                                        else -> item.kind
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo") },
                                    modifier = Modifier.weight(1.5f)
                                )

                                OutlinedTextField(
                                    value = item.month.toString(),
                                    onValueChange = { newVal ->
                                        newVal.toIntOrNull()?.let { m ->
                                            draftEvents = draftEvents.mapIndexed { i, ev ->
                                                if (i == index) ev.copy(month = m) else ev
                                            }
                                        }
                                    },
                                    label = { Text("Mes") },
                                    modifier = Modifier.weight(0.8f)
                                )

                                if (item.kind != "missed") {
                                    OutlinedTextField(
                                        value = item.amount,
                                        onValueChange = { newVal ->
                                            draftEvents = draftEvents.mapIndexed { i, ev ->
                                                if (i == index) ev.copy(amount = newVal) else ev
                                            }
                                        },
                                        label = { Text("Importe ($currency)") },
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val converted = draftEvents.map { item ->
                                when (item.kind) {
                                    "extra" -> LoanEvent.Extra(item.id, item.month, item.amount)
                                    "recurring" -> LoanEvent.Recurring(item.id, item.month, item.endMonth, item.every, item.amount)
                                    "missed" -> LoanEvent.Missed(item.id, item.month)
                                    "catchup" -> LoanEvent.Catchup(item.id, item.month, item.amount)
                                    else -> error("Tipo desconocido")
                                }
                            }
                            val success = onApplyEvents(converted)
                            if (success) {
                                errorMessage = ""
                            } else {
                                errorMessage = "No se pudieron aplicar los eventos. Revisa los valores."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Error al validar lote de eventos."
                        }
                    },
                    enabled = hasPending,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Aplicar cambios")
                }

                OutlinedButton(
                    onClick = {
                        draftEvents = currentEvents.map { ev ->
                            when (ev) {
                                is LoanEvent.Extra -> EditableEventItem(ev.id, "extra", ev.month, amount = ev.amount)
                                is LoanEvent.Recurring -> EditableEventItem(ev.id, "recurring", ev.month, ev.endMonth, ev.every, ev.amount)
                                is LoanEvent.Missed -> EditableEventItem(ev.id, "missed", ev.month, amount = "0.00")
                                is LoanEvent.Catchup -> EditableEventItem(ev.id, "catchup", ev.month, amount = ev.amount)
                            }
                        }
                        errorMessage = ""
                    },
                    enabled = hasPending
                ) {
                    Text("Descartar cambios")
                }
            }
        }
    }
}
