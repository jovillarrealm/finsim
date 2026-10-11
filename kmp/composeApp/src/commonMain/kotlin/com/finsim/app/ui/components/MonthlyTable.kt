package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.finsim.domain.MonthRow

@Composable
fun MonthlyTable(
    currency: Currency,
    result: LoanResult,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detalle mes a mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toca cualquier fila para seleccionar y editar ese mes.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            val scrollState = rememberScrollState()

            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        TableCell("Mes", width = 50.dp, isHeader = true)
                        TableCell("Fecha", width = 90.dp, isHeader = true)
                        TableCell("Saldo inicial", width = 110.dp, isHeader = true)
                        TableCell("Interés", width = 85.dp, isHeader = true)
                        TableCell("Seguro", width = 80.dp, isHeader = true)
                        TableCell("Cap. habitual", width = 95.dp, isHeader = true)
                        TableCell("Cap. extra", width = 90.dp, isHeader = true)
                        TableCell("Saldo final", width = 110.dp, isHeader = true)
                        TableCell("Caja pagada", width = 110.dp, isHeader = true)
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(result.rows, key = { it.month }) { row ->
                            Row(
                                modifier = Modifier
                                    .clickable { onSelectMonth(row.month) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                TableCell(row.month.toString(), width = 50.dp)
                                TableCell(formatMonth(row.date), width = 90.dp)
                                TableCell(formatMoney(row.openingBalance, currency), width = 110.dp)
                                TableCell(formatMoney(row.interestCharged, currency), width = 85.dp)
                                TableCell(formatMoney(row.insuranceCharged, currency), width = 80.dp)
                                TableCell(formatMoney(row.scheduled.principal, currency), width = 95.dp)
                                TableCell(formatMoney(row.extra.principal, currency), width = 90.dp)
                                TableCell(formatMoney(row.closingBalance, currency), width = 110.dp)
                                TableCell(formatMoney(row.cashPaid, currency), width = 110.dp, isHighlight = true)
                            }
                            HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
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
    isHighlight: Boolean = false
) {
    Text(
        text = text,
        fontSize = if (isHeader) 11.sp else 12.sp,
        fontWeight = if (isHeader) FontWeight.Bold else if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isHighlight) BrandBlue else if (isHeader) BrandNavy else Color(0xFF334155),
        modifier = Modifier.width(width).padding(horizontal = 4.dp)
    )
}
