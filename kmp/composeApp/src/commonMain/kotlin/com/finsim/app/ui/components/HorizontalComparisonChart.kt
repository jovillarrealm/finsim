package com.finsim.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import com.finsim.domain.MonthRow
import kotlin.math.max
import kotlin.math.roundToInt

data class MonthComparisonData(
    val month: Int,
    val date: String,
    val originalRow: MonthRow?,
    val modifiedRow: MonthRow?
)

private fun signedMoney(diffStr: String, currency: Currency): String {
    val dec = Decimal.parse(diffStr)
    val sign = if (dec.isPositive) "+" else ""
    return "$sign${formatMoney(diffStr, currency)}"
}

@Composable
fun HorizontalComparisonChart(
    currency: Currency,
    original: LoanResult,
    modified: LoanResult,
    selectedMonth: Int,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // viewMode: "curve" (Opción 3) or "dual_track" (Opción 1)
    var viewMode by remember { mutableStateOf("curve") }

    val totalMonths = max(original.rows.size, modified.rows.size).coerceAtLeast(1)
    val origMap = remember(original) { original.rows.associateBy { it.month } }
    val modMap = remember(modified) { modified.rows.associateBy { it.month } }

    val comparisonData = remember(original, modified, totalMonths) {
        (1..totalMonths).map { m ->
            val orig = origMap[m]
            val mod = modMap[m]
            MonthComparisonData(
                month = m,
                date = mod?.date ?: orig?.date ?: "Mes $m",
                originalRow = orig,
                modifiedRow = mod
            )
        }
    }

    // Cash ceiling for dual-track scale
    var maxCash = 1.0
    for (d in comparisonData) {
        val cOrig = d.originalRow?.cashPaid?.toDoubleOrNull() ?: 0.0
        val cMod = d.modifiedRow?.cashPaid?.toDoubleOrNull() ?: 0.0
        if (cOrig > maxCash) maxCash = cOrig
        if (cMod > maxCash) maxCash = cMod
    }

    // Macro deltas (modificado - original)
    val interestDiff = (Decimal.parse(modified.totals.interest) - Decimal.parse(original.totals.interest)).toFixed(2)
    val insuranceDiff = (Decimal.parse(modified.totals.insurance) - Decimal.parse(original.totals.insurance)).toFixed(2)
    val cashDiff = (Decimal.parse(modified.totals.cashPaid) - Decimal.parse(original.totals.cashPaid)).toFixed(2)
    val origPayoff = original.payoffMonth
    val modPayoff = modified.payoffMonth
    val deltaMonths = if (origPayoff != null && modPayoff != null) {
        modPayoff - origPayoff
    } else null
    val complete = original.complete && modified.complete

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with View Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Original y modificado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Text(
                        text = "Diferencias = modificado − original. Los abonos reducen plazo conservando cuota.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle between Option 3 (Curva) and Option 1 (Doble pista)
                Surface(
                    color = Color(0xFFEBEFE5),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSage)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        FilterChip(
                            selected = viewMode == "curve",
                            onClick = { viewMode = "curve" },
                            label = { Text("Curva de saldo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandForest,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        FilterChip(
                            selected = viewMode == "dual_track",
                            onClick = { viewMode = "dual_track" },
                            label = { Text("Doble pista", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandForest,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Macro Delta Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeltaMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Intereses",
                    value = signedMoney(interestDiff, currency),
                    isFavorable = Decimal.parse(interestDiff).isNegative
                )
                DeltaMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Seguros",
                    value = signedMoney(insuranceDiff, currency),
                    isFavorable = Decimal.parse(insuranceDiff).isNegative
                )
                DeltaMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Caja total",
                    value = signedMoney(cashDiff, currency),
                    isFavorable = Decimal.parse(cashDiff).isNegative
                )
                DeltaMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Plazo",
                    value = if (complete && deltaMonths != null) {
                        val sign = if (deltaMonths > 0) "+" else ""
                        "$sign$deltaMonths meses"
                    } else "Incompleto",
                    isFavorable = (deltaMonths ?: 0) < 0
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payoff dates status
            Text(
                text = "Liquidación original: ${original.payoffDate?.let { formatMonth(it) } ?: "No alcanzada"} · Modificado: ${modified.payoffDate?.let { formatMonth(it) } ?: "No alcanzada"}",
                fontSize = 12.sp,
                color = BrandDark,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // View 1 (Opción 3: Curva de saldo superpuesta) vs View 2 (Opción 1: Doble pista continua)
            if (viewMode == "curve") {
                AmortizationCurveView(
                    currency = currency,
                    original = original,
                    modified = modified,
                    totalMonths = totalMonths,
                    selectedMonth = selectedMonth,
                    onSelectMonth = onSelectMonth
                )
            } else {
                DualTrackTimelineView(
                    currency = currency,
                    data = comparisonData,
                    ceiling = maxCash,
                    selectedMonth = selectedMonth,
                    onSelectMonth = onSelectMonth
                )
            }
        }
    }
}

@Composable
private fun DeltaMetricCard(
    label: String,
    value: String,
    isFavorable: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceHighlight,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, fontSize = 10.sp, color = TextForestMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFavorable) BrandSage else if (value.startsWith("+")) BrandAmber else BrandDark
            )
        }
    }
}

/**
 * Opción 3: Gráfico de Amortización Unificado con Superposición y Línea de Saldo
 * Canvas interactivo con scrubber táctil para deslizar y comparar el saldo mes a mes.
 */
@Composable
private fun AmortizationCurveView(
    currency: Currency,
    original: LoanResult,
    modified: LoanResult,
    totalMonths: Int,
    selectedMonth: Int,
    onSelectMonth: (Int) -> Unit
) {
    // Initial max principal balance
    val origStart = original.rows.firstOrNull()?.openingBalance?.toDoubleOrNull() ?: 1.0
    val modStart = modified.rows.firstOrNull()?.openingBalance?.toDoubleOrNull() ?: 1.0
    val maxPrincipal = max(origStart, modStart).coerceAtLeast(1.0)

    val origBalances = remember(original, totalMonths) {
        val map = original.rows.associate { it.month to (it.closingBalance.toDoubleOrNull() ?: 0.0) }
        (1..totalMonths).map { map[it] ?: 0.0 }
    }

    val modBalances = remember(modified, totalMonths) {
        val map = modified.rows.associate { it.month to (it.closingBalance.toDoubleOrNull() ?: 0.0) }
        (1..totalMonths).map { map[it] ?: 0.0 }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(BrandForest, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Saldo Original", fontSize = 11.sp, color = BrandDark, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(BrandLimeDark, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Saldo Modificado", fontSize = 11.sp, color = BrandDark, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(BrandLime, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Capital Ahorrado", fontSize = 11.sp, color = TextForestMuted)
            }
        }

        // Canvas for curves
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFFFAFAF5), RoundedCornerShape(8.dp))
                .border(0.5.dp, BorderSage, RoundedCornerShape(8.dp))
                .pointerInput(totalMonths) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val m = 1 + (fraction * (totalMonths - 1)).roundToInt()
                        onSelectMonth(m.coerceIn(1, totalMonths))
                    }
                }
                .pointerInput(totalMonths) {
                    detectDragGestures { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val m = 1 + (fraction * (totalMonths - 1)).roundToInt()
                        onSelectMonth(m.coerceIn(1, totalMonths))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                val w = size.width
                val h = size.height

                // Draw 4 horizontal grid lines (25%, 50%, 75%, 100%)
                val gridColor = Color(0xFFE5ECE0)
                for (step in 1..3) {
                    val y = h * (step / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (totalMonths <= 1) return@Canvas

                val xForMonth = { m: Int ->
                    ((m - 1).toFloat() / (totalMonths - 1).toFloat()) * w
                }
                val yForBalance = { bal: Double ->
                    val frac = (bal / maxPrincipal).coerceIn(0.0, 1.0).toFloat()
                    h * (1f - frac)
                }

                // Shaded Area between Original and Modified (Debt reduction visual)
                val areaPath = Path()
                areaPath.moveTo(xForMonth(1), yForBalance(origBalances[0]))
                for (m in 2..totalMonths) {
                    areaPath.lineTo(xForMonth(m), yForBalance(origBalances[m - 1]))
                }
                for (m in totalMonths downTo 1) {
                    areaPath.lineTo(xForMonth(m), yForBalance(modBalances[m - 1]))
                }
                areaPath.close()
                drawPath(areaPath, color = BrandLime.copy(alpha = 0.35f))

                // Original Curve (BrandForest)
                val origPath = Path()
                origPath.moveTo(xForMonth(1), yForBalance(origBalances[0]))
                for (m in 2..totalMonths) {
                    origPath.lineTo(xForMonth(m), yForBalance(origBalances[m - 1]))
                }
                drawPath(
                    path = origPath,
                    color = BrandForest,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Modified Curve (BrandLimeDark)
                val modPath = Path()
                modPath.moveTo(xForMonth(1), yForBalance(modBalances[0]))
                for (m in 2..totalMonths) {
                    modPath.lineTo(xForMonth(m), yForBalance(modBalances[m - 1]))
                }
                drawPath(
                    path = modPath,
                    color = BrandLimeDark,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Payoff dot on modified curve if reached
                val modPayoff = modified.payoffMonth
                if (modPayoff != null && modPayoff in 1..totalMonths) {
                    val px = xForMonth(modPayoff)
                    val py = yForBalance(0.0)
                    drawCircle(color = BrandLimeDark, radius = 6f, center = Offset(px, py))
                    drawCircle(color = Color.White, radius = 3f, center = Offset(px, py))
                }

                // Selected month scrubber vertical line
                val selX = xForMonth(selectedMonth.coerceIn(1, totalMonths))
                drawLine(
                    color = BrandDark,
                    start = Offset(selX, 0f),
                    end = Offset(selX, h),
                    strokeWidth = 2f
                )
                val selModY = yForBalance(modBalances[(selectedMonth - 1).coerceIn(0, totalMonths - 1)])
                drawCircle(color = BrandDark, radius = 5f, center = Offset(selX, selModY))
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(selX, selModY))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Inspection Info (Scrubber read-out)
        val selOrigRow = original.rows.find { it.month == selectedMonth }
        val selModRow = modified.rows.find { it.month == selectedMonth }
        val origBal = selOrigRow?.closingBalance ?: "0.00"
        val modBal = selModRow?.closingBalance ?: "0.00"
        val balSaved = (Decimal.parse(origBal) - Decimal.parse(modBal)).toFixed(2)

        Surface(
            color = SurfaceHighlight,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mes seleccionado: $selectedMonth ${selModRow?.date?.let { "(${formatMonth(it)})" } ?: ""}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandDark
                    )
                    Surface(
                        color = if (Decimal.parse(balSaved).isPositive) BrandSageLight else Color(0xFFEBEFE5),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (Decimal.parse(balSaved).isPositive) Color(0xFFC7D9BA) else BorderSage)
                    ) {
                        Text(
                            text = "Deuda evitada: ${formatMoney(balSaved, currency)}",
                            color = if (Decimal.parse(balSaved).isPositive) BrandSage else TextForestMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Saldo restante: Original ${formatMoney(origBal, currency)} · Modificado ${formatMoney(modBal, currency)}",
                    fontSize = 12.sp,
                    color = TextForestMuted
                )
            }
        }
    }
}

/**
 * Opción 1: Lienzo de Doble Pista Continuo (Dual-Track Timeline)
 * Lista continua de alta densidad sin tarjetas repetitivas.
 */
@Composable
private fun DualTrackTimelineView(
    currency: Currency,
    data: List<MonthComparisonData>,
    ceiling: Double,
    selectedMonth: Int,
    onSelectMonth: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = BrandForest, label = "Capital")
            LegendItem(color = BrandLimeDark, label = "Extra")
            LegendItem(color = BrandAmber, label = "Interés")
            LegendItem(color = BrandPlum, label = "Seguro")
        }

        // Table Header
        Surface(
            color = Color(0xFFEDF1E6),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4D5E4C),
                    modifier = Modifier.width(64.dp)
                )
                Text(
                    text = "Original (Habitual)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modificado (Con eventos)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandLimeDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Continuous Timeline Scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .border(0.5.dp, BorderSage, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(data, key = { it.month }) { item ->
                    val isSelected = item.month == selectedMonth
                    DualTrackRow(
                        item = item,
                        currency = currency,
                        ceiling = ceiling,
                        isSelected = isSelected,
                        onClick = { onSelectMonth(item.month) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DualTrackRow(
    item: MonthComparisonData,
    currency: Currency,
    ceiling: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val origCash = item.originalRow?.cashPaid ?: "0.00"
    val modCash = item.modifiedRow?.cashPaid ?: "0.00"
    val isPaidOffInMod = item.modifiedRow == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) BrandSageLight else if (item.month % 2 == 0) Color(0xFFF7F9EF) else Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month label
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = "M${item.month}",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BrandForest else BrandDark
            )
            Text(
                text = formatMonth(item.date).split(" ").firstOrNull() ?: "",
                fontSize = 10.sp,
                color = TextForestMuted
            )
        }

        // Original track bar
        Box(modifier = Modifier.weight(1f)) {
            if (item.originalRow != null) {
                MiniStackedBar(
                    row = item.originalRow,
                    ceiling = ceiling,
                    cashStr = origCash,
                    currency = currency
                )
            } else {
                Text("—", fontSize = 11.sp, color = TextForestMuted)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Modified track bar
        Box(modifier = Modifier.weight(1f)) {
            if (!isPaidOffInMod && item.modifiedRow != null) {
                MiniStackedBar(
                    row = item.modifiedRow,
                    ceiling = ceiling,
                    cashStr = modCash,
                    currency = currency
                )
            } else {
                Surface(
                    color = BrandSageLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Liquidado",
                        color = BrandSage,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStackedBar(
    row: MonthRow,
    ceiling: Double,
    cashStr: String,
    currency: Currency
) {
    val scheduledP = row.scheduled.principal.toDoubleOrNull() ?: 0.0
    val extraP = (row.extra.principal.toDoubleOrNull() ?: 0.0) + (row.catchup.principal.toDoubleOrNull() ?: 0.0)
    val interest = (row.scheduled.interest.toDoubleOrNull() ?: 0.0) +
            (row.catchup.interest.toDoubleOrNull() ?: 0.0) +
            (row.extra.interest.toDoubleOrNull() ?: 0.0)
    val insurance = (row.scheduled.insurance.toDoubleOrNull() ?: 0.0) +
            (row.catchup.insurance.toDoubleOrNull() ?: 0.0) +
            (row.extra.insurance.toDoubleOrNull() ?: 0.0)

    val safeCeiling = if (ceiling > 0.0) ceiling else 1.0
    val totalCash = scheduledP + extraP + interest + insurance
    val barFraction = (totalCash / safeCeiling).coerceIn(0.0, 1.0).toFloat()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth(barFraction)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE5EAE1))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val sum = totalCash.coerceAtLeast(0.001)
                val fP = (scheduledP / sum).toFloat()
                val fE = (extraP / sum).toFloat()
                val fInt = (interest / sum).toFloat()
                val fIns = (insurance / sum).toFloat()

                if (fP > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fP).background(BrandForest))
                if (fE > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fE).background(BrandLimeDark))
                if (fInt > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fInt).background(BrandAmber))
                if (fIns > 0f) Box(modifier = Modifier.fillMaxHeight().weight(fIns).background(BrandPlum))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatMoney(cashStr, currency),
            fontSize = 9.sp,
            color = TextForestMuted,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = TextForestMuted)
    }
}
