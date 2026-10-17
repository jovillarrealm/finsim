package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult

@OptIn(ExperimentalLayoutApi::class)
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
    var exportMessage by remember { mutableStateOf("") }
    val downloader = rememberScheduleDownloader()

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Detalle mes a mes · $scenarioName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandForest
                    )
                    Text(
                        "${result.rows.size} meses simulados · Importes en ${currency.name}. Desplazamiento continuo en 2D.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandForest
                    )
                ) {
                    Text(if (expanded) "Ocultar tabla" else "Ver tabla")
                }

                if (expanded) {
                    Button(
                        onClick = {
                            val csv = generateScheduleCsv(result, scenarioName)
                            downloader.downloadCsv("cronograma-$scenarioName.csv", csv) { exportMessage = it }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandEmerald,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text("Exportar CSV / Excel")
                    }

                    OutlinedButton(
                        onClick = {
                            downloader.downloadArrow("cronograma-$scenarioName.arrow", result) { exportMessage = it }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandForest
                        )
                    ) {
                        Text("Exportar Apache Arrow (.arrow)")
                    }
                }
            }

            if (exportMessage.isNotEmpty()) {
                Text(
                    exportMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (expanded) {
                Text(
                    "Desplaza horizontalmente para ver todas las columnas y verticalmente para explorar todos los meses:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextForestMuted
                )

                val headers = listOf(
                    "Mes", "Fecha", "Saldo inicial", "Capital habitual", "Capital adicional", "Interés generado", "Seguro generado",
                    "Intereses pagados", "Seguros pagados", "Otros cargos", "Pago total", "Saldo final", "Interés pendiente", "Seguro pendiente", "Capital vencido", "Mora", "Deuda", "No aplicado"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    color = SurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Container with Synchronized Horizontal Scroll and Sticky Header
                    Column(Modifier.horizontalScroll(horizontalScroll)) {
                        // Sticky Header Row
                        Row(
                            Modifier
                                .background(BrandForest.copy(alpha = 0.06f))
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            headers.forEach {
                                Text(
                                    it,
                                    Modifier.width(160.dp).padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandForest
                                )
                            }
                        }

                        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                        // Vertically Scrollable Rows Body
                        Column(
                            Modifier
                                .heightIn(max = 480.dp)
                                .verticalScroll(verticalScroll)
                        ) {
                            result.rows.forEachIndexed { rowIndex, row ->
                                val paid = paidComponents(row)
                                val isSelected = row.month == selectedMonth

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) BrandMintLight else if (rowIndex % 2 == 1) BackgroundClean else androidx.compose.ui.graphics.Color.Transparent)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Month column + Select button
                                    Column(Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                        OutlinedButton(
                                            onClick = { onSelectMonth(row.month) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Seleccionar M${row.month}${if (isSelected) " ✓" else ""}", fontSize = 11.sp)
                                        }
                                        CopyResult("mes $scenarioName mes ${row.month}", "M${row.month}")
                                    }

                                    // Date column
                                    Box(Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                        CopyResult("fecha $scenarioName mes ${row.month}", formatMonth(row.date))
                                    }

                                    // Metric values
                                    val unapplied = sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied))
                                    val values = listOf(
                                        row.openingBalance, paid[0], paid[1], row.interestCharged, row.insuranceCharged,
                                        paid[2], paid[3], paid[4], row.cashPaid, row.closingBalance,
                                        row.pendingInterest, row.pendingInsurance, row.overduePrincipal, row.arrears, row.debt,
                                        unapplied
                                    )

                                    values.forEachIndexed { index, amount ->
                                        Box(Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                            CopyMoney("tabla ${headers[index + 2]} $scenarioName mes ${row.month}", amount, currency)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
