package com.finsim.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import kotlin.math.roundToInt

private val componentLabels = listOf("Capital habitual", "Capital adicional", "Intereses pagados", "Seguros pagados", "Otros cargos")
private val componentColors = listOf(BrandForest, BrandEmerald, BrandAmber, BrandPlum, Color(0xFF0284C7))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HorizontalComparisonChart(
    currency: Currency,
    original: LoanResult,
    modified: LoanResult,
    hiddenComponents: Set<Int>,
    hiddenScenarios: Set<Int>,
    onHiddenComponentsChange: (Set<Int>) -> Unit,
    onHiddenScenariosChange: (Set<Int>) -> Unit,
    selectedMonth: Int,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val results = listOf(original, modified)
    val names = listOf("Original", "Modificado")
    val horizon = maxOf(original.rows.lastOrNull()?.month ?: 0, modified.rows.lastOrNull()?.month ?: 0)
    val month = selectedMonth.coerceIn(0, horizon)
    LaunchedEffect(horizon, selectedMonth) { if (month != selectedMonth) onSelectMonth(month) }
    val components = remember(original, modified) { results.map { result -> result.rows.associate { it.month to paidComponents(it) } } }
    val available = remember(components) { (0..4).filter { c -> components.any { rows -> rows.values.any { it[c] != "0.00" } } }.toSet() }
    LaunchedEffect(available) { onHiddenComponentsChange(hiddenComponents.intersect(available)) }
    val visibleComponents = available - hiddenComponents
    val range = monthWindow(month, horizon)
    var monthInput by remember(month) { mutableStateOf(month.toString()) }
    fun select(value: Int) { onSelectMonth(value.coerceIn(0, horizon)) }

    var showAdvancedDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header & Differences
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Original y modificado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest
                )
                Text(
                    "Original: línea discontinua · Modificado: línea continua. Diferencias = modificado − original.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextForestMuted
                )
                if (!original.complete || !modified.complete) {
                    Text(
                        "Resultado incompleto: diferencias parciales; sin ahorro ni liquidación definitivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandAmberText
                    )
                }
            }

            // Summary Differences Row with Vibrant Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Intereses" to (original.totals.interest to modified.totals.interest),
                    "Seguros" to (original.totals.insurance to modified.totals.insurance),
                    "Caja total" to (original.totals.cashPaid to modified.totals.cashPaid)
                ).forEach { (label, amounts) ->
                    Column(
                        Modifier
                            .background(BrandMintLight, RoundedCornerShape(8.dp))
                            .border(1.dp, KpiMintBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Diferencia $label (Δ)", style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontWeight = FontWeight.Medium)
                        CopyMoney("diferencia $label Modificado menos Original", difference(amounts.second, amounts.first), currency, signed = true)
                    }
                }
                Column(
                    Modifier
                        .background(BrandMintLight, RoundedCornerShape(8.dp))
                        .border(1.dp, KpiMintBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Diferencia de plazo", style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontWeight = FontWeight.Medium)
                    val term = if (original.complete && modified.complete && original.payoffMonth != null && modified.payoffMonth != null)
                        "${if (modified.payoffMonth!! > original.payoffMonth!!) "+" else ""}${modified.payoffMonth!! - original.payoffMonth!!} meses" else "Incompleto"
                    CopyResult("diferencia de plazo", term)
                }
            }

            Text(
                "Liquidación Original: ${original.payoffDate?.let(::formatMonth) ?: "No alcanzada"} · Modificado: ${modified.payoffDate?.let(::formatMonth) ?: "No alcanzada"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextForestMuted
            )

            // CHART 1: Saldo de capital al cierre
            val balances = results.mapIndexed { i, result ->
                ChartSeries(names[i], BrandForest, i == 0, range.map { m -> resultBalance(result, m) })
            }.filterIndexed { i, _ -> i !in hiddenScenarios }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultLineChart("Saldo de capital al cierre", currency, range, month, balances, ::select)
                // Bottom Legend / Toggles for Chart 1
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Leyenda y filtros:", style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontWeight = FontWeight.Medium)
                    names.forEachIndexed { i, name ->
                        FilterChip(
                            selected = i !in hiddenScenarios,
                            onClick = { onHiddenScenariosChange(if (i in hiddenScenarios) hiddenScenarios - i else hiddenScenarios + i) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandMintLight,
                                selectedLabelColor = BrandDark
                            ),
                            label = { Text("$name: ${if (i in hiddenScenarios) "oculto" else "visible"}") }
                        )
                    }
                }
            }

            // CHART 2: Pagos mensuales por componente
            val payments = results.flatMapIndexed { i, result ->
                if (i in hiddenScenarios) emptyList() else visibleComponents.sorted().map { c ->
                    ChartSeries(
                        "${names[i]} ${componentLabels[c]}",
                        componentColors[c],
                        i == 0,
                        range.map { m ->
                            if (m == 0) null
                            else components[i][m]?.get(c) ?: if (result.complete && m > (result.rows.lastOrNull()?.month ?: 0)) "0.00" else null
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultLineChart("Pagos mensuales por componente", currency, range, month, payments, ::select)
                // Bottom Legend / Toggles for Chart 2
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Componentes pagados por mes:", style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        available.forEach { c ->
                            FilterChip(
                                selected = c in visibleComponents,
                                onClick = { onHiddenComponentsChange(if (c in hiddenComponents) hiddenComponents - c else hiddenComponents + c) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandMintLight,
                                    selectedLabelColor = BrandDark
                                ),
                                label = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(componentColors[c], RoundedCornerShape(2.dp)))
                                        Text("${componentLabels[c]}: ${if (c in visibleComponents) "visible" else "oculto"}")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (hiddenScenarios.size == 2 || visibleComponents.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().background(BrandAmberLight, RoundedCornerShape(8.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sin series visibles${if (hiddenScenarios.size < 2) " de componentes" else ""}", color = BrandAmberText)
                    OutlinedButton(onClick = { onHiddenScenariosChange(emptySet()); onHiddenComponentsChange(emptySet()) }) {
                        Text("Restablecer")
                    }
                }
            }

            // Tramo Navigation & Month Selector
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    enabled = range.first > 0,
                    onClick = { select((range.first - 12).coerceAtLeast(0)) },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Anterior") }
                OutlinedButton(
                    enabled = range.last < horizon,
                    onClick = { select(range.last + 1) },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Siguiente") }
            }

            Text(
                "Tramo M${range.first}–M${range.last} de M0–M$horizon",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = BrandForest
            )

            OutlinedTextField(
                value = monthInput,
                onValueChange = { monthInput = it; it.toIntOrNull()?.let { value -> if (value in 0..horizon) select(value) } },
                label = { Text("Mes seleccionado (0–$horizon)") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Mes seleccionado (0–$horizon)" }
            )

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // DETALLE DEL MES: Tabla Comparativa a 3 Columnas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detalle del mes $month",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandForest
                    )
                    val origBalance = resultBalance(original, month)
                    val modBalance = resultBalance(modified, month)
                    if (origBalance != null && modBalance != null) {
                        Row(
                            Modifier.background(BrandMintLight, RoundedCornerShape(8.dp)).border(1.dp, KpiMintBorder, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Diferencia de saldo:", style = MaterialTheme.typography.labelSmall, color = TextForestMuted)
                            CopyMoney("diferencia de saldo mes $month", difference(modBalance, origBalance), currency, signed = true)
                        }
                    }
                }

                val origRow = original.rows.find { it.month == month }
                val modRow = modified.rows.find { it.month == month }
                val origPaid = components[0][month]
                val modPaid = components[1][month]

                // 3-Column Table Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    color = SurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Header Row
                        Row(
                            Modifier.fillMaxWidth().background(TableHeaderBg).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Métrica", Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandForest)
                            Text("Original${origRow?.date?.let { " · ${formatMonth(it)}" } ?: ""}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandForest)
                            Spacer(Modifier.width(6.dp))
                            Text("Modificado${modRow?.date?.let { " · ${formatMonth(it)}" } ?: ""}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandForest)
                        }

                        // Core Metrics Rows
                        val origBal = resultBalance(original, month)
                        val modBal = resultBalance(modified, month)
                        if (origBal != null && modBal != null) {
                            DetailTableRow("Saldo de capital", origBal, modBal, currency, "saldo de capital", month, dotColor = BrandForest)
                        }

                        if (month == 0) {
                            Text(
                                "M0: capital inicial, antes de pagos. No admite eventos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextForestMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            // Total Paid
                            val origTotalPaid = origRow?.cashPaid ?: if (original.complete) "0.00" else null
                            val modTotalPaid = modRow?.cashPaid ?: if (modified.complete) "0.00" else null
                            if (origTotalPaid != null && modTotalPaid != null) {
                                DetailTableRow("Total pagado", origTotalPaid, modTotalPaid, currency, "total pagado", month, isHighlight = true, dotColor = BrandEmerald)
                            }

                            // Payment Components
                            if (origPaid != null && modPaid != null) {
                                componentLabels.forEachIndexed { c, title ->
                                    DetailTableRow(title, origPaid[c], modPaid[c], currency, title, month, dotColor = componentColors[c])
                                }
                            }

                            // Generated Amounts (Separated from Paid Amounts)
                            val origInterestCharged = origRow?.interestCharged ?: "0.00"
                            val modInterestCharged = modRow?.interestCharged ?: "0.00"
                            DetailTableRow("Interés generado", origInterestCharged, modInterestCharged, currency, "Interés generado", month, dotColor = BrandAmber)

                            val origInsuranceCharged = origRow?.insuranceCharged ?: "0.00"
                            val modInsuranceCharged = modRow?.insuranceCharged ?: "0.00"
                            DetailTableRow("Seguro generado", origInsuranceCharged, modInsuranceCharged, currency, "Seguro generado", month, dotColor = BrandPlum)

                            // Visible Sum if filtered
                            if (visibleComponents.isNotEmpty() && origPaid != null && modPaid != null) {
                                val origVis = sumMoney(visibleComponents.map { origPaid[it] })
                                val modVis = sumMoney(visibleComponents.map { modPaid[it] })
                                DetailTableRow("Importe visible", origVis, modVis, currency, "importe visible", month, dotColor = Color(0xFF0284C7))
                            }

                            // Accordion for Secondary / Accounting Details
                            HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                            TextButton(
                                onClick = { showAdvancedDetails = !showAdvancedDetails },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (showAdvancedDetails) "▲ Ocultar conceptos contables y pendientes" else "▼ Ver conceptos contables y pendientes (Comisión, IVA, Mora, Excedentes)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandEmerald
                                    )
                                }
                            }

                            if (showAdvancedDetails) {
                                val advancedMetrics = listOf(
                                    Triple("Comisión pagada", { r: com.finsim.domain.MonthRow -> r.commissionPaid }, Color(0xFF0284C7)),
                                    Triple("IVA pagado", { r: com.finsim.domain.MonthRow -> r.vatPaid }, Color(0xFF0284C7)),
                                    Triple("Interés pendiente", { r: com.finsim.domain.MonthRow -> r.pendingInterest }, BrandAmber),
                                    Triple("Seguro pendiente", { r: com.finsim.domain.MonthRow -> r.pendingInsurance }, BrandPlum),
                                    Triple("Capital vencido", { r: com.finsim.domain.MonthRow -> r.overduePrincipal }, Color(0xFFDC2626)),
                                    Triple("Mora", { r: com.finsim.domain.MonthRow -> r.arrears }, Color(0xFFDC2626)),
                                    Triple("Deuda total", { r: com.finsim.domain.MonthRow -> r.debt }, BrandForest),
                                    Triple("Excedente no aplicado", { r: com.finsim.domain.MonthRow ->
                                        sumMoney(listOf(r.scheduled.unapplied, r.catchup.unapplied, r.extra.unapplied))
                                    }, BrandAmber)
                                )
                                advancedMetrics.forEach { (label, extractor, dotColor) ->
                                    val valOrig = origRow?.let(extractor) ?: "0.00"
                                    val valMod = modRow?.let(extractor) ?: "0.00"
                                    DetailTableRow(label, valOrig, valMod, currency, label, month, dotColor = dotColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTableRow(
    label: String,
    origAmount: String,
    modAmount: String,
    currency: Currency,
    copyLabel: String,
    month: Int,
    isHighlight: Boolean = false,
    dotColor: Color? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isHighlight) BrandMintLight else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (dotColor != null) {
                Box(Modifier.size(8.dp).background(dotColor, RoundedCornerShape(4.dp)))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlight) BrandForest else TextMain
            )
        }
        Box(
            Modifier
                .weight(1f)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            CopyMoney("$copyLabel Original mes $month", origAmount, currency)
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .weight(1f)
                .background(BrandMintLight, RoundedCornerShape(6.dp))
                .border(0.5.dp, KpiMintBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            CopyMoney("$copyLabel Modificado mes $month", modAmount, currency)
        }
    }
}

private data class ChartSeries(val label: String, val color: Color, val dashed: Boolean, val values: List<String?>)

private fun formatScaleNumber(value: Double, currency: Currency): String {
    val symbol = when (currency) {
        Currency.USD -> "$"
        Currency.EUR -> "€"
        Currency.COP, Currency.ARS -> "$"
    }
    return when {
        value >= 1_000_000 -> "$symbol${(value / 1_000_000).roundToInt()}M"
        value >= 1_000 -> "$symbol${(value / 1_000).roundToInt()}k"
        else -> "$symbol${value.roundToInt()}"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultLineChart(
    title: String,
    currency: Currency,
    range: IntRange,
    month: Int,
    series: List<ChartSeries>,
    onSelect: (Int) -> Unit
) {
    val maximum = series.flatMap { it.values }.filterNotNull().maxByOrNull { it.toDouble() } ?: "0.00"
    val upperBound = if (maximum.toDouble() <= 0.0) "1.00" else maximum
    val scale = upperBound.toDouble()

    var hoveredPoint by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BrandForest)
            val currentInspectMonth = hoveredPoint ?: month
            if (series.isNotEmpty()) {
                val pointValues = series.mapNotNull { s ->
                    val idx = currentInspectMonth - range.first
                    if (idx in s.values.indices) s.values[idx]?.let { s.label to (it to s.color) } else null
                }
                if (pointValues.isNotEmpty()) {
                    Surface(
                        color = BrandDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        FlowRow(
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "M$currentInspectMonth:",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandMint,
                                fontWeight = FontWeight.Bold
                            )
                            pointValues.forEach { (label, data) ->
                                val (valStr, color) = data
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
                                    Text(
                                        "$label: ${formatMoney(valStr, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (series.isEmpty()) {
            Text("Sin series visibles", style = MaterialTheme.typography.bodySmall, color = TextForestMuted)
        }

        // Chart with Left Y-Axis Scale
        Row(Modifier.fillMaxWidth().height(220.dp)) {
            // Y-Axis Labels
            Column(
                Modifier.width(52.dp).fillMaxHeight().padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(formatScaleNumber(scale, currency), style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontSize = 10.sp)
                Text(formatScaleNumber(scale * 0.66, currency), style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontSize = 10.sp)
                Text(formatScaleNumber(scale * 0.33, currency), style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontSize = 10.sp)
                Text(formatScaleNumber(0.0, currency), style = MaterialTheme.typography.labelSmall, color = TextForestMuted, fontSize = 10.sp)
            }

            // Canvas with Area Gradient Fills
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    Modifier.fillMaxSize()
                        .semantics {
                            contentDescription = "$title; selecciona el mes con el control Mes seleccionado"
                            stateDescription = series.joinToString { "${it.label}: ${if (it.dashed) "discontinua" else "continua"}" }
                        }
                        .pointerInput(range) {
                            detectTapGestures { point ->
                                val selected = range.first + (point.x / size.width * (range.last - range.first)).roundToInt().coerceIn(0, range.last - range.first)
                                onSelect(selected)
                            }
                        }
                        .pointerInput(range) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val selected = range.first + (change.position.x / size.width * (range.last - range.first)).roundToInt().coerceIn(0, range.last - range.first)
                                onSelect(selected)
                            }
                        }
                        .pointerInput(range) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Move) {
                                        val pos = event.changes.first().position
                                        if (pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()) {
                                            hoveredPoint = range.first + (pos.x / size.width * (range.last - range.first)).roundToInt().coerceIn(0, range.last - range.first)
                                        }
                                    } else if (event.type == PointerEventType.Exit) {
                                        hoveredPoint = null
                                    }
                                }
                            }
                        }
                ) {
                    fun x(index: Int) = index.toFloat() / (range.last - range.first).coerceAtLeast(1) * size.width

                    fun y(value: String): Float {
                        val d = value.toDoubleOrNull() ?: 0.0
                        if (d <= 0.0) return size.height
                        val topPad = 14f
                        val bottomPad = 6f
                        val availableHeight = size.height - topPad - bottomPad
                        val normalized = (d / scale).toFloat().coerceIn(0f, 1f)
                        val elevation = maxOf(normalized * availableHeight, 10f)
                        return size.height - bottomPad - elevation
                    }

                    // Horizontal Grid Lines
                    for (i in 0..3) {
                        drawLine(
                            BorderSubtle,
                            Offset(0f, size.height * i / 3),
                            Offset(size.width, size.height * i / 3),
                            strokeWidth = 1f
                        )
                    }

                    // Area Gradient Fills under continuous lines
                    series.forEach { line ->
                        if (!line.dashed) {
                            val areaPath = Path()
                            var areaStarted = false
                            var firstPoint = Offset.Zero
                            var lastPoint = Offset.Zero
                            line.values.forEachIndexed { index, value ->
                                if (value != null) {
                                    val pt = Offset(x(index), y(value))
                                    if (!areaStarted) {
                                        areaPath.moveTo(pt.x, pt.y)
                                        firstPoint = pt
                                        areaStarted = true
                                    } else {
                                        areaPath.lineTo(pt.x, pt.y)
                                    }
                                    lastPoint = pt
                                }
                            }
                            if (areaStarted) {
                                areaPath.lineTo(lastPoint.x, size.height)
                                areaPath.lineTo(firstPoint.x, size.height)
                                areaPath.close()
                                drawPath(
                                    areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(line.color.copy(alpha = 0.22f), line.color.copy(alpha = 0.01f)),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                )
                            }
                        }
                    }

                    // Series Lines
                    series.forEach { line ->
                        val path = Path()
                        var connected = false
                        line.values.forEachIndexed { index, value ->
                            if (value == null) {
                                connected = false
                            } else {
                                val point = Offset(x(index), y(value))
                                if (connected) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                                connected = true
                                drawCircle(line.color, if (line.dashed) 2.5f else 3.5f, point)
                            }
                        }
                        drawPath(
                            path,
                            line.color,
                            style = Stroke(
                                width = if (line.dashed) 2f else 3f,
                                pathEffect = if (line.dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
                            )
                        )
                    }

                    // Hover Tracking Line
                    hoveredPoint?.let { hMonth ->
                        val hX = x(hMonth - range.first)
                        drawLine(
                            BrandEmerald.copy(alpha = 0.6f),
                            Offset(hX, 0f),
                            Offset(hX, size.height),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }

                    // Active Month Indicator Line
                    val activeX = x(month - range.first)
                    drawLine(BrandDark, Offset(activeX, 0f), Offset(activeX, size.height), strokeWidth = 2f)
                }
            }
        }

        // X-Axis Labels
        Row(
            Modifier.fillMaxWidth().padding(start = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(range.first, (range.first + range.last) / 2, range.last).distinct().forEach {
                Text("M$it", style = MaterialTheme.typography.labelSmall, color = TextForestMuted)
            }
        }
    }
}
