package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Detalle mes a mes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Text(
                        text = "Toca cualquier fila para seleccionarla y editar sus eventos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted
                    )
                }
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

            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .border(0.5.dp, BorderSage, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFEDF1E6))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("Mes", width = 54.dp, isHeader = true)
                        TableCell("Fecha", width = 84.dp, isHeader = true)
                        TableCell("Saldo inicial", width = 115.dp, isHeader = true, alignRight = true)
                        TableCell("Capital", width = 100.dp, isHeader = true, alignRight = true)
                        TableCell("Extra / Recup.", width = 110.dp, isHeader = true, alignRight = true)
                        TableCell("Interés", width = 95.dp, isHeader = true, alignRight = true)
                        TableCell("Seguro", width = 85.dp, isHeader = true, alignRight = true)
                        TableCell("Pago total", width = 115.dp, isHeader = true, alignRight = true)
                        TableCell("Saldo final", width = 115.dp, isHeader = true, alignRight = true)
                    }

                    // Virtualized rows
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(result.rows, key = { it.month }) { row ->
                            val isSelected = row.month == selectedMonth
                            val schedP = row.scheduled.principal.toDoubleOrNull() ?: 0.0
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
                                TableCell("M${row.month}", width = 54.dp, isBold = isSelected, color = if (isSelected) BrandForest else BrandDark)
                                TableCell(formatMonth(row.date), width = 84.dp, color = TextForestMuted)
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
