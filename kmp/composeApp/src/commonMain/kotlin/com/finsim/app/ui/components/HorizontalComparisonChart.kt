package com.finsim.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult
import kotlin.math.roundToInt

private val componentLabels = listOf("Capital habitual", "Capital adicional", "Intereses pagados", "Seguros pagados", "Otros cargos")
private val componentColors = listOf(BrandForest, BrandLimeDark, BrandAmber, BrandPlum, Color(0xFF176B91))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HorizontalComparisonChart(
    currency: Currency,
    original: LoanResult,
    modified: LoanResult,
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
    // Remember exclusions so a newly available category starts enabled after applying events.
    var hiddenComponents by remember { mutableStateOf(emptySet<Int>()) }
    var hiddenScenarios by remember { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(available) { hiddenComponents = hiddenComponents.intersect(available) }
    val visibleComponents = available - hiddenComponents
    val range = monthWindow(month, horizon)
    var monthInput by remember(month) { mutableStateOf(month.toString()) }
    fun select(value: Int) { onSelectMonth(value.coerceIn(0, horizon)) }

    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceWarm)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Original y modificado", style = MaterialTheme.typography.titleMedium)
            Text("Original: línea discontinua · Modificado: línea continua. Diferencias = modificado − original.", style = MaterialTheme.typography.bodySmall)
            if (!original.complete || !modified.complete) Text("Resultado incompleto: diferencias parciales; sin ahorro ni liquidación definitivos.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Intereses" to (original.totals.interest to modified.totals.interest),
                    "Seguros" to (original.totals.insurance to modified.totals.insurance),
                    "Caja total" to (original.totals.cashPaid to modified.totals.cashPaid)).forEach { (label, amounts) ->
                    Column {
                        Text("Diferencia $label", style = MaterialTheme.typography.labelSmall)
                        CopyMoney("diferencia $label Modificado menos Original", difference(amounts.second, amounts.first), currency)
                    }
                }
                Column {
                    Text("Diferencia de plazo", style = MaterialTheme.typography.labelSmall)
                    val term = if (original.complete && modified.complete && original.payoffMonth != null && modified.payoffMonth != null)
                        "${modified.payoffMonth!! - original.payoffMonth!!} meses" else "Incompleto"
                    CopyResult("diferencia de plazo", term)
                }
            }
            Text("Liquidación Original: ${original.payoffDate?.let(::formatMonth) ?: "No alcanzada"} · Modificado: ${modified.payoffDate?.let(::formatMonth) ?: "No alcanzada"}", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, name -> FilterChip(i !in hiddenScenarios,
                    { hiddenScenarios = if (i in hiddenScenarios) hiddenScenarios - i else hiddenScenarios + i },
                    label = { Text("Mostrar $name") }) }
            }
            Text("Componentes pagados por mes", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                available.forEach { c -> FilterChip(c in visibleComponents,
                    { hiddenComponents = if (c in hiddenComponents) hiddenComponents - c else hiddenComponents + c },
                    label = { Text(componentLabels[c], color = componentColors[c]) }) }
            }
            if (hiddenScenarios.size == 2 || visibleComponents.isEmpty()) {
                Text("Sin series visibles${if (hiddenScenarios.size < 2) " de componentes" else ""}")
                OutlinedButton(onClick = { hiddenScenarios = emptySet(); hiddenComponents = emptySet() }) { Text("Restablecer") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(enabled = range.first > 0, onClick = { select((range.first - 12).coerceAtLeast(0)) }) { Text("Anterior") }
                OutlinedButton(enabled = range.last < horizon, onClick = { select(range.last + 1) }) { Text("Siguiente") }
            }
            Text("Tramo M${range.first}–M${range.last} de M0–M$horizon", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = monthInput, onValueChange = { monthInput = it; it.toIntOrNull()?.let { value -> if (value in 0..horizon) select(value) } },
                label = { Text("Mes seleccionado (0–$horizon)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            val balances = results.mapIndexed { i, result -> ChartSeries(names[i], BrandForest, i == 0,
                range.map { m -> resultBalance(result, m) }) }.filterIndexed { i, _ -> i !in hiddenScenarios }
            ResultLineChart("Saldo de capital al cierre", currency, range, month, balances, ::select)
            val payments = results.flatMapIndexed { i, result ->
                if (i in hiddenScenarios) emptyList() else visibleComponents.sorted().map { c ->
                    ChartSeries("${names[i]} ${componentLabels[c]}", componentColors[c], i == 0,
                        range.map { m -> if (m == 0) null else components[i][m]?.get(c) ?: if (result.complete && m > (result.rows.lastOrNull()?.month ?: 0)) "0.00" else null })
                }
            }
            ResultLineChart("Pagos mensuales por componente", currency, range, month, payments, ::select)
            Text("Detalle del mes $month", style = MaterialTheme.typography.titleMedium)
            Text("El detalle conserva ambos escenarios y los importes reales aunque ocultes series.", style = MaterialTheme.typography.bodySmall)
            val origBalance = resultBalance(original, month)
            val modBalance = resultBalance(modified, month)
            if (origBalance != null && modBalance != null) {
                Text("Diferencia de saldo (modificado − original)")
                CopyMoney("diferencia de saldo mes $month", difference(modBalance, origBalance), currency)
            }
            results.forEachIndexed { i, result ->
                val row = result.rows.find { it.month == month }
                val name = names[i]
                Text("$name${row?.date?.let { " · ${formatMonth(it)}" } ?: ""}", style = MaterialTheme.typography.titleSmall)
                fun label(data: String) = "$data $name mes $month"
                val balance = resultBalance(result, month)
                if (balance != null) { Text("Saldo de capital"); CopyMoney(label("saldo de capital"), balance, currency) }
                when {
                    month == 0 -> Text("M0: capital inicial, antes de pagos. No admite eventos.")
                    row != null -> {
                        val paid = components[i].getValue(month)
                        componentLabels.forEachIndexed { c, title ->
                            Text(title); CopyMoney(label(title), paid[c], currency)
                        }
                        if (i in hiddenScenarios || visibleComponents.isEmpty()) Text("Importe visible: sin series de componentes visibles")
                        else { Text("Importe visible"); CopyMoney(label("importe visible"), sumMoney(visibleComponents.map { paid[it] }), currency) }
                        Text("Total pagado"); CopyMoney(label("total pagado"), row.cashPaid, currency)
                        listOf("Interés generado" to row.interestCharged, "Seguro generado" to row.insuranceCharged,
                            "Interés pendiente" to row.pendingInterest, "Seguro pendiente" to row.pendingInsurance,
                            "Capital vencido" to row.overduePrincipal, "Mora" to row.arrears, "Deuda total" to row.debt,
                            "Excedente no aplicado" to sumMoney(listOf(row.scheduled.unapplied, row.catchup.unapplied, row.extra.unapplied))).forEach { (title, amount) ->
                            Text(title); CopyMoney(label(title), amount, currency)
                        }
                    }
                    result.complete -> {
                        Text("Liquidado; sin pagos posteriores en este mes.")
                        Text("Total pagado"); CopyMoney(label("total pagado"), "0.00", currency)
                    }
                    else -> Text("Sin datos simulados para este mes; resultado incompleto.")
                }
            }
        }
    }
}

private data class ChartSeries(val label: String, val color: Color, val dashed: Boolean, val values: List<String?>)

@Composable
private fun ResultLineChart(title: String, currency: Currency, range: IntRange, month: Int,
    series: List<ChartSeries>, onSelect: (Int) -> Unit) {
    val maximum = series.flatMap { it.values }.filterNotNull().maxByOrNull { it.toDouble() } ?: "0.00"
    val scale = maximum.toDouble().coerceAtLeast(1.0)
    Text(title, style = MaterialTheme.typography.titleSmall)
    Text("Escala: ${formatMoney("0.00", currency)} a ${formatMoney(maximum, currency)}", style = MaterialTheme.typography.bodySmall)
    if (series.isEmpty()) Text("Sin series visibles")
    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = "$title; selecciona el mes con el control Mes seleccionado" }
        .pointerInput(range) { detectTapGestures { point -> onSelect(range.first + (point.x / size.width * (range.last - range.first)).roundToInt().coerceIn(0, range.last - range.first)) } }
        .pointerInput(range) { detectDragGestures { change, _ ->
            change.consume()
            onSelect(range.first + (change.position.x / size.width * (range.last - range.first)).roundToInt().coerceIn(0, range.last - range.first))
        } }) {
        fun x(index: Int) = index.toFloat() / (range.last - range.first).coerceAtLeast(1) * size.width
        for (i in 0..4) drawLine(BorderSage, Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4))
        series.forEach { line ->
            val path = Path()
            var connected = false
            line.values.forEachIndexed { index, value ->
                if (value == null) connected = false else {
                    val point = Offset(x(index), size.height * (1 - value.toDouble() / scale).toFloat())
                    if (connected) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                    connected = true
                    drawCircle(line.color, if (line.dashed) 2f else 3f, point)
                }
            }
            drawPath(path, line.color, style = Stroke(width = if (line.dashed) 2f else 3f,
                pathEffect = if (line.dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null))
        }
        drawLine(BrandDark, Offset(x(month - range.first), 0f), Offset(x(month - range.first), size.height), strokeWidth = 1.5f)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(range.first, (range.first + range.last) / 2, range.last).distinct().forEach { Text("M$it", style = MaterialTheme.typography.labelSmall) }
    }
}
