package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import com.finsim.domain.MonthRow
import kotlin.math.max

data class MonthComparisonData(
    val month: Int,
    val date: String,
    val originalRow: MonthRow?,
    val modifiedRow: MonthRow?
)

@Composable
fun HorizontalComparisonChart(
    currency: Currency,
    original: LoanResult,
    modified: LoanResult,
    selectedMonth: Int,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMonths = max(original.rows.size, modified.rows.size)
    val origMap = original.rows.associateBy { it.month }
    val modMap = modified.rows.associateBy { it.month }

    val comparisonData = (1..totalMonths).map { m ->
        val orig = origMap[m]
        val mod = modMap[m]
        MonthComparisonData(
            month = m,
            date = mod?.date ?: orig?.date ?: "Mes $m",
            originalRow = orig,
            modifiedRow = mod
        )
    }

    // Determine max cash ceiling across all months for common scale
    var maxCash = 1.0
    for (d in comparisonData) {
        val cOrig = d.originalRow?.cashPaid?.toDoubleOrNull() ?: 0.0
        val cMod = d.modifiedRow?.cashPaid?.toDoubleOrNull() ?: 0.0
        if (cOrig > maxCash) maxCash = cOrig
        if (cMod > maxCash) maxCash = cMod
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Comparación mensual · Barras horizontales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Escala común en $currency. Selecciona un mes para editar sus eventos.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = BrandBlue, label = "Capital")
                LegendItem(color = BrandTeal, label = "Extra")
                LegendItem(color = BrandOrange, label = "Interés")
                LegendItem(color = BrandPurple, label = "Seguro")
            }

            // Month list with vertical scroll
            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(comparisonData, key = { it.month }) { data ->
                        val isSelected = data.month == selectedMonth
                        MonthRowCard(
                            data = data,
                            currency = currency,
                            ceiling = maxCash,
                            isSelected = isSelected,
                            onSelect = { onSelectMonth(data.month) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text = label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun MonthRowCard(
    data: MonthComparisonData,
    currency: Currency,
    ceiling: Double,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val origCash = data.originalRow?.cashPaid ?: "0.00"
    val modCash = data.modifiedRow?.cashPaid ?: "0.00"
    val description = "Mes ${data.month}: Original $currency $origCash, Modificado $currency $modCash."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrandBlue.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) BrandBlue else BorderLight,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(10.dp)
            .semantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mes ${data.month} · ${formatMonth(data.date)}",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) BrandBlue else BrandNavy
            )
            if (isSelected) {
                Surface(
                    color = BrandBlue,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Seleccionado",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Original Bar
        BarWithLabel(
            label = "Original",
            row = data.originalRow,
            currency = currency,
            ceiling = ceiling
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Modified Bar
        BarWithLabel(
            label = "Modificado",
            row = data.modifiedRow,
            currency = currency,
            ceiling = ceiling
        )
    }
}

@Composable
private fun BarWithLabel(
    label: String,
    row: MonthRow?,
    currency: Currency,
    ceiling: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.width(75.dp)
        )

        if (row != null && row.cashPaid != "0.00") {
            val pSched = row.scheduled.principal.toDoubleOrNull() ?: 0.0
            val pExtra = (row.extra.principal.toDoubleOrNull() ?: 0.0) + (row.catchup.principal.toDoubleOrNull() ?: 0.0)
            val pInt = (row.scheduled.interest.toDoubleOrNull() ?: 0.0) + (row.catchup.interest.toDoubleOrNull() ?: 0.0) + (row.extra.interest.toDoubleOrNull() ?: 0.0)
            val pIns = (row.scheduled.insurance.toDoubleOrNull() ?: 0.0) + (row.catchup.insurance.toDoubleOrNull() ?: 0.0) + (row.extra.insurance.toDoubleOrNull() ?: 0.0)
            val total = pSched + pExtra + pInt + pIns

            val totalFraction = (total / ceiling).coerceIn(0.01, 1.0).toFloat()

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(totalFraction)
                ) {
                    if (pSched > 0) {
                        Box(modifier = Modifier.fillMaxHeight().weight((pSched / total).toFloat()).background(BrandBlue))
                    }
                    if (pExtra > 0) {
                        Box(modifier = Modifier.fillMaxHeight().weight((pExtra / total).toFloat()).background(BrandTeal))
                    }
                    if (pInt > 0) {
                        Box(modifier = Modifier.fillMaxHeight().weight((pInt / total).toFloat()).background(BrandOrange))
                    }
                    if (pIns > 0) {
                        Box(modifier = Modifier.fillMaxHeight().weight((pIns / total).toFloat()).background(BrandPurple))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatMoney(row.cashPaid, currency),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandNavy,
                modifier = Modifier.width(100.dp)
            )
        } else {
            Text(
                text = "Sin pago",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
