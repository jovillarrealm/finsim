package com.finsim.ingestion

import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.Insurance
import com.finsim.domain.Rate
import com.finsim.domain.Scenario
import com.finsim.reconciliation.StatementRow
import kotlinx.serialization.Serializable

@Serializable
enum class ImportTargetMode(val label: String) {
    SCENARIO("Escenario"),
    RECONCILIATION("Conciliación")
}

@Serializable
enum class ImportDocumentKind {
    LOAN_STATEMENT,
    ACCOUNT_SUMMARY,
    UNKNOWN
}

@Serializable
enum class BalanceMeaning {
    PRINCIPAL,
    COMPOUND_DEBT,
    UNKNOWN
}

@Serializable
enum class PaymentMeaning {
    BASE_PAYMENT,
    TOTAL_PAYMENT,
    UNKNOWN
}

@Serializable
enum class TermMeaning {
    ORIGINAL,
    REMAINING,
    UNKNOWN
}

@Serializable
enum class InsuranceAmountMeaning {
    PERIODIC_CHARGE,
    INSURED_VALUE,
    UNKNOWN
}

@Serializable
enum class InsurancePeriodicity(val label: String) {
    MONTHLY("mensual"),
    ANNUAL("anual"),
    OTHER("otra"),
    UNKNOWN("no identificada")
}

@Serializable
enum class ExtractionEngine(val label: String) {
    TIER_1_DIGITAL_TEXT("Texto digital"),
    TIER_2_LOCAL_VLM("Visión local VLM"),
    TIER_2_LOCAL_DOCLING("Visión local Granite-Docling")
}

/** Parses a user-reviewed monetary value without rounding away excess fractional digits. */
fun parseImportMoney(value: String, label: String): Decimal {
    val amount = Decimal.parse(value.trim().replace(',', '.'))
    require(amount.scale <= 2) {
        "$label debe tener como máximo dos decimales; no se redondeará automáticamente."
    }
    return amount
}

@Serializable
enum class RegulatoryFramework(
    val label: String,
    val defaultCurrency: Currency,
    val defaultRateKind: String
) {
    COLOMBIA_SFC("Colombia · SFC", Currency.COP, "annual"),
    MEXICO_CONDUSEF("México · CONDUSEF", Currency.COP, "annual"),
    ARGENTINA_BCRA("Argentina · BCRA", Currency.ARS, "nominal"),
    CHILE_CMF("Chile · CMF", Currency.USD, "annual"),
    GENERIC("Genérico", Currency.COP, "annual")
}

@Serializable
data class ExtractedRate(
    val valuePercent: String,
    val kind: String, // "annual", "monthly", "nominal"
    val nominalFrequency: Int? = null
)

@Serializable
data class ExtractedInsurance(
    val name: String,
    val monthlyCost: String? = null,
    val amountMeaning: InsuranceAmountMeaning = InsuranceAmountMeaning.UNKNOWN,
    val periodicity: InsurancePeriodicity = InsurancePeriodicity.UNKNOWN
)

@Serializable
data class ExtractedDocumentData(
    val sourceEngine: ExtractionEngine = ExtractionEngine.TIER_1_DIGITAL_TEXT,
    val analyzedPage: Int = 1,
    val bankNameCandidate: String? = null,
    val statementMonth: Int? = null,
    val statementCutDate: String? = null,
    val closingBalance: String? = null,
    val originalPrincipal: String? = null,
    val termMonths: Int? = null,
    val rate: ExtractedRate? = null,
    val monthlyPayment: String? = null,
    val principalPaid: String? = null,
    val interestCharged: String? = null,
    val totalInsurance: String? = null,
    val insurances: List<ExtractedInsurance> = emptyList(),
    val rawExtractedText: String? = null,
    val confidence: Float = 1.0f,
    val documentKind: ImportDocumentKind = ImportDocumentKind.UNKNOWN,
    val detectedCurrencies: Set<Currency> = emptySet(),
    val hasUnsupportedCurrency: Boolean = false,
    val capitalBalance: String? = null,
    val compoundBalance: String? = null,
    val balanceMeaning: BalanceMeaning = BalanceMeaning.UNKNOWN,
    val unclassifiedPrincipal: String? = null,
    val originalTermMonths: Int? = null,
    val remainingTermMonths: Int? = null,
    val basePayment: String? = null,
    val totalPayment: String? = null,
    val paymentMeaning: PaymentMeaning = PaymentMeaning.UNKNOWN,
    val termMeaning: TermMeaning = TermMeaning.UNKNOWN,
    /** Unresolved conflicts or interpretation limits that must be shown during human review. */
    val reviewWarnings: List<String> = emptyList()
) {
    val engine: ExtractionEngine get() = sourceEngine

    /** Explains why the source cannot be used for a loan target or its selected currency. */
    fun importBlockReason(
        expectedCurrency: Currency? = null,
        confirmCurrencyAssumption: Boolean = false
    ): String? = when {
        hasUnsupportedCurrency -> "El documento usa una moneda que el simulador no admite."
        detectedCurrencies.size > 1 -> "El documento mezcla monedas; no se puede usar para un préstamo ni para conciliarlo."
        documentKind == ImportDocumentKind.ACCOUNT_SUMMARY -> "Un resumen de cuenta no es un extracto de préstamo y no se puede importar ni conciliar como préstamo."
        documentKind == ImportDocumentKind.UNKNOWN -> "Confirma que el documento corresponde a un extracto de préstamo antes de continuar."
        expectedCurrency != null && detectedCurrencies.singleOrNull()?.let { it != expectedCurrency } == true ->
            "La moneda del extracto no coincide con la del escenario activo (${expectedCurrency.name})."
        expectedCurrency != null && detectedCurrencies.isEmpty() && !confirmCurrencyAssumption ->
            "Confirma que los importes del documento están en ${expectedCurrency.name}."
        else -> null
    }

    /** Applies an explicit reviewer confirmation to an otherwise unclassified document. */
    fun withLoanStatementConfirmation(confirmed: Boolean): ExtractedDocumentData =
        if (documentKind == ImportDocumentKind.UNKNOWN && confirmed) {
            copy(documentKind = ImportDocumentKind.LOAN_STATEMENT)
        } else this

    /** Maps an explicitly reviewed monthly row. Missing month stays missing unless supplied by the reviewer. */
    fun toStatementRow(
        fallbackMonth: Int? = null,
        expectedCurrency: Currency? = null,
        reviewedCurrency: Currency? = null,
        confirmCurrencyAssumption: Boolean = false
    ): StatementRow {
        val blockReason = importBlockReason(expectedCurrency, confirmCurrencyAssumption)
        require(blockReason == null) { blockReason ?: "Documento no compatible." }
        val m = statementMonth ?: fallbackMonth
            ?: throw IllegalArgumentException("Indica el mes del extracto; no se asignará uno automáticamente.")
        require(m in 1..1200) { "El mes a conciliar debe estar entre 1 y 1200." }
        val detectedCurrency = detectedCurrencies.singleOrNull()
        val currency = reviewedCurrency ?: detectedCurrency
            ?: throw IllegalArgumentException("Indica y confirma la moneda de los importes revisados.")
        require(detectedCurrency == currency || confirmCurrencyAssumption) {
            "Confirma la moneda seleccionada; no se detectó esa moneda en el documento."
        }
        require(expectedCurrency == null || currency == expectedCurrency) {
            "La moneda del extracto no coincide con la del escenario activo (${expectedCurrency?.name})."
        }

        val insuranceFromCharges = insurances
            .filter { it.amountMeaning == InsuranceAmountMeaning.PERIODIC_CHARGE && it.monthlyCost != null }
            .takeIf { it.isNotEmpty() }
            ?.let { items ->
                items.fold(Decimal.ZERO) { acc, item ->
                    acc + parseImportMoney(item.monthlyCost!!, "El cargo periódico de seguro")
                }.toFixed(2)
            }
        val insurance = totalInsurance?.let { parseImportMoney(it, "El seguro total") }?.toFixed(2)
            ?: insuranceFromCharges

        return StatementRow(
            month = m,
            currency = currency,
            interest = interestCharged?.let { parseImportMoney(it, "El interés cobrado").toFixed(2) },
            insurance = insurance,
            principal = principalPaid?.let { parseImportMoney(it, "El capital pagado").toFixed(2) },
            balance = (capitalBalance.takeIf { balanceMeaning == BalanceMeaning.PRINCIPAL }
                ?: closingBalance.takeIf { balanceMeaning == BalanceMeaning.PRINCIPAL })
                ?.let { parseImportMoney(it, "El saldo final").toFixed(2) }
        ).also { it.validate() }
    }

    /**
     * Maps reviewed candidates into a [Scenario]. Ambiguous or absent facts remain absent and must be
     * supplied by the reviewer; the optional base scenario is only used when a caller explicitly passes it.
     */
    fun toScenario(
        baseScenario: Scenario? = null,
        framework: RegulatoryFramework = RegulatoryFramework.COLOMBIA_SFC,
        customName: String? = null,
        startDate: String? = null,
        confirmNoInsurance: Boolean = false,
        confirmCalculatedPayment: Boolean = false,
        confirmCurrencyAssumption: Boolean = false,
        reviewedCurrency: Currency? = null
    ): Scenario {
        val blockReason = importBlockReason()
        require(blockReason == null) { blockReason ?: "Documento no compatible." }

        val currentPrincipal = if (balanceMeaning == BalanceMeaning.PRINCIPAL) {
            capitalBalance ?: closingBalance
        } else null
        val isCurrent = currentPrincipal != null
        val principal = currentPrincipal ?: originalPrincipal ?: baseScenario?.principal
            ?: throw IllegalArgumentException("Indica el capital del préstamo; un saldo compuesto no sirve como capital.")
        val source = when {
            isCurrent -> "current"
            originalPrincipal != null -> "original"
            baseScenario != null -> baseScenario.source
            else -> throw IllegalArgumentException("Confirma si el capital indicado corresponde al saldo actual o al monto original.")
        }

        val reviewedTerm = when (source) {
            "current" -> remainingTermMonths ?: termMonths.takeIf { termMeaning == TermMeaning.REMAINING }
            else -> originalTermMonths ?: termMonths.takeIf { termMeaning == TermMeaning.ORIGINAL }
        }
        val months = reviewedTerm ?: baseScenario?.termMonths
            ?: throw IllegalArgumentException(
                if (source == "current") "Indica el plazo restante; el plazo original no se convertirá automáticamente."
                else "Indica el plazo original del préstamo."
            )

        val detectedCurrency = detectedCurrencies.singleOrNull()
        val currency = reviewedCurrency ?: detectedCurrency ?: baseScenario?.currency
            ?: framework.defaultCurrency
        require(detectedCurrency == currency || confirmCurrencyAssumption) {
            "Confirma la moneda seleccionada (${currency.name}); no se detectó esa moneda en el documento."
        }

        val insuranceCharges = insurances.filter {
            it.amountMeaning == InsuranceAmountMeaning.PERIODIC_CHARGE && it.monthlyCost != null
        }
        require(insuranceCharges.isNotEmpty() || confirmNoInsurance) {
            "Confirma que el documento no informa cargos periódicos de seguro."
        }

        val reviewedPayment = basePayment
            ?: monthlyPayment.takeIf { paymentMeaning == PaymentMeaning.BASE_PAYMENT }
        val payment = reviewedPayment ?: baseScenario?.payment
        require(payment != null || confirmCalculatedPayment) {
            "Confirma que la cuota base se calculará con el capital, la tasa y el plazo revisados."
        }
        val principalAmount = parseImportMoney(principal, "El capital del préstamo")
        val paymentAmount = payment?.let { parseImportMoney(it, "La cuota base") }
        val insuranceAmounts = insuranceCharges.map {
            parseImportMoney(it.monthlyCost!!, "El cargo periódico de seguro")
        }

        val scenarioRate = if (rate == null) {
            baseScenario?.rate
                ?: throw IllegalArgumentException("Indica la tasa y su convención; no se asignará una tasa de ejemplo.")
        } else {
            val rateRatio = Decimal.parse(rate.valuePercent.replace(',', '.')) / Decimal.parse("100")
            when (rate.kind.lowercase()) {
                "monthly" -> Rate.Monthly(rateRatio.toString())
                "nominal" -> Rate.Nominal(rateRatio.toString(), rate.nominalFrequency
                    ?: throw IllegalArgumentException("Indica la frecuencia de capitalización de la tasa nominal."))
                "annual" -> Rate.Annual(rateRatio.toString())
                else -> throw IllegalArgumentException("La convención de tasa detectada no es válida.")
            }
        }

        val periodStart = startDate ?: baseScenario?.startDate
            ?: throw IllegalArgumentException("Indica el primer periodo del escenario.")
        val name = customName?.ifBlank { null }
            ?: bankNameCandidate?.let { "$it · Préstamo importado" }
            ?: "Préstamo importado (${framework.label})"

        return Scenario(
            version = 1,
            currency = currency,
            name = name.take(120),
            source = source,
            principal = principalAmount.toFixed(2),
            startDate = periodStart,
            termMonths = months,
            rate = scenarioRate,
            payment = paymentAmount?.toFixed(2),
            insurance = insuranceCharges.mapIndexed { index, item ->
                Insurance(
                    id = "ins_${index + 1}",
                    name = item.name,
                    kind = "fixed",
                    value = insuranceAmounts[index].toFixed(2),
                    endsAtPayoff = true
                )
            },
            events = emptyList()
        ).also { it.validate() }
    }
}
