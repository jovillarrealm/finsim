package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
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
    var viewMode by remember { mutableStateOf(if (initialExpanded) "table" else "milestones") } // "milestones" or "table"
    var exportMessage by remember { mutableStateOf("") }
    val downloader = rememberScheduleDownloader()

    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    // Filtrado Curado de Hitos Decisivos (Ticket 04 / T10)
    val rows = result.rows
    val prepaymentMonths = remember(rows) {
        rows.filter {
            val ext = it.extra.principal.toDoubleOrNull() ?: 0.0
            val cat = it.catchup.principal.toDoubleOrNull() ?: 0.0
            ext + cat > 0.0
        }.map { it.month }.toSet()
    }

    val milestoneRows = remember(rows, prepaymentMonths, result.payoffMonth) {
        val set = mutableSetOf<Int>()
        if (rows.isNotEmpty()) {
            set.add(1) // Mes 01: Inicio
            for (m in prepaymentMonths) {
                if (m > 1) set.add(m - 1) // Mes previo al abono
                set.add(m) // Mes de abono extraordinario
            }
            if (result.payoffMonth != null) {
                set.add(result.payoffMonth!!)
            } else {
                set.add(rows.last().month)
            }
            if (prepaymentMonths.isEmpty() && rows.size > 2) {
                set.add(rows.size / 2)
            }
        }
        rows.filter { it.month in set }
    }

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
                        "Cronograma Clave · $scenarioName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandForest
                    )
                    Text(
                        "${result.rows.size} meses simulados · Importes en ${currency.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted
                    )
                }
            }

            // Segmented View Toggle: Hitos Clave vs Tabla Completa
            Surface(
                color = Color(0xFFEBEFE5),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSage),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val viewOptions = listOf(
                        "milestones" to "Hitos Clave (${milestoneRows.size})",
                        "table" to "Tabla Completa (${result.rows.size})"
                    )
                    viewOptions.forEach { (modeKey, modeTitle) ->
                        val isSelected = viewMode == modeKey
                        Surface(
                            color = if (isSelected) SurfaceCard else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewMode = modeKey }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandForest else TextForestMuted
                                )
                            }
                        }
                    }
                }
            }

            // Export Actions Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewMode = if (viewMode == "table") "milestones" else "table" },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandForest
                    )
                ) {
                    Text(if (viewMode == "table") "Ocultar tabla" else "Ver tabla")
                }

                Button(
                    onClick = {
                        val csv = generateScheduleCsv(result, scenarioName)
                        downloader.downloadCsv("cronograma-$scenarioName.csv", csv) { exportMessage = it }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Text("Exportar CSV / Excel")
                }

                OutlinedButton(
                    onClick = {
                        downloader.downloadArrow("cronograma-$scenarioName.arrow", result) { exportMessage = it }
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandEmerald),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandForest
                    )
                ) {
                    Text("Exportar Apache Arrow (.arrow)")
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

            // VISTA 1: Hitos Clave Curados
            if (viewMode == "milestones") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    milestoneRows.forEach { row ->
                        val isPayoff = result.payoffMonth == row.month || row.closingBalance == "0.00"
                        val isExtra = (row.extra.principal.toDoubleOrNull() ?: 0.0) + (row.catchup.principal.toDoubleOrNull() ?: 0.0) > 0.0
                        val isSelected = row.month == selectedMonth

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMonth(row.month) },
                            color = if (isPayoff) Color(0xFFF0FDF4) else if (isExtra) Color(0xFFF0FDF4) else if (isSelected) BrandMintLight else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isPayoff) Color(0xFF86EFAC) else if (isExtra) KpiMintBorder else if (isSelected) BrandEmerald else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Milestone Icon Badge
                                Surface(
                                    color = if (isPayoff) BrandEmerald else if (isExtra) BrandChartreuse else BrandSageLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isPayoff) "✓" else if (isExtra) "⚡" else "M${row.month.toString().padStart(2, '0')}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPayoff) Color.White else if (isExtra) BrandDark else BrandSage
                                        )
                                    }
                                }

                                // Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Mes ${row.month.toString().padStart(2, '0')} · ${formatMonth(row.date)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandDark
                                        )
                                        if (isExtra) {
                                            Surface(
                                                color = BrandSageLight,
                                                shape = RoundedCornerShape(999.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, KpiMintBorder)
                                            ) {
                                                Text(
                                                    text = "+${formatMoney(row.extra.principal, currency)} Extra",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandSage,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isPayoff) {
                                            Surface(
                                                color = BrandEmerald,
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Text(
                                                    text = "Fin de Crédito",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isPayoff) {
                                            "Deuda extinguida al 100%"
                                        } else if (isExtra) {
                                            "Salto directo de amortización a capital"
                                        } else {
                                            "Cuota: ${formatMoney(row.cashPaid, currency)} · Cap ${formatMoney(row.scheduled.principal, currency)} | Int ${formatMoney(row.interestCharged, currency)}"
                                        },
                                        fontSize = 11.sp,
                                        color = TextForestMuted,
                                        maxLines = 1
                                    )
                                }

                                // Metrics Column (Right-aligned)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isPayoff) "SALDO $0,00" else formatMoney(row.cashPaid, currency),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPayoff) BrandEmerald else if (isExtra) BrandSage else BrandDark
                                    )
                                    Text(
                                        text = if (isPayoff) "100% Amortizado" else "Saldo: ${formatMoney(row.closingBalance, currency)}",
                                        fontSize = 10.sp,
                                        color = TextForestMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // VISTA 2: Tabla Completa Exhaustiva en 2D
            if (viewMode == "table") {
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
                    Column(Modifier.horizontalScroll(horizontalScroll)) {
                        Row(
                            Modifier
                                .background(TableHeaderBg)
                                .padding(vertical = 10.dp),
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
                                        .background(if (isSelected) Color(0xFFD1FAE5) else if (rowIndex % 2 == 1) TableZebraRow else Color.White)
                                        .then(if (isSelected) Modifier.border(1.dp, BrandEmerald, RoundedCornerShape(4.dp)) else Modifier)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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

                                    Box(Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                        CopyResult("fecha $scenarioName mes ${row.month}", formatMonth(row.date))
                                    }

                                    val unapplied = sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied))
                                    val values = listOf(
                                        row.openingBalance, paid[0], paid[1], row.interestCharged, row.insuranceCharged,
                                        paid[2], paid[3], paid[4], row.cashPaid, row.closingBalance,
                                        row.pendingInterest, row.pendingInsurance, row.overduePrincipal, row.arrears, row.debt, unapplied
                                    )
                                    val labels = listOf(
                                        "saldo inicial", "Capital habitual", "Capital adicional", "interés generado", "seguro generado",
                                        "intereses pagados", "seguros pagados", "otros cargos", "pago total", "saldo de capital",
                                        "interés pendiente", "seguro pendiente", "capital vencido", "mora", "deuda", "excedente no aplicado"
                                    )

                                    values.forEachIndexed { colIndex, amount ->
                                        Box(Modifier.width(160.dp).padding(horizontal = 4.dp)) {
                                            CopyMoney("tabla ${labels[colIndex]} $scenarioName mes ${row.month}", amount, currency)
                                        }
                                    }
                                }
                                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
