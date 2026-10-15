package com.finsim.ingestion

import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.Insurance
import com.finsim.domain.Rate
import com.finsim.domain.Scenario
import com.finsim.domain.exampleScenario
import com.finsim.reconciliation.StatementRow
import kotlinx.serialization.Serializable

@Serializable
enum class ImportTargetMode(val label: String) {
    SCENARIO("Escenario"),
    RECONCILIATION("Conciliación")
}

@Serializable
enum class ExtractionEngine(val label: String) {
    TIER_1_DIGITAL_TEXT("Texto digital"),
    TIER_2_LOCAL_VLM("Visión local VLM")
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
    val monthlyCost: String
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
    val confidence: Float = 1.0f
) {
    val engine: ExtractionEngine get() = sourceEngine

    /**
     * Maps extracted monthly charges into a validated [StatementRow] for reconciliation audit.
     */
    fun toStatementRow(fallbackMonth: Int = 1): StatementRow {
        val m = statementMonth ?: fallbackMonth
        val totIns = totalInsurance ?: if (insurances.isNotEmpty()) {
            try {
                insurances.fold(Decimal.fromInt(0)) { acc, item ->
                    acc + Decimal.parse(item.monthlyCost.replace(',', '.'))
                }.toFixed(2)
            } catch (_: Exception) {
                null
            }
        } else null

        val cleanPrincipal = principalPaid?.let {
            try { Decimal.parse(it.replace(',', '.')).toFixed(2) } catch (_: Exception) { null }
        }
        val cleanInterest = interestCharged?.let {
            try { Decimal.parse(it.replace(',', '.')).toFixed(2) } catch (_: Exception) { null }
        }
        val cleanBalance = closingBalance?.let {
            try { Decimal.parse(it.replace(',', '.')).toFixed(2) } catch (_: Exception) { null }
        }

        return StatementRow(
            month = m,
            interest = cleanInterest,
            insurance = totIns,
            principal = cleanPrincipal,
            balance = cleanBalance
        )
    }
    /**
     * Maps extracted document candidates into a validated [Scenario] instance.
     */
    fun toScenario(
        baseScenario: Scenario = exampleScenario,
        framework: RegulatoryFramework = RegulatoryFramework.COLOMBIA_SFC,
        customName: String? = null
    ): Scenario {
        val rawPrincipal = closingBalance ?: originalPrincipal ?: baseScenario.principal
        val principalCandidate = try {
            Decimal.parse(rawPrincipal.replace(',', '.')).toFixed(2)
        } catch (_: Exception) {
            baseScenario.principal
        }

        val parsedTerm = termMonths?.coerceIn(1, 1200) ?: baseScenario.termMonths
        val currency = framework.defaultCurrency
        val cutDate = statementCutDate?.trim() ?: baseScenario.startDate

        val parsedRate = if (rate != null) {
            val decRatio = try {
                val percentDec = Decimal.parse(rate.valuePercent.replace(',', '.'))
                (percentDec / Decimal.parse("100")).toString()
            } catch (_: Exception) {
                baseScenario.rate.value
            }
            when (rate.kind.lowercase()) {
                "monthly" -> Rate.Monthly(decRatio)
                "nominal" -> Rate.Nominal(decRatio, rate.nominalFrequency ?: 12)
                else -> Rate.Annual(decRatio)
            }
        } else {
            baseScenario.rate
        }

        val parsedInsurances = if (insurances.isNotEmpty()) {
            insurances.mapIndexed { index, ins ->
                val cost = try {
                    Decimal.parse(ins.monthlyCost.replace(',', '.')).toFixed(2)
                } catch (_: Exception) {
                    "0.00"
                }
                Insurance(
                    id = "ins_${index + 1}",
                    name = ins.name.ifBlank { "Seguro ${index + 1}" },
                    kind = "fixed",
                    value = cost,
                    endsAtPayoff = true
                )
            }
        } else {
            baseScenario.insurance
        }

        val name = customName?.ifBlank { null }
            ?: bankNameCandidate?.let { "$it · Préstamo importado" }
            ?: "Préstamo importado (${framework.label})"

        val validStartDate = if (Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(cutDate)) {
            cutDate
        } else {
            baseScenario.startDate
        }

        val candidatePayment = monthlyPayment?.trim()?.let {
            try {
                Decimal.parse(it.replace(',', '.')).toFixed(2)
            } catch (_: Exception) {
                null
            }
        }

        return Scenario(
            version = 1,
            currency = currency,
            name = name.take(120),
            source = "current",
            principal = principalCandidate,
            startDate = validStartDate,
            termMonths = parsedTerm,
            rate = parsedRate,
            payment = candidatePayment,
            insurance = parsedInsurances,
            events = emptyList()
        )
    }
}
