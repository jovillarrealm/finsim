package com.finsim.comparison

import com.finsim.core.Decimal
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import com.finsim.persistence.ScenarioStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable enum class Need { CASH, PURCHASE }
@Serializable enum class Product { LOAN, CARD_EXISTING, CARD_NEW }
@Serializable data class PersonalContext(val income: Datum = Datum(), val expenses: Datum = Datum(), val debts: Datum = Datum())
@Serializable enum class Coverage { UNKNOWN, EXPENSES, DEBTS, ADD }
@Serializable enum class FeeTreatment { INITIAL, DEDUCTED, FINANCED, SEPARATE, INCLUDED }
@Serializable data class Fee(
    val id: String, val name: String = "Cargo", val amount: Datum = Datum(),
    val existing: Boolean = false, val coverage: Coverage = Coverage.UNKNOWN,
    val needsReview: Boolean = false,
    val treatment: FeeTreatment = FeeTreatment.SEPARATE,
    val start: String = "1", val recurring: Boolean = false, val end: String = ""
)
@Serializable enum class OfferMode { QUOTE, SIMULATION }
@Serializable enum class RateKind { EM, EA, NOMINAL }
@Serializable data class OfferInsurance(
    val id: String, val name: String = "Seguro", val percentage: Boolean = false,
    val value: Datum = Datum(), val endsAtPayoff: Boolean = true, val endMonth: String = ""
)
@Serializable enum class Country { CO, AR }
@Serializable enum class Provenance { RECORDED, ESTIMATED }
@Serializable data class Datum(
    val text: String = "", val provenance: Provenance = Provenance.RECORDED,
    val source: String = "", val date: String = ""
)
@Serializable enum class IndicatorKind { CFT, VTUA }
@Serializable data class PublishedIndicator(
    val kind: IndicatorKind, val value: Datum = Datum(), val unit: String = "", val taxBase: String = ""
)
@Serializable data class PaymentException(val month: String, val amount: Datum)
@Serializable data class Offer(
    val id: String,
    val name: String = "",
    val source: String = "",
    val sourceDate: String = "",
    val indicators: List<PublishedIndicator> = emptyList(),
    val mode: OfferMode = OfferMode.QUOTE,
    val product: Product = Product.LOAN,
    val requiresPaymentReview: Boolean = false,
    val price: Datum = Datum(),
    val downPayment: Datum = Datum(),
    val discount: Datum = Datum(),
    val principal: Datum = Datum(),
    val rate: Datum = Datum(),
    val rateKind: RateKind = RateKind.EM,
    val frequency: String = "",
    val insurance: List<OfferInsurance> = emptyList(),
    val currency: Currency? = null,
    val net: Datum = Datum(),
    val count: String = "",
    val payment: Datum = Datum(),
    val firstMonth: String = "",
    val exceptions: List<PaymentException> = emptyList(),
    val fees: List<Fee> = emptyList(),
    val chargesConfirmed: Boolean = false
)
@Serializable data class ComparisonDraft(
    val country: Country? = null,
    val need: Need = Need.CASH,
    val purchase: String = "",
    val context: PersonalContext? = null,
    val currency: Currency? = null,
    val referenceMonth: String = "",
    val cashGoal: Datum = Datum(),
    val offers: List<Offer> = listOf(Offer("A"), Offer("B"))
)
data class CashRow(
    val month: Int, val payment: String?, val initial: String = "0.00",
    val interest: String? = null, val principal: String? = null,
    val insurance: String? = null, val balance: String? = null,
    val extra: String = "0.00", val uncertain: Boolean = false,
    val excludedPreexisting: String? = "0.00",
    val cash: String = "0.00",
    val installment: Boolean = false,
    val margin: String? = null
)
data class OfferResult(
    val id: String, val rows: List<CashRow>, val knownTotal: String,
    val financeCost: String?, val complete: Boolean, val estimated: Boolean,
    val warnings: List<String>, val initial: String = "0.00",
    val net: String? = null,
    val quotedPayments: String = "0.00",
    val lastMonth: Int? = null
)
data class ComparisonResult(
    val offers: List<OfferResult>, val horizon: Int,
    val equivalent: Boolean, val differences: Map<String, String>
)
data class ComparisonState(
    val draft: ComparisonDraft = ComparisonDraft(),
    val applied: ComparisonDraft? = null,
    val result: ComparisonResult? = null,
    val errors: Map<String, String> = emptyMap(),
    val isDirty: Boolean = false,
    val message: String = "",
    val reference: ComparisonDraft? = null
) {
    val hasPending: Boolean get() = draft != applied
}
private fun amountSafe(d: Datum): Decimal? = runCatching { amount(d) }.getOrNull()
internal fun amount(d: Datum): Decimal? = d.text.takeIf { it.isNotEmpty() }?.let(Decimal::parse)
private fun validDate(value: String): Boolean {
    if (!Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(value)) return false
    val (year, month, day) = value.split("-").map { it.toInt() }
    if (year < 1 || month !in 1..12) return false
    val days = listOf(31, if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    return day in 1..days[month - 1]
}
internal fun validateDraft(d: ComparisonDraft): Map<String, String> {
    val errors = linkedMapOf<String, String>()
    fun origin(path: String, source: String, date: String) {
        if (source.length > 2000) errors[path + ".source"] = "La fuente admite hasta 2.000 caracteres."
        if (date.isNotEmpty() && !validDate(date)) errors[path + ".date"] = "Usa una fecha real AAAA-MM-DD."
    }
    fun money(path: String, v: Datum, positive: Boolean = false) {
        origin(path, v.source, v.date)
        if (v.text.isEmpty()) return
        if (!Regex("^[0-9]+(\\.[0-9]{1,2})?$").matches(v.text) || v.text.length > 19) {
            errors[path] = "Usa un importe no negativo, con punto y máximo dos decimales."
            return
        }
        val a = Decimal.parse(v.text)
        if (a > Decimal.parse("1000000000000000") || (positive && !a.isPositive))
            errors[path] = "El importe debe ser positivo cuando corresponde y no superar 1.000.000.000.000.000."
    }
    fun month(path: String, v: String, zero: Boolean = false): Int? {
        if (v.isEmpty()) return null
        val n = v.toIntOrNull()
        if (n == null || n !in (if (zero) 0 else 1)..1200) errors[path] = "Usa un mes o cantidad entera hasta 1.200."
        return n
    }
    money("cashGoal", d.cashGoal, true)
    d.context?.let {
        money("context.income", it.income)
        money("context.expenses", it.expenses)
        money("context.debts", it.debts)
    }
    d.offers.flatMap { it.fees }.filter { it.existing }.groupBy { it.id }.forEach { (id, fees) ->
        if (fees.distinct().size != 1) errors["context." + id] = "Una obligación común debe conservar importe, calendario y cobertura únicos."
    }
    if (d.referenceMonth.isNotEmpty() && !Regex("^[0-9]{4}-(0[1-9]|1[0-2])$").matches(d.referenceMonth))
        errors["referenceMonth"] = "Usa año-mes: AAAA-MM."
    if (d.offers.map { it.id } != listOf("A", "B")) errors["offers"] = "Se requieren las posiciones únicas A y B."
    d.offers.forEach { o ->
        if (o.name.length > 120) errors[o.id + ".name"] = "El nombre admite hasta 120 caracteres."
        origin(o.id + ".source", o.source, o.sourceDate)
        if (o.indicators.map { it.kind }.distinct().size != o.indicators.size) errors[o.id + ".indicators"] = "No repitas el mismo indicador."
        o.indicators.forEachIndexed { i, indicator ->
            val path = o.id + ".indicator." + i
            origin(path, indicator.value.source, indicator.value.date)
            if (indicator.value.text.isNotEmpty() && (!Regex("^[0-9]+(\\.[0-9]+)?$").matches(indicator.value.text) || indicator.value.text.length > 42))
                errors[path + ".value"] = "Usa el valor publicado sin símbolo ni texto; hasta 40 dígitos."
            if (indicator.unit.length > 120 || indicator.taxBase.length > 500) errors[path] = "Acorta unidad o base de impuestos."
        }
        money(o.id + ".price", o.price, true)
        money(o.id + ".downPayment", o.downPayment)
        money(o.id + ".discount", o.discount)
        if (d.need == Need.CASH && o.product != Product.LOAN) errors[o.id + ".product"] = "Las tarjetas se comparan como compra aislada."
        if (amountSafe(o.downPayment) != null && amountSafe(o.price) != null && amountSafe(o.downPayment)!! >= amountSafe(o.price)!!)
            errors[o.id + ".downPayment"] = "El aporte debe ser menor que el precio; se requiere financiación positiva."
        money(o.id + ".net", o.net, true)
        money(o.id + ".payment", o.payment)
        if ((o.fees.map { it.id } + o.insurance.map { it.id }).distinct().size != o.fees.size + o.insurance.size ||
            o.fees.any { it.id.isBlank() }) errors[o.id + ".fees"] = "Los cargos y seguros requieren identidades distintas."
        o.fees.forEachIndexed { i, f ->
            val path = o.id + ".fee." + i
            money(path + ".amount", f.amount)
            if (f.existing && f.treatment in listOf(FeeTreatment.FINANCED, FeeTreatment.DEDUCTED))
                errors[path + ".treatment"] = "Un cargo preexistente no puede financiarse o descontarse de esta nueva operación."
            val start = month(path + ".start", f.start, true)
            val end = month(path + ".end", f.end, true)
            if (start == null && f.treatment in listOf(FeeTreatment.SEPARATE, FeeTreatment.INCLUDED))
                errors[path + ".start"] = "Indica el mes del cargo."
            if (f.recurring && f.treatment !in listOf(FeeTreatment.SEPARATE, FeeTreatment.INCLUDED))
                errors[path + ".end"] = "Solo los cargos aparte o incluidos se repiten."
            if (end != null && start != null && end < start) errors[path + ".end"] = "El final debe ser posterior o igual al comienzo."
        }
        money(o.id + ".principal", o.principal, true)
        if (o.rate.text.isNotEmpty() && (!Regex("^[0-9]+(\\.[0-9]+)?$").matches(o.rate.text) || o.rate.text.length > 42))
            errors[o.id + ".rate"] = "Usa una tasa porcentual no negativa, hasta 40 dígitos; CFT/VTUA no son interés."
        if (o.frequency.isNotEmpty() && o.frequency.toIntOrNull() !in 1..1200)
            errors[o.id + ".frequency"] = "La frecuencia debe ser un entero de 1 a 1.200."
        if (o.mode == OfferMode.SIMULATION && o.payment.text.isNotEmpty())
            money(o.id + ".payment", o.payment, true)
        if (o.insurance.map { it.id }.distinct().size != o.insurance.size || o.insurance.any { it.id.isBlank() })
            errors[o.id + ".insurance"] = "Identificadores de seguro únicos y no vacíos."
        o.insurance.forEachIndexed { i, insurance ->
            if (insurance.name.isBlank() || insurance.name.length > 120) errors[o.id + ".insurance." + i + ".name"] = "Indica un nombre de hasta 120 caracteres."
            if (insurance.percentage) {
                origin(o.id + ".insurance." + i + ".value", insurance.value.source, insurance.value.date)
                if (insurance.value.text.isNotEmpty() && (!Regex("^[0-9]+(\\.[0-9]+)?$").matches(insurance.value.text) || insurance.value.text.length > 42))
                    errors[o.id + ".insurance." + i + ".value"] = "Usa un porcentaje no negativo de hasta 40 dígitos."
            } else money(o.id + ".insurance." + i + ".value", insurance.value)
            if (!insurance.endsAtPayoff) {
                month(o.id + ".insurance." + i + ".endMonth", insurance.endMonth)
            }
        }
        val n = month(o.id + ".count", o.count)
        val start = month(o.id + ".firstMonth", o.firstMonth, true) ?: 1
        if (n != null && start + n - 1 > 1200) errors[o.id + ".count"] = "El calendario excede el mes 1.200."
        if (o.exceptions.map { it.month.toIntOrNull() }.distinct().size != o.exceptions.size) errors[o.id + ".exceptions"] = "No repitas meses."
        o.exceptions.forEachIndexed { i, e ->
            val path = o.id + ".exception." + i
            val m = month(path + ".month", e.month, true)
            money(path + ".amount", e.amount)
            if (m == null || n == null || m !in start until start + n)
                errors[path + ".month"] = "El mes debe pertenecer al calendario de cuotas."
        }
    }
    return errors
}
private fun feeActive(f: Fee, month: Int): Boolean {
    if (f.treatment == FeeTreatment.INITIAL) return month == 0
    val start = f.start.toIntOrNull() ?: return false
    return if (f.recurring) month >= start && (f.end.toIntOrNull()?.let { month <= it } ?: true) else month == start
}
internal fun calculate(d: ComparisonDraft): ComparisonResult = calculateAtHorizon(d, 0)
private fun calculateAtHorizon(d: ComparisonDraft, minimumHorizon: Int): ComparisonResult {
    val offers = d.offers.map { o ->
        val n = o.count.toIntOrNull()
        val first = o.firstMonth.toIntOrNull() ?: 1
        val canSimulate = !o.requiresPaymentReview && o.mode == OfferMode.SIMULATION && n != null && amount(o.principal) != null &&
            o.rate.text.isNotEmpty() && (o.rateKind != RateKind.NOMINAL || o.frequency.isNotEmpty()) &&
            o.insurance.all { it.value.text.isNotEmpty() && (it.endsAtPayoff || it.endMonth.isNotEmpty()) }
        val simulation = if (canSimulate) simulateLoan(Scenario(
            name = o.name.ifBlank { o.id }, principal = o.principal.text, startDate = "2000-01",
            currency = d.currency ?: Currency.COP, termMonths = n!!,
            rate = when (o.rateKind) {
                RateKind.EM -> Rate.Monthly((Decimal.parse(o.rate.text) / Decimal.fromInt(100)).toString())
                RateKind.EA -> Rate.Annual((Decimal.parse(o.rate.text) / Decimal.fromInt(100)).toString())
                RateKind.NOMINAL -> Rate.Nominal((Decimal.parse(o.rate.text) / Decimal.fromInt(100)).toString(), o.frequency.toInt())
            },
            payment = o.payment.text.ifEmpty { null },
            insurance = o.insurance.map {
                Insurance(it.id, it.name, if (it.percentage) "percentage" else "fixed",
                    if (it.percentage) (Decimal.parse(it.value.text) / Decimal.fromInt(100)).toString() else it.value.text,
                    it.endsAtPayoff, it.endMonth.toIntOrNull())
            }
        )) else null
        if (simulation != null) require((simulation.rows.lastOrNull()?.month ?: 0) + first - 1 <= 1200) { "El calendario calculado excede el mes 1.200." }
        val paymentRows = if (o.mode == OfferMode.SIMULATION) simulation?.rows?.map {
            CashRow(it.month + first - 1, it.cashPaid, interest = it.interestCharged,
                principal = it.scheduled.principal, insurance = it.insuranceCharged, balance = it.closingBalance)
        } ?: emptyList() else if (n == null) emptyList() else (first until first + n).map { m ->
            CashRow(m, amount(o.exceptions.firstOrNull { it.month.toIntOrNull() == m }?.amount ?: o.payment)?.toFixed())
        }

        val warnings = mutableListOf<String>()
        val principal = amount(o.principal)
        val reductions = o.fees.filter { !it.existing && it.treatment in listOf(FeeTreatment.FINANCED, FeeTreatment.DEDUCTED) }
        val reductionsKnown = reductions.all { amount(it.amount) != null }
        val derivedNet = if (principal != null && reductionsKnown)
            principal - reductions.fold(Decimal.ZERO) { a, f -> a + amount(f.amount)!! } else null
        val net = if (d.need == Need.PURCHASE) amount(o.price)
            else amount(o.net) ?: derivedNet ?: if (reductions.isEmpty()) amount(d.cashGoal) else null
        val expectedPrincipal = if (d.need == Need.PURCHASE && amount(o.price) != null && amount(o.downPayment) != null && reductionsKnown)
            amount(o.price)!! - amount(o.downPayment)!! + reductions.fold(Decimal.ZERO) { a, f -> a + amount(f.amount)!! } else null
        var accounting = if (d.need == Need.PURCHASE)
            expectedPrincipal != null && expectedPrincipal.isPositive && (principal == null || principal.compareTo(expectedPrincipal) == 0)
            else net != null && net.isPositive && (derivedNet == null || amount(o.net) == null || derivedNet.compareTo(amount(o.net)!!) == 0)
        if (simulation != null && !simulation.complete) warnings.add("Modelo incompleto. Deuda remanente: " + simulation.remainingDebt)
        if (simulation?.payoffMonth != null && o.payment.text.isNotEmpty() && simulation.payoffMonth != n) {
            accounting = false
            warnings.add("La cuota conocida no coincide con el plazo declarado; el modelo liquida en el mes " + simulation.payoffMonth + ". Revisa cuota y plazo.")
        }
        if (!accounting) warnings.add("Revisa capital, cargos y efectivo neto; no se ajustaron tus datos.")
        val allFeesKnown = o.fees.filter { !it.existing || it.treatment == FeeTreatment.INCLUDED }.all { !it.needsReview && amount(it.amount) != null && (!it.recurring || it.end.isNotEmpty()) }
        val ownHorizon = maxOf(paymentRows.maxOfOrNull { it.month } ?: 0,
            o.fees.filter { it.treatment == FeeTreatment.SEPARATE || it.treatment == FeeTreatment.INCLUDED }.maxOfOrNull {
                if (it.recurring) it.end.toIntOrNull() ?: it.start.toIntOrNull() ?: 0 else it.start.toIntOrNull() ?: 0
            } ?: 0)
        val horizon = maxOf(ownHorizon, minimumHorizon)
        val commonExisting = d.offers.flatMap { it.fees }.filter { it.existing }.distinctBy { it.id }
        val rows = (0..horizon).map { month ->
            val p = paymentRows.firstOrNull { it.month == month }
            val fees = o.fees.filter { feeActive(it, month) }
            val initial = fees.filter { !it.existing && it.treatment == FeeTreatment.INITIAL }.fold(if (month == 0 && d.need == Need.PURCHASE) amount(o.downPayment) ?: Decimal.ZERO else Decimal.ZERO) { a, f -> a + (amount(f.amount) ?: Decimal.ZERO) }
            val extra = fees.filter { !it.existing && it.treatment == FeeTreatment.SEPARATE }.fold(Decimal.ZERO) { a, f -> a + (amount(f.amount) ?: Decimal.ZERO) }
            val basePayment = p?.payment ?: if (p == null && n != null && (o.mode == OfferMode.QUOTE || simulation?.complete == true)) "0.00" else null
            val includedFees = fees.filter { it.treatment == FeeTreatment.INCLUDED }
            val includedAmount = includedFees.fold(Decimal.ZERO) { a, f -> a + (amount(f.amount) ?: Decimal.ZERO) }
            val payment = if (basePayment != null && o.mode == OfferMode.SIMULATION)
                (Decimal.parse(basePayment) + includedAmount).toFixed() else basePayment
            if (o.mode == OfferMode.QUOTE && payment != null)
                require(includedAmount <= Decimal.parse(payment)) { "Los cargos incluidos superan la cuota del mes " + month }
            val includedExisting = fees.filter { it.existing && it.treatment == FeeTreatment.INCLUDED }
            val excluded = if (includedExisting.all { amount(it.amount) != null })
                includedExisting.fold(Decimal.ZERO) { a, f -> a + amount(f.amount)!! } else null
            if (excluded != null && payment != null) require(excluded <= Decimal.parse(payment)) { "El cargo preexistente incluido supera la cuota en el mes " + month }
            val uncertain = !o.chargesConfirmed || o.requiresPaymentReview || fees.filter { !it.existing || it.treatment == FeeTreatment.INCLUDED }
                .any { it.needsReview || amount(it.amount) == null || (it.recurring && it.end.isEmpty()) }
            val cash = (if (excluded != null && payment != null) Decimal.parse(payment) - excluded else Decimal.ZERO) + initial + extra
            val context = d.context
            val common = commonExisting.filter { feeActive(it, month) }
            val margin = if (context != null && payment != null && excluded != null && !uncertain &&
                listOf(context.income, context.expenses, context.debts).all { amount(it) != null } &&
                common.all { it.coverage != Coverage.UNKNOWN && amount(it.amount) != null && (!it.recurring || it.end.isNotEmpty()) }) {
                val baseline = common.filter { it.coverage == Coverage.ADD }.fold(Decimal.ZERO) { a, f -> a + amount(f.amount)!! }
                (amount(context.income)!! - amount(context.expenses)!! - amount(context.debts)!! - baseline -
                    cash).toFixed()
            } else null
            (p ?: CashRow(month, payment)).copy(payment = payment, initial = initial.toFixed(), extra = extra.toFixed(),
                uncertain = uncertain, excludedPreexisting = excluded?.toFixed(), cash = cash.toFixed(), installment = p != null && (o.mode == OfferMode.QUOTE || simulation?.payoffMonth == null || month <= simulation.payoffMonth + first - 1), margin = margin)

        }
        val total = rows.fold(Decimal.ZERO) { a, r -> a + Decimal.parse(r.cash) }
        val repayments = paymentRows.fold(Decimal.ZERO) { a, r -> a + Decimal.parse(r.payment ?: "0") }
        if ((principal ?: expectedPrincipal) != null && o.mode == OfferMode.QUOTE && paymentRows.all { it.payment != null } && repayments < (principal ?: expectedPrincipal)!!) {
            accounting = false
            warnings.add("Las cuotas no reconcilian el capital; revisa el calendario. No se infiere una subvención o saldo perdonado.")
        }
        val complete = n != null && paymentRows.isNotEmpty() && paymentRows.all { it.payment != null } &&
            o.chargesConfirmed && !o.requiresPaymentReview && allFeesKnown && accounting && (o.mode == OfferMode.QUOTE || simulation?.complete == true)
        if (o.requiresPaymentReview) warnings.add("Cotización modificada: confirma pagos actualizados o pasa a simulación. Las cuotas no se escalaron.")
        if (o.fees.any { it.needsReview }) warnings.add("Revisa importes de cargos al cambiar sus condiciones; no se arrastran como confirmados.")
        if (!complete) warnings.add("Subtotal de pagos conocidos; falta confirmar cuotas, neto o cargos. No es costo de vida completo.")
        OfferResult(o.id, rows, total.toFixed(), if (complete) (total - net!!).toFixed() else null,
            complete, o.firstMonth.isEmpty() || d.referenceMonth.isEmpty() ||
                (listOf(o.payment, o.net, o.principal, o.rate, o.price, o.downPayment, o.discount, d.cashGoal) + o.exceptions.map { it.amount } +
                    o.insurance.map { it.value } + o.fees.map { it.amount } + (d.context?.let { listOf(it.income, it.expenses, it.debts) } ?: emptyList())).any { it.provenance == Provenance.ESTIMATED },
            warnings, rows[0].initial, net?.toFixed(), rows.fold(Decimal.ZERO) { a, row -> a + Decimal.parse(row.payment ?: "0") }.toFixed(), if (n == null) null else ownHorizon)
    }
    val actualHorizon = offers.flatMap { it.rows }.maxOfOrNull { it.month } ?: 0
    if (actualHorizon > minimumHorizon) return calculateAtHorizon(d, actualHorizon)
    val equivalent = d.currency != null && d.country != null &&
        d.offers.all { it.currency == null || it.currency == d.currency } &&
        if (d.need == Need.PURCHASE) d.purchase.isNotBlank() && d.offers.all { amount(it.price) != null }
        else amount(d.cashGoal) != null && offers.all { it.net != null && Decimal.parse(it.net).compareTo(amount(d.cashGoal)!!) == 0 }
    val differences = if (equivalent && offers.all { it.complete }) mapOf(
        "Total pagado" to (Decimal.parse(offers[1].knownTotal) - Decimal.parse(offers[0].knownTotal)).toFixed(),
        "Costo nominal" to (Decimal.parse(offers[1].financeCost!!) - Decimal.parse(offers[0].financeCost!!)).toFixed(),
        "Salida inicial" to (Decimal.parse(offers[1].initial) - Decimal.parse(offers[0].initial)).toFixed(),
        "Último mes" to ((offers[1].lastMonth ?: 0) - (offers[0].lastMonth ?: 0)).toString()
    ) else emptyMap()
    val priceDiff = if (equivalent && d.need == Need.PURCHASE) mapOf("Precio" to (amount(d.offers[1].price)!! - amount(d.offers[0].price)!!).toFixed()) else emptyMap()
    return ComparisonResult(offers, offers.flatMap { it.rows }.maxOfOrNull { it.month } ?: 0, equivalent, differences + priceDiff)
}

const val COMPARISON_STORAGE_KEY = "finsim.comparison.v1"
@Serializable private data class ComparisonDocument(
    val type: String, val version: Int, val modelVersion: String,
    val applied: ComparisonDraft?, val draft: ComparisonDraft, val reference: ComparisonDraft?
)
private val comparisonJson = Json { encodeDefaults = true }
class ComparisonWorkspace(private val storage: ScenarioStorage) {
    var state = ComparisonState()
        private set
    init {
        try {
            storage.getItem(COMPARISON_STORAGE_KEY)?.let {
                val doc = decode(it)
                state = ComparisonState(draft = doc.draft, applied = doc.applied, result = doc.applied?.let(::calculate), reference = doc.reference)
            }
        } catch (e: Exception) { state = state.copy(message = "No se pudo recuperar el borrador: " + e.message) }
    }
    private fun decode(text: String): ComparisonDocument {
        require(text.encodeToByteArray().size <= 2_000_000) { "El archivo supera 2 MB." }
        val doc = try { comparisonJson.decodeFromString(ComparisonDocument.serializer(), text) }
            catch (e: Exception) { throw IllegalArgumentException("JSON de comparación inválido. Los préstamos anteriores se abren en Préstamo.", e) }
        require(doc.type == "financing-comparison" && doc.version == 1 && doc.modelVersion == "monthly-v1") { "Tipo, versión o reglas no compatibles." }
        require(validateDraft(doc.draft).isEmpty() && (doc.applied == null || validateDraft(doc.applied).isEmpty()) && (doc.reference == null || validateDraft(doc.reference).isEmpty())) { "El borrador contiene campos inválidos." }
        return doc
    }
    fun exportJson(): Result<String> = runCatching {
        require(validateDraft(state.draft).isEmpty()) { "Corrige las ediciones inválidas antes de guardar o exportar; puedes dejar datos vacíos." }
        val text = comparisonJson.encodeToString(ComparisonDocument.serializer(),
            ComparisonDocument("financing-comparison", 1, "monthly-v1", state.applied, state.draft, state.reference))
        require(text.encodeToByteArray().size <= 2_000_000) { "El archivo supera 2 MB." }
        text
    }
    fun save(): Boolean = try {
        val text = exportJson().getOrThrow()
        storage.setItem(COMPARISON_STORAGE_KEY, text)
        state = state.copy(isDirty = false, message = "Borrador guardado; los pendientes no se aplicaron.")
        true
    } catch (e: Exception) {
        state = state.copy(message = "No se pudo guardar. " + e.message)
        false
    }
    fun importJson(text: String): Boolean = try {
        val doc = decode(text)
        val result = doc.applied?.let(::calculate)
        state = ComparisonState(doc.draft, doc.applied, result, isDirty = true, message = "Importado; revisa y guarda explícitamente.", reference = doc.reference)
        true
    } catch (e: Exception) {
        state = state.copy(message = e.message ?: "No se pudo importar.")
        false
    }
    fun edit(draft: ComparisonDraft) {
        val previous = state.draft
        val globalChange = previous.country != draft.country || previous.currency != draft.currency ||
            previous.need != draft.need || previous.cashGoal.text != draft.cashGoal.text
        val reviewedDraft = if (state.applied == null) draft else draft.copy(offers = draft.offers.map { next ->
            val old = previous.offers.firstOrNull { it.id == next.id }
            val baseChange = globalChange || (old != null && (old.product != next.product || old.principal.text != next.principal.text ||
                old.rate.text != next.rate.text || old.rateKind != next.rateKind || old.frequency != next.frequency ||
                old.mode != next.mode || old.count != next.count || old.price.text != next.price.text || old.downPayment.text != next.downPayment.text || old.net.text != next.net.text))
            if (!baseChange) next else next.copy(
                requiresPaymentReview = next.mode == OfferMode.QUOTE || (old?.mode != next.mode && next.payment.text.isNotEmpty()),
                fees = next.fees.map { if (it.existing) it else it.copy(needsReview = true) })
        })
        state = state.copy(draft = reviewedDraft, isDirty = true, errors = validateDraft(reviewedDraft))
    }
    fun recordReference(): Boolean {
        if (!apply()) return false
        state = state.copy(reference = state.applied, message = "Referencia registrada explícitamente.")
        return true
    }
    fun apply(): Boolean {
        val errors = validateDraft(state.draft)
        if (errors.isNotEmpty()) { state = state.copy(errors = errors); return false }
        return try {
            val result = calculate(state.draft)
            state = state.copy(applied = state.draft, result = result, errors = emptyMap(), isDirty = true, reference = state.reference ?: state.draft, message = "Comparación aplicada.")
            true
        } catch (e: Exception) { state = state.copy(message = e.message ?: "No se pudo calcular."); false }
    }
    fun discard() {
        val restored = state.applied ?: ComparisonDraft()
        state = state.copy(draft = restored, errors = emptyMap(), isDirty = state.isDirty || state.draft != restored)
    }
}
