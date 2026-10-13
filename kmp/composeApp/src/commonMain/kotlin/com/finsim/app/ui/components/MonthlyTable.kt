package com.finsim.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import com.finsim.domain.MonthRow

@Composable
fun MonthlyTable(
    currency: Currency,
    result: LoanResult,
    selectedMonth: Int = 1,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with accordion trigger (entire row is touch target)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Detalle mes a mes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Surface(
                        color = BrandSageLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${result.rows.size} meses",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandSage,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = if (isExpanded) BrandForest else BrandSageLight,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExpanded) BrandForest else BorderSage
                    )
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar cronograma ▴" else "Ver cronograma completo ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) Color.White else BrandForest,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isExpanded)
                    "Importes en ${currency.name}. Toca cualquier fila para seleccionarla y editar sus eventos."
                else
                    "Importes en ${currency.name}. Despliega para consultar cargos mensuales y distribución.",
                style = MaterialTheme.typography.bodySmall,
                color = TextForestMuted
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    val scrollState = rememberScrollState()

                    // Horizontal scroll hint bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "9 columnas · Desliza horizontalmente ↔",
                            fontSize = 11.sp,
                            color = TextForestMuted,
                            fontWeight = FontWeight.Medium
                        )
                        if (scrollState.canScrollForward) {
                            Surface(
                                color = BrandSageLight,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Más columnas ➔",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandForest,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Table container: horizontally scrollable, vertical flow in outer scroll
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSage, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                        ) {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFEDF1E6))
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell("Mes", width = 56.dp, isHeader = true)
                                TableCell("Fecha", width = 86.dp, isHeader = true)
                                TableCell("Saldo inicial", width = 115.dp, isHeader = true, alignRight = true)
                                TableCell("Capital", width = 100.dp, isHeader = true, alignRight = true)
                                TableCell("Extra / Recup.", width = 110.dp, isHeader = true, alignRight = true)
                                TableCell("Interés", width = 95.dp, isHeader = true, alignRight = true)
                                TableCell("Seguro", width = 85.dp, isHeader = true, alignRight = true)
                                TableCell("Pago total", width = 115.dp, isHeader = true, alignRight = true)
                                TableCell("Saldo final", width = 115.dp, isHeader = true, alignRight = true)
                            }

                            // Data rows without nested LazyColumn scroll trap
                            result.rows.forEach { row ->
                                val isSelected = row.month == selectedMonth
                                val extraP = (row.extra.principal.toDoubleOrNull() ?: 0.0) + (row.catchup.principal.toDoubleOrNull() ?: 0.0)

                                Row(
                                    modifier = Modifier
                                        .clickable { onSelectMonth(row.month) }
                                        .background(
                                            if (isSelected) BrandSageLight
                                            else if (row.month % 2 == 0) Color(0xFFF7F9EF)
                                            else Color.White
                                        )
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableCell("M${row.month}", width = 56.dp, isBold = isSelected, color = if (isSelected) BrandForest else BrandDark)
                                    TableCell(formatMonth(row.date), width = 86.dp, color = TextForestMuted)
                                    TableCell(formatMoney(row.openingBalance, currency), width = 115.dp, alignRight = true)
                                    TableCell(formatMoney(row.scheduled.principal, currency), width = 100.dp, alignRight = true, color = BrandForest)
                                    TableCell(
                                        if (extraP > 0) formatMoney(row.extra.principal, currency) else "—",
                                        width = 110.dp,
                                        alignRight = true,
                                        color = if (extraP > 0) BrandLimeDark else TextForestMuted
                                    )
                                    TableCell(formatMoney(row.interestCharged, currency), width = 95.dp, alignRight = true, color = BrandAmber)
                                    TableCell(formatMoney(row.insuranceCharged, currency), width = 85.dp, alignRight = true, color = BrandPlum)
                                    TableCell(formatMoney(row.cashPaid, currency), width = 115.dp, alignRight = true, isBold = true)
                                    TableCell(formatMoney(row.closingBalance, currency), width = 115.dp, alignRight = true, isBold = isSelected)
                                }
                            }
                        }

                        // Subtle edge shadows for horizontal scroll affordance
                        if (scrollState.canScrollForward) {
                            Box(
                                modifier = Modifier.matchParentSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(18.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color(0x1F173F32))
                                            )
                                        )
                                )
                            }
                        }

                        if (scrollState.canScrollBackward) {
                            Box(
                                modifier = Modifier.matchParentSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(14.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0x1F173F32), Color.Transparent)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    alignRight: Boolean = false,
    color: Color = BrandDark
) {
    Text(
        text = text,
        fontSize = if (isHeader) 11.sp else 12.sp,
        fontWeight = if (isHeader) FontWeight.Bold else if (isBold) FontWeight.Bold else FontWeight.Normal,
        color = if (isHeader) Color(0xFF4D5E4C) else color,
        textAlign = if (alignRight) TextAlign.End else TextAlign.Start,
        modifier = Modifier.width(width).padding(horizontal = 4.dp)
    )
}
