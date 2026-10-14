package com.finsim.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finsim.comparison.*
import com.finsim.core.Decimal
import com.finsim.domain.Currency

@Composable
fun ComparisonScreen(store: ComparisonWorkspace) {
    var state by remember(store) { mutableStateOf(store.state) }
    fun edit(d: ComparisonDraft) { store.edit(d); state = store.state }
    var expandedOffer by remember { mutableStateOf<String?>("A") }
    var transferMessage by remember { mutableStateOf("") }
    val d = state.draft
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().imePadding()) {
        Text("Compara ofertas con tu dinero", style = MaterialTheme.typography.headlineSmall)
        Text("Añade la información que tienes. Vacío significa desconocido; escribe 0 para confirmar cero. Importes con punto decimal.")
        state.result?.let { ComparisonResults(it, state.applied!!) }
        ComparisonChoice("País de la operación", Country.entries.map { it.name }, d.country?.name) {
            edit(d.copy(country = Country.valueOf(it)))
        }
        ComparisonChoice("Moneda común", Currency.entries.map { it.name }, d.currency?.name) {
            edit(d.copy(currency = Currency.valueOf(it)))
        }
        ComparisonChoice("Necesidad", listOf("Efectivo neto", "Misma compra"), if (d.need == Need.CASH) "Efectivo neto" else "Misma compra") { edit(d.copy(need = if (it == "Efectivo neto") Need.CASH else Need.PURCHASE)) }
        if (d.need == Need.PURCHASE) ComparisonField("Identifica la misma compra", d.purchase) { edit(d.copy(purchase = it)) }
        if (d.need == Need.CASH) ComparisonDatum("Efectivo neto que necesitas", d.cashGoal, state.errors["cashGoal"]) { edit(d.copy(cashGoal = it)) }
        ComparisonField("Mes inicial (AAAA-MM, opcional)", d.referenceMonth, state.errors["referenceMonth"]) { edit(d.copy(referenceMonth = it)) }
        d.offers.forEachIndexed { index, o ->
            fun change(next: Offer) { edit(d.copy(offers = d.offers.mapIndexed { i, old -> if (i == index) next else old })) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expandedOffer = if (expandedOffer == o.id) null else o.id }) { Text((if (expandedOffer == o.id) "Cerrar editor de oferta " else "Editar oferta ") + o.id, style = MaterialTheme.typography.titleLarge) }
                    Text(o.name.ifBlank { "Oferta " + o.id } + " · " + o.count.ifBlank { "?" } + " cuotas · " + o.payment.text.ifBlank { "importe por completar/calcular" })
                    if (expandedOffer == o.id) {
                    ComparisonChoice("Modo " + o.id, OfferMode.entries.map { it.name }, o.mode.name) { change(o.copy(mode = OfferMode.valueOf(it))) }
                    ComparisonDatum("Capital contractual " + o.id + " (opcional en cotización)", o.principal, state.errors[o.id + ".principal"]) { change(o.copy(principal = it)) }
                    if (d.need == Need.PURCHASE) {
                        ComparisonChoice("Producto " + o.id, Product.entries.map { it.name }, o.product.name) { change(o.copy(product = Product.valueOf(it))) }
                        ComparisonDatum("Precio de esta oferta " + o.id, o.price, state.errors[o.id + ".price"]) { change(o.copy(price = it)) }
                        ComparisonDatum("Aporte propio " + o.id, o.downPayment, state.errors[o.id + ".downPayment"]) { change(o.copy(downPayment = it)) }
                        ComparisonDatum("Descuento informado " + o.id + " (ya reflejado en precio)", o.discount, state.errors[o.id + ".discount"]) { change(o.copy(discount = it)) }
                    }
                    if (o.mode == OfferMode.SIMULATION) {
                        ComparisonChoice("Convención de interés " + o.id, RateKind.entries.map { it.name }, o.rateKind.name) { change(o.copy(rateKind = RateKind.valueOf(it))) }
                        ComparisonDatum("Tasa de interés % " + o.id, o.rate, state.errors[o.id + ".rate"]) { change(o.copy(rate = it)) }
                        if (o.rateKind == RateKind.NOMINAL) ComparisonField("Capitalizaciones por año " + o.id, o.frequency, state.errors[o.id + ".frequency"]) { change(o.copy(frequency = it)) }
                        Text("Simulación fija mensual educativa; no interés diario, abonos ni reglas contractuales de una entidad. EM: efectiva mensual; EA: efectiva anual; NOMINAL: nominal anual.")
                        o.insurance.forEachIndexed { i, ins ->
                            fun update(next: OfferInsurance) { change(o.copy(insurance = o.insurance.mapIndexed { j, old -> if (i == j) next else old })) }
                            ComparisonField("Nombre del seguro " + o.id + " #" + (i + 1), ins.name) { update(ins.copy(name = it)) }
                            ComparisonChoice("Seguro " + o.id + " #" + (i + 1), listOf("Importe mensual", "Porcentaje del saldo inicial"), if (ins.percentage) "Porcentaje del saldo inicial" else "Importe mensual") { update(ins.copy(percentage = it != "Importe mensual")) }
                            ComparisonDatum("Valor del seguro " + o.id + " #" + (i + 1), ins.value, state.errors[o.id + ".insurance." + i + ".value"]) { update(ins.copy(value = it)) }
                            ComparisonChoice("Final del seguro " + o.id + " #" + (i + 1), listOf("Al liquidar", "Hasta mes explícito"), if (ins.endsAtPayoff) "Al liquidar" else "Hasta mes explícito") { update(ins.copy(endsAtPayoff = it == "Al liquidar")) }
                            if (!ins.endsAtPayoff) ComparisonField("Último mes del seguro (desde primera cuota) " + o.id + " #" + (i + 1), ins.endMonth, state.errors[o.id + ".insurance." + i + ".endMonth"]) { update(ins.copy(endMonth = it)) }
                            TextButton(onClick = { change(o.copy(insurance = o.insurance.filterIndexed { j, _ -> j != i })) }) { Text("Quitar seguro " + (i + 1)) }
                        }
                        TextButton(onClick = { change(o.copy(insurance = o.insurance + OfferInsurance("insurance-" + ((o.insurance.mapNotNull { it.id.removePrefix("insurance-").toIntOrNull() }.maxOrNull() ?: 0) + 1)))) }) { Text("Añadir seguro " + o.id) }
                    }
                    ComparisonField("Nombre " + o.id, o.name, state.errors[o.id + ".name"]) { change(o.copy(name = it)) }
                    ComparisonField("Fuente de la oferta " + o.id + " (opcional)", o.source, state.errors[o.id + ".source.source"]) { change(o.copy(source = it)) }
                    ComparisonField("Fecha de la oferta " + o.id + " (AAAA-MM-DD)", o.sourceDate, state.errors[o.id + ".source.date"]) { change(o.copy(sourceDate = it)) }
                    Text(if (o.mode == OfferMode.QUOTE) "Cotización transcrita; reproduce los pagos ingresados." else "Simulación bajo supuestos mensuales educativos.")
                    Text("Sin perfiles contractuales contrastados. Elegir país no acredita reproducir un contrato.")
                    o.indicators.forEachIndexed { i, indicator ->
                        fun update(next: PublishedIndicator) { change(o.copy(indicators = o.indicators.mapIndexed { j, old -> if (j == i) next else old })) }
                        Text("Indicador publicado " + indicator.kind.name + "; no se usa como interés.")
                        ComparisonDatum("Valor publicado " + o.id + " " + indicator.kind, indicator.value, state.errors[o.id + ".indicator." + i + ".value"]) { update(indicator.copy(value = it)) }
                        ComparisonField("Unidad " + o.id + " " + indicator.kind, indicator.unit) { update(indicator.copy(unit = it)) }
                        ComparisonField("Base de impuestos " + o.id + " " + indicator.kind, indicator.taxBase) { update(indicator.copy(taxBase = it)) }
                        if (indicator.value.source.isEmpty() || indicator.value.date.isEmpty()) Text("Indicador sin procedencia completa.")
                        TextButton(onClick = { change(o.copy(indicators = o.indicators.filterIndexed { j, _ -> j != i })) }) { Text("Quitar indicador " + indicator.kind) }
                    }
                    IndicatorKind.entries.filter { kind -> o.indicators.none { it.kind == kind } }.forEach { kind ->
                        TextButton(onClick = { change(o.copy(indicators = o.indicators + PublishedIndicator(kind))) }) { Text("Añadir indicador publicado " + kind) }
                    }
                    if (d.need == Need.CASH) ComparisonDatum("Neto recibido " + o.id + " (vacío: objetivo común)", o.net, state.errors[o.id + ".net"]) { change(o.copy(net = it)) }
                    ComparisonField("Cantidad de cuotas " + o.id, o.count, state.errors[o.id + ".count"]) { change(o.copy(count = it)) }
                    ComparisonDatum((if (o.mode == OfferMode.SIMULATION) "Cuota base capital e interés, opcional " else "Importe por cuota ") + o.id, o.payment, state.errors[o.id + ".payment"]) { change(o.copy(payment = it)) }
                    ComparisonField("Mes de primera cuota " + o.id + " (vacío: mes 1 estimado)", o.firstMonth, state.errors[o.id + ".firstMonth"]) { change(o.copy(firstMonth = it)) }
                    if (o.mode == OfferMode.QUOTE) {
                    o.exceptions.forEachIndexed { i, e ->
                        val path = o.id + ".exception." + i
                        ComparisonField("Mes excepcional " + o.id + " #" + (i + 1), e.month, state.errors[path + ".month"]) { text ->
                            change(o.copy(exceptions = o.exceptions.mapIndexed { j, old -> if (i == j) old.copy(month = text) else old }))
                        }
                        ComparisonDatum("Cuota excepcional " + o.id + " #" + (i + 1), e.amount, state.errors[path + ".amount"]) { value ->
                            change(o.copy(exceptions = o.exceptions.mapIndexed { j, old -> if (i == j) old.copy(amount = value) else old }))
                        }
                        TextButton(onClick = { change(o.copy(exceptions = o.exceptions.filterIndexed { j, _ -> j != i })) }) { Text("Quitar excepción " + (i + 1)) }
                    }
                    TextButton(onClick = { change(o.copy(exceptions = o.exceptions + PaymentException("", Datum()))) }) { Text("Añadir cuota diferente " + o.id) }
                    }
                    if (o.requiresPaymentReview) {
                        Text("Pagos pendientes de revisión: cambiar capital o plazo no cambia la cotización del banco.")
                        OutlinedButton(onClick = { change(o.copy(requiresPaymentReview = false)) }) { Text((if (o.mode == OfferMode.QUOTE) "Confirmar cuotas actualizadas " else "Confirmar cuota base compatible ") + o.id) }
                    }
                    Text("Cargos: importes por mes; para porcentajes con base no modelada, indica un importe estimado.")
                    o.fees.forEachIndexed { i, f ->
                        fun update(next: Fee) { change(o.copy(fees = o.fees.mapIndexed { j, old -> if (i == j) next else old })) }
                        val key = o.id + ".fee." + i
                        if (f.needsReview) {
                            Text("Importe por revisar tras cambiar condiciones.")
                            OutlinedButton(onClick = { update(f.copy(needsReview = false)) }) { Text("Confirmar cargo actualizado " + o.id + " #" + (i + 1)) }
                        }
                        ComparisonChoice("Origen del cargo " + o.id + " #" + (i + 1), listOf("Nuevo", "Preexistente"), if (f.existing) "Preexistente" else "Nuevo") { update(f.copy(existing = it == "Preexistente")) }
                        if (f.existing) {
                            ComparisonField("Identidad de obligación común " + o.id + " #" + (i + 1), f.id) { update(f.copy(id = it)) }
                            ComparisonChoice("Ya incluido en contexto " + o.id + " #" + (i + 1), Coverage.entries.map { it.name }, f.coverage.name) { update(f.copy(coverage = Coverage.valueOf(it))) }
                            Text("EXPENSES: ya en gastos · DEBTS: ya en cuotas vigentes · ADD: sumar aparte · UNKNOWN: por confirmar. La obligación se mantiene en ambas alternativas.")
                        }
                        ComparisonField("Concepto del cargo " + o.id + " #" + (i + 1), f.name) { update(f.copy(name = it)) }
                        ComparisonDatum("Importe del cargo " + o.id + " #" + (i + 1), f.amount, state.errors[key + ".amount"]) { update(f.copy(amount = it)) }
                        ComparisonChoice("Tratamiento " + o.id + " #" + (i + 1), FeeTreatment.entries.map { it.name }, f.treatment.name) { update(f.copy(treatment = FeeTreatment.valueOf(it), recurring = false)) }
                        Text("INITIAL: propio inicial · DEDUCTED: descontado · FINANCED: financiado · SEPARATE: aparte · INCLUDED: ya incluido en cuota.")
                        if (f.treatment in listOf(FeeTreatment.SEPARATE, FeeTreatment.INCLUDED)) {
                            ComparisonField("Mes del cargo " + o.id + " #" + (i + 1), f.start, state.errors[key + ".start"]) { update(f.copy(start = it)) }
                            ComparisonChoice("Repetición " + o.id + " #" + (i + 1), listOf("Una vez", "Mensual"), if (f.recurring) "Mensual" else "Una vez") { update(f.copy(recurring = it == "Mensual")) }
                            if (f.recurring) ComparisonField("Último mes del cargo " + o.id + " #" + (i + 1) + " (vacío: desconocido)", f.end, state.errors[key + ".end"]) { update(f.copy(end = it)) }
                        }
                        TextButton(onClick = { change(o.copy(fees = o.fees.filterIndexed { j, _ -> j != i })) }) { Text("Quitar cargo " + (i + 1)) }
                    }
                    TextButton(onClick = { change(o.copy(fees = o.fees + Fee("fee-" + ((o.fees.mapNotNull { it.id.removePrefix("fee-").toIntOrNull() }.maxOrNull() ?: 0) + 1)))) }) { Text("Añadir cargo " + o.id) }
                    Row {
                        Checkbox(o.chargesConfirmed, { change(o.copy(chargesConfirmed = it)) }, modifier = Modifier.semantics { contentDescription = "Confirmar todos los cargos de la oferta " + o.id })
                        Text("Confirmo todos los cargos; los no indicados son cero", Modifier.weight(1f).padding(top = 12.dp))
                    }
                    }
                }
            }
        }
        OutlinedButton(onClick = { edit(d.copy(context = if (d.context == null) PersonalContext() else null)) }) { Text(if (d.context == null) "Añadir contexto personal opcional" else "Omitir contexto personal") }
        d.context?.let { ctx ->
            ComparisonDatum("Ingreso neto mensual", ctx.income, state.errors["context.income"]) { edit(d.copy(context = ctx.copy(income = it))) }
            ComparisonDatum("Gastos habituales mensuales", ctx.expenses, state.errors["context.expenses"]) { edit(d.copy(context = ctx.copy(expenses = it))) }
            ComparisonDatum("Cuotas vigentes mensuales", ctx.debts, state.errors["context.debts"]) { edit(d.copy(context = ctx.copy(debts = it))) }
            Text("Referencia mensual constante. El margen incluye salidas iniciales y obligaciones preexistentes una vez; puede ser negativo. No es saldo en cuenta, ahorro ni garantía de disponer del aporte.")
        }
        Text(if (state.isDirty) "Borrador con trabajo sin guardar." else "Sin cambios desde el último guardado.")
        Button(onClick = { store.save(); state = store.state }) { Text("Guardar borrador local") }
        ComparisonFileActions(exportJson = { store.exportJson() }, importJson = { text ->
            store.importJson(text).also { state = store.state }
        }, onMessage = { transferMessage = it })
        if (transferMessage.isNotBlank()) Text(transferMessage)
        Text(if (state.hasPending) "Cambios pendientes: las vistas muestran la última aplicación." else "Vistas al día.")
        Button(onClick = { if (store.apply()) expandedOffer = null; state = store.state }) { Text("Aplicar comparación") }
        OutlinedButton(onClick = { store.discard(); state = store.state }) { Text("Descartar cambios") }
        state.errors.forEach { (key, value) -> Text(key + ": " + value, color = MaterialTheme.colorScheme.error) }
        if (state.message.isNotEmpty()) Text(state.message)
        state.reference?.let { reference ->
            var showReference by remember { mutableStateOf(false) }
            Text(if (state.applied == reference) "Usando la cotización de referencia." else "Escenario hipotético; referencia conservada.")
            TextButton(onClick = { showReference = !showReference }) { Text(if (showReference) "Ocultar referencia" else "Ver cotización de referencia") }
            if (showReference) reference.offers.forEach { original ->
                Text("Referencia " + original.id + ": " + original.name + " · capital " + original.principal.text + " · " + original.count + " cuotas de " + original.payment.text + " · tasa % " + original.rate.text)
                original.fees.forEach { Text(it.name + ": " + it.amount.text + " · " + it.treatment) }
            }
        }
        OutlinedButton(onClick = { store.recordReference(); state = store.state }) { Text("Registrar como cotización de referencia") }

    }
}

@Composable
internal fun ComparisonChoice(label: String, choices: List<String>, selected: String?, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(label + ": " + (selected?.let(::comparisonLabel) ?: "Sin completar")) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { value -> DropdownMenuItem(text = { Text(comparisonLabel(value)) }, onClick = { onSelect(value); open = false }) }
        }
    }
}
private fun comparisonLabel(value: String): String = when (value) {
    "CO" -> "Colombia"; "AR" -> "Argentina"
    "QUOTE" -> "Cotización"; "SIMULATION" -> "Simulación mensual"
    "LOAN" -> "Préstamo personal"; "CARD_EXISTING" -> "Tarjeta existente"; "CARD_NEW" -> "Tarjeta nueva"
    "INITIAL" -> "Con dinero propio al inicio"; "FINANCED" -> "Financiado dentro del capital"
    "DEDUCTED" -> "Descontado del crédito"; "SEPARATE" -> "Cobrado aparte"; "INCLUDED" -> "Incluido en la cuota"
    "EXPENSES" -> "Ya incluido en gastos"; "DEBTS" -> "Ya incluido en cuotas vigentes"; "ADD" -> "Sumar aparte"; "UNKNOWN" -> "Por confirmar"
    else -> value
}
@Composable
internal fun ComparisonField(label: String, text: String, error: String? = null, numeric: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = text, onValueChange = onChange, label = { Text(label) },
        isError = error != null, supportingText = { if (error != null) Text(error) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text), modifier = Modifier.fillMaxWidth())
}
@Composable
internal fun ComparisonDatum(label: String, value: Datum, error: String? = null, onChange: (Datum) -> Unit) {
    ComparisonField(label, value.text, error, numeric = true) { onChange(value.copy(text = it)) }
    var detail by remember { mutableStateOf(false) }
    TextButton(onClick = { detail = !detail }) { Text(if (detail) "Ocultar procedencia: " + label else "Estimación y fuente: " + label) }
    if (detail) {
    Row {
        Checkbox(value.provenance == Provenance.ESTIMATED, { onChange(value.copy(provenance = if (it) Provenance.ESTIMATED else Provenance.RECORDED)) }, modifier = Modifier.semantics { contentDescription = "Marcar estimación: " + label })
        Text("Estimación: " + label, Modifier.weight(1f).padding(top = 12.dp))
    }
    ComparisonField("Fuente: " + label, value.source) { onChange(value.copy(source = it)) }
    ComparisonField("Fecha AAAA-MM-DD: " + label, value.date) { onChange(value.copy(date = it)) }
    }
}
@Composable
internal fun ComparisonResults(result: ComparisonResult, applied: ComparisonDraft) {
    var selected by remember { mutableStateOf(0) }
    var table by remember { mutableStateOf(false) }
    val max = result.offers.flatMap { it.rows }.maxOfOrNull { Decimal.parse(it.cash) } ?: Decimal.ONE
    val currency = applied.currency
    val selectedMonth = selected.coerceIn(0, result.horizon)
    Text("Resultados aplicados · " + (currency?.name ?: "moneda por completar"), style = MaterialTheme.typography.titleLarge)
    Text("País: " + (applied.country?.name?.let(::comparisonLabel) ?: "por completar") + " · calendario: " + applied.referenceMonth.ifBlank { "relativo estimado" })
    Text(if (applied.need == Need.CASH) "Objetivo neto: " + applied.cashGoal.text else "Misma compra: " + applied.purchase)
    Text("B menos A; signo positivo significa aumento. No hay una opción ganadora automática.")
    if (!result.equivalent) Text("Completa la misma necesidad, país y moneda antes de comparar diferencias.")
    result.offers.forEach { o ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Oferta " + o.id, style = MaterialTheme.typography.titleMedium)
                Text("Cuotas registradas: " + o.quotedPayments)
                Text("Salidas nuevas conocidas: " + o.knownTotal + " · " + if (o.complete) "completo bajo tus datos" else "subtotal parcial")
                val payments = o.rows.filter { it.installment }.mapNotNull { it.payment?.let(Decimal::parse) }
                Text("Cuota/rango registrado: " + if (payments.isEmpty()) "por completar" else payments.minOrNull()!!.toFixed() + " a " + payments.maxOrNull()!!.toFixed())
                Text("Capital contractual: " + applied.offers.first { it.id == o.id }.principal.text.ifBlank { "por completar" })
                if (applied.need == Need.CASH) Text("Neto recibido: " + (o.net ?: "por completar"))
                Text("Costo nominal: " + (o.financeCost ?: "sin completar"))
                Text("Salida inicial: " + o.initial)
                Text( "Último mes de obligaciones declarado: " + (o.lastMonth ?: "sin completar"))
                if (o.estimated) Text("Incluye estimaciones o alineación relativa.")
                o.warnings.forEach { Text(it) }
            }
        }
    }
    result.differences.forEach { (name, value) -> Text("B − A · " + name + ": " + if (value.startsWith("-")) value else "+" + value) }
    Text("Pagos mensuales · misma escala 0 a " + max.toFixed() + " · horizonte mes 0 a " + result.horizon)
    Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = "Salidas mensuales de A y B con la misma escala. Consulta el detalle textual debajo." }
        .pointerInput(result) { detectTapGestures { selected = ((it.x / size.width) * (result.horizon + 1)).toInt().coerceIn(0, result.horizon) } }) {
        val column = size.width / (result.horizon + 1)
        if (max.isPositive) result.offers.forEachIndexed { offer, resultOffer ->
            resultOffer.rows.forEach { row ->
                val height = ((Decimal.parse(row.cash) / max).toString().toFloat()).coerceIn(0f, 1f) * size.height
                drawRect(if (offer == 0) Color(0xFF315D43) else Color(0xFF8D630C),
                    topLeft = Offset(column * (row.month + offer * 0.45f), size.height - height),
                    size = Size(column * 0.4f, height))
            }
        }
    }
    Text("Mes 0 (inicial) → mes " + result.horizon + " · verde A / ocre B; valores accesibles debajo.")
    Text("Mes seleccionado: " + selectedMonth)
    Slider(value = selectedMonth.toFloat(), onValueChange = { selected = it.roundToInt() },
        steps = maxOf(0, result.horizon - 1), valueRange = 0f..maxOf(1, result.horizon).toFloat(),
        modifier = Modifier.semantics { contentDescription = "Seleccionar mes común" })
    ComparisonMonth(result, selectedMonth, max)
    TextButton(onClick = { table = !table }) { Text(if (table) "Ocultar detalle mensual" else "Mostrar tabla mensual accesible") }
    if (table) LazyColumn(Modifier.fillMaxWidth().height(400.dp)) {
        items((0..result.horizon).toList()) { month ->
            TextButton(onClick = { selected = month }) { Text("Seleccionar mes " + month) }
            ComparisonMonth(result, month, max)
        }
    }

}
@Composable
private fun ComparisonMonth(result: ComparisonResult, month: Int, max: Decimal) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Mes " + month)
        result.offers.forEachIndexed { i, o ->
            val row = o.rows.firstOrNull { it.month == month }
            val cash = row?.let { Decimal.parse(it.cash) } ?: Decimal.ZERO
            val known = row?.payment != null && !row.uncertain
            val label = "Oferta " + o.id + ": " + (if (known) cash.toFixed() else "subtotal " + cash.toFixed() + " · pagos/cargos por confirmar")
            Text(label)
            Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFE1E7DA))) {
                if (max.isPositive && cash.isPositive) Box(Modifier.fillMaxWidth((cash / max).toString().toFloat().coerceIn(0f, 1f)).height(12.dp).background(if (i == 0) Color(0xFF315D43) else Color(0xFF8D630C)))
            }
            row?.let {
                Text("Cuota: " + (it.payment ?: "?") + " · inicial: " + it.initial + " · cargos aparte: " + it.extra)
                if (it.interest != null) Text("Capital " + it.principal + " · interés " + it.interest + " · seguro " + it.insurance + " · saldo " + it.balance)
                Text("Preexistente incluido que se excluye: " + (it.excludedPreexisting ?: "desconocido"))
                Text("Margen mensual: " + (it.margin ?: "no disponible con estos datos"))
            }
        }
    }
}
