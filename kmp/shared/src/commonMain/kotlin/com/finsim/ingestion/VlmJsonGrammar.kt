package com.finsim.ingestion

import com.finsim.domain.Currency
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class VlmInsuranceJson(
    val name: String = "",
    val amount: String? = null,
    val amountMeaning: String? = null
)

@Serializable
data class VlmExtractionPayload(
    val bankName: String? = null,
    val documentKind: String? = null,
    val currencies: List<String> = emptyList(),
    val principal: String? = null,
    val originalPrincipal: String? = null,
    val capitalBalance: String? = null,
    val compoundBalance: String? = null,
    val balanceMeaning: String? = null,
    val ratePercentage: String? = null,
    val rateKind: String? = null,
    val nominalFrequency: Int? = null,
    val termMonths: Int? = null,
    val originalTermMonths: Int? = null,
    val remainingTermMonths: Int? = null,
    val termMeaning: String? = null,
    val statementMonth: Int? = null,
    val statementCutDate: String? = null,
    val monthlyPayment: String? = null,
    val basePayment: String? = null,
    val totalPayment: String? = null,
    val paymentMeaning: String? = null,
    val principalPaid: String? = null,
    val interestPaid: String? = null,
    val insurancePaid: String? = null,
    val finalBalance: String? = null,
    val insurances: List<VlmInsuranceJson> = emptyList()
)

/**
 * Constrained decoding schema and parser for local VLM (Gemma 4 E2B INT4) vision inferences.
 * Guarantees extraction of banking fields without markdown hallucinations or JSON fence noise.
 */
object VlmJsonGrammar {

    /**
     * Strict JSON schema definition for grammar/logit masking in local inference runtimes.
     */
    val JSON_SCHEMA_PROMPT: String = """
        {
          "type": "object",
          "description": "Los importes numéricos deben usar decimales JSON canónicos, sin separadores de miles. Las cadenas de importes pueden usar formato bancario solo cuando los separadores sean inequívocos; no redondear ni reinterpretar formatos ambiguos.",
          "properties": {
            "bankName": { "type": ["string", "null"] },
            "documentKind": { "type": ["string", "null"], "enum": ["LOAN_STATEMENT", "ACCOUNT_SUMMARY", "UNKNOWN", null] },
            "currencies": { "type": "array", "items": { "type": "string" } },
            "principal": { "type": ["string", "number", "null"] },
            "originalPrincipal": { "type": ["string", "number", "null"] },
            "capitalBalance": { "type": ["string", "number", "null"] },
            "compoundBalance": { "type": ["string", "number", "null"] },
            "balanceMeaning": { "type": ["string", "null"], "enum": ["PRINCIPAL", "COMPOUND_DEBT", "UNKNOWN", null] },
            "ratePercentage": { "type": ["string", "number", "null"] },
            "rateKind": { "type": ["string", "null"], "enum": ["annual", "monthly", "nominal", null] },
            "nominalFrequency": { "type": ["integer", "null"] },
            "termMonths": { "type": ["integer", "null"] },
            "originalTermMonths": { "type": ["integer", "null"] },
            "remainingTermMonths": { "type": ["integer", "null"] },
            "termMeaning": { "type": ["string", "null"], "enum": ["ORIGINAL", "REMAINING", "UNKNOWN", null] },
            "statementMonth": { "type": ["integer", "null"] },
            "statementCutDate": { "type": ["string", "null"] },
            "monthlyPayment": { "type": ["string", "number", "null"] },
            "basePayment": { "type": ["string", "number", "null"] },
            "totalPayment": { "type": ["string", "number", "null"] },
            "paymentMeaning": { "type": ["string", "null"], "enum": ["BASE_PAYMENT", "TOTAL_PAYMENT", "UNKNOWN", null] },
            "principalPaid": { "type": ["string", "number", "null"] },
            "interestPaid": { "type": ["string", "number", "null"] },
            "insurancePaid": { "type": ["string", "number", "null"] },
            "finalBalance": { "type": ["string", "number", "null"] },
            "insurances": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": { "type": "string" },
                  "amount": { "type": ["string", "number", "null"] },
                  "amountMeaning": { "type": ["string", "null"], "enum": ["PERIODIC_CHARGE", "INSURED_VALUE", "UNKNOWN", null] }
                },
                "required": ["name"]
              }
            }
          },
          "required": ["principal", "ratePercentage"]
        }
    """.trimIndent()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Cleans raw VLM text output by stripping markdown code blocks, backticks, or preamble.
     */
    fun cleanJson(raw: String): String {
        var text = raw.trim()

        if (text.contains("```")) {
            val fenceMatch = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE).find(text)
            if (fenceMatch != null) {
                text = fenceMatch.groupValues[1].trim()
            }
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }

        return text
    }

    /**
     * Parses raw string output from the local VLM into [ExtractedDocumentData].
     */
    fun parseVlmResponse(
        rawOutput: String,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode = ImportTargetMode.SCENARIO,
        pageIndex: Int = 1
    ): ExtractedDocumentData {
        val cleaned = cleanJson(rawOutput)
        val jsonElement = try {
            jsonParser.parseToJsonElement(cleaned)
        } catch (_: Exception) {
            return ExtractedDocumentData(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                analyzedPage = pageIndex,
                confidence = 0.0f,
                rawExtractedText = rawOutput
            )
        }

        if (jsonElement !is JsonObject) {
            return ExtractedDocumentData(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                analyzedPage = pageIndex,
                confidence = 0.0f,
                rawExtractedText = rawOutput
            )
        }

        val bankName = jsonElement.getStringOrNull("bankName")
        val principal = jsonElement.getMoneyOrNull("principal")
        val originalPrincipal = jsonElement.getMoneyOrNull("originalPrincipal")
        val capitalBalance = jsonElement.getMoneyOrNull("capitalBalance")
        val compoundBalance = jsonElement.getMoneyOrNull("compoundBalance")
        val finalBalance = jsonElement.getMoneyOrNull("finalBalance")
        val reportedBalanceMeaning = when (jsonElement.getStringOrNull("balanceMeaning")?.uppercase()) {
            "PRINCIPAL" -> BalanceMeaning.PRINCIPAL
            "COMPOUND_DEBT" -> BalanceMeaning.COMPOUND_DEBT
            else -> BalanceMeaning.UNKNOWN
        }
        val reviewedCapitalBalance = capitalBalance ?: principal.takeIf { reportedBalanceMeaning == BalanceMeaning.PRINCIPAL }
        val reviewedCompoundBalance = compoundBalance ?: finalBalance
            ?.takeIf { reportedBalanceMeaning == BalanceMeaning.COMPOUND_DEBT }
        val balanceMeaning = when {
            reportedBalanceMeaning == BalanceMeaning.PRINCIPAL && compoundBalance != null -> BalanceMeaning.UNKNOWN
            reportedBalanceMeaning == BalanceMeaning.COMPOUND_DEBT && (capitalBalance != null || principal != null) -> BalanceMeaning.UNKNOWN
            reportedBalanceMeaning != BalanceMeaning.UNKNOWN -> reportedBalanceMeaning
            reviewedCapitalBalance != null -> BalanceMeaning.PRINCIPAL
            reviewedCompoundBalance != null -> BalanceMeaning.COMPOUND_DEBT
            else -> BalanceMeaning.UNKNOWN
        }
        val ratePercent = jsonElement.getStringOrNull("ratePercentage")?.replace(',', '.')
        val rawRateKind = jsonElement.getStringOrNull("rateKind")?.lowercase()
        val rateKind = when (rawRateKind) {
            "monthly", "month" -> "monthly"
            "nominal" -> "nominal"
            "annual", "ea", "tea" -> "annual"
            else -> rawRateKind ?: "unknown"
        }
        val rate = if (!ratePercent.isNullOrBlank()) {
            ExtractedRate(
                valuePercent = ratePercent,
                kind = rateKind,
                nominalFrequency = jsonElement.getIntOrNull("nominalFrequency")
            )
        } else null

        val termMonths = jsonElement.getIntOrNull("termMonths")
        val originalTermMonths = jsonElement.getIntOrNull("originalTermMonths")
        val remainingTermMonths = jsonElement.getIntOrNull("remainingTermMonths")
        val reportedTermMeaning = when (jsonElement.getStringOrNull("termMeaning")?.uppercase()) {
            "ORIGINAL" -> TermMeaning.ORIGINAL
            "REMAINING" -> TermMeaning.REMAINING
            else -> TermMeaning.UNKNOWN
        }
        val termMeaning = when {
            reportedTermMeaning != TermMeaning.UNKNOWN -> reportedTermMeaning
            remainingTermMonths != null -> TermMeaning.REMAINING
            originalTermMonths != null -> TermMeaning.ORIGINAL
            else -> TermMeaning.UNKNOWN
        }
        val statementMonth = jsonElement.getIntOrNull("statementMonth")
        val statementCutDate = jsonElement.getStringOrNull("statementCutDate")
        val monthlyPayment = jsonElement.getMoneyOrNull("monthlyPayment")
        val basePayment = jsonElement.getMoneyOrNull("basePayment")
        val totalPayment = jsonElement.getMoneyOrNull("totalPayment")
        val reportedPaymentMeaning = when (jsonElement.getStringOrNull("paymentMeaning")?.uppercase()) {
            "BASE_PAYMENT" -> PaymentMeaning.BASE_PAYMENT
            "TOTAL_PAYMENT" -> PaymentMeaning.TOTAL_PAYMENT
            else -> PaymentMeaning.UNKNOWN
        }
        val paymentMeaning = when {
            reportedPaymentMeaning != PaymentMeaning.UNKNOWN -> reportedPaymentMeaning
            basePayment != null -> PaymentMeaning.BASE_PAYMENT
            totalPayment != null -> PaymentMeaning.TOTAL_PAYMENT
            else -> PaymentMeaning.UNKNOWN
        }
        val principalPaid = jsonElement.getMoneyOrNull("principalPaid")
        val interestPaid = jsonElement.getMoneyOrNull("interestPaid")
        val insurancePaid = jsonElement.getMoneyOrNull("insurancePaid")

        val insurancesList = mutableListOf<ExtractedInsurance>()
        val insurancesArray = jsonElement["insurances"] as? JsonArray
        if (insurancesArray != null) {
            for (item in insurancesArray) {
                if (item is JsonObject) {
                    val name = item.getStringOrNull("name").orEmpty()
                    val amountClean = item.getMoneyOrNull("amount")
                    val amountMeaning = when (item.getStringOrNull("amountMeaning")?.uppercase()) {
                        "PERIODIC_CHARGE" -> InsuranceAmountMeaning.PERIODIC_CHARGE
                        "INSURED_VALUE" -> InsuranceAmountMeaning.INSURED_VALUE
                        else -> InsuranceAmountMeaning.UNKNOWN
                    }
                    insurancesList.add(
                        ExtractedInsurance(name = name, monthlyCost = amountClean, amountMeaning = amountMeaning)
                    )
                }
            }
        }

        val currencyCodes = (jsonElement["currencies"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content?.uppercase() }.orEmpty()
        val currencies = currencyCodes.mapNotNull { code -> Currency.entries.firstOrNull { it.name == code } }.toSet()
        var confidence = 0.85f
        if (principal != null || finalBalance != null) confidence += 0.05f
        if (rate != null) confidence += 0.05f
        if (termMonths != null) confidence += 0.03f

        return ExtractedDocumentData(
            sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
            analyzedPage = pageIndex,
            bankNameCandidate = bankName,
            documentKind = when (jsonElement.getStringOrNull("documentKind")?.uppercase()) {
                "LOAN_STATEMENT" -> ImportDocumentKind.LOAN_STATEMENT
                "ACCOUNT_SUMMARY" -> ImportDocumentKind.ACCOUNT_SUMMARY
                else -> ImportDocumentKind.UNKNOWN
            },
            detectedCurrencies = currencies,
            hasUnsupportedCurrency = currencyCodes.any { code -> Currency.entries.none { it.name == code } },
            statementMonth = statementMonth,
            statementCutDate = statementCutDate,
            closingBalance = reviewedCapitalBalance ?: reviewedCompoundBalance ?: finalBalance,
            originalPrincipal = originalPrincipal,
            termMonths = termMonths,
            rate = rate,
            monthlyPayment = monthlyPayment,
            principalPaid = principalPaid,
            interestCharged = interestPaid,
            totalInsurance = insurancePaid,
            insurances = insurancesList,
            rawExtractedText = rawOutput,
            confidence = confidence.coerceIn(0.0f, 1.0f),
            capitalBalance = reviewedCapitalBalance,
            compoundBalance = reviewedCompoundBalance,
            balanceMeaning = balanceMeaning,
            unclassifiedPrincipal = principal.takeIf {
                originalPrincipal == null && reportedBalanceMeaning == BalanceMeaning.UNKNOWN &&
                    capitalBalance == null && compoundBalance == null
            },
            originalTermMonths = originalTermMonths,
            remainingTermMonths = remainingTermMonths,
            basePayment = basePayment,
            totalPayment = totalPayment,
            paymentMeaning = paymentMeaning,
            termMeaning = termMeaning
        )
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val el = get(key) ?: return null
        if (el is JsonPrimitive) {
            if (el.content == "null") return null
            return el.content.trim()
        }
        return null
    }

    private fun JsonObject.getMoneyOrNull(key: String): String? = get(key)?.let { element ->
        when (element) {
            JsonNull -> null
            is JsonPrimitive -> if (element.isString) {
                parseVlmMoneyText(element.content, key)
            } else {
                parseCanonicalJsonMoney(element.content, key)
            }
            else -> throw IllegalArgumentException("El importe JSON '$key' debe ser un número o una cadena.")
        }
    }

    /** JSON numeric values are canonical decimal lexemes; never route them through the bank-text cleaner. */
    private fun parseCanonicalJsonMoney(raw: String, key: String): String {
        require(CANONICAL_JSON_DECIMAL.matches(raw)) {
            "El importe JSON '$key' debe usar un decimal canónico sin separadores localizados ni exponentes."
        }
        return parseImportMoney(raw, "El importe JSON '$key'").toFixed(2)
    }

    /**
     * VLM strings can contain a bank's explicit grouping/decimal convention. A lone separator with
     * three trailing digits is ambiguous (for example, 1.005) and needs human review.
     */
    private fun parseVlmMoneyText(raw: String, key: String): String? {
        val text = raw
            .replace("$", "")
            .replace(CURRENCY_CODE, "")
            .trim()
        if (text.isEmpty()) return null

        val canonical = when {
            PLAIN_INTEGER.matches(text) -> text
            PLAIN_DOT_DECIMAL.matches(text) -> text
            PLAIN_COMMA_DECIMAL.matches(text) -> text.replace(',', '.')
            GROUPED_COMMA_DECIMAL.matches(text) -> text.replace(",", "")
            GROUPED_DOT_DECIMAL.matches(text) -> text.replace(".", "").replace(',', '.')
            GROUPED_COMMA_INTEGER.matches(text) -> text.replace(",", "")
            GROUPED_DOT_INTEGER.matches(text) -> text.replace(".", "")
            AMBIGUOUS_SINGLE_SEPARATOR.matches(text) -> throw IllegalArgumentException(
                "El importe JSON '$key' tiene un formato ambiguo; usa un decimal sin separadores de miles o una agrupación inequívoca."
            )
            else -> {
                val excessDecimals = EXCESS_DECIMALS.matches(text)
                if (excessDecimals) {
                    return parseImportMoney(text.replace(',', '.'), "El importe JSON '$key'").toFixed(2)
                }
                throw IllegalArgumentException("El importe JSON '$key' tiene un formato bancario no reconocido; revísalo antes de importar.")
            }
        }
        return parseImportMoney(canonical, "El importe JSON '$key'").toFixed(2)
    }

    private val CANONICAL_JSON_DECIMAL = Regex("""-?(?:0|[1-9]\d*)(?:\.\d+)?""")
    private val CURRENCY_CODE = Regex("""(?i)\b(?:COP|USD|ARS|MXN|EUR)\b""")
    private val PLAIN_INTEGER = Regex("""[+-]?\d+""")
    private val PLAIN_DOT_DECIMAL = Regex("""[+-]?\d+\.\d{1,2}""")
    private val PLAIN_COMMA_DECIMAL = Regex("""[+-]?\d+,\d{1,2}""")
    private val GROUPED_COMMA_DECIMAL = Regex("""[+-]?\d{1,3}(?:,\d{3})+\.\d{1,2}""")
    private val GROUPED_DOT_DECIMAL = Regex("""[+-]?\d{1,3}(?:\.\d{3})+,\d{1,2}""")
    private val GROUPED_COMMA_INTEGER = Regex("""[+-]?\d{1,3}(?:,\d{3}){2,}""")
    private val GROUPED_DOT_INTEGER = Regex("""[+-]?\d{1,3}(?:\.\d{3}){2,}""")
    private val AMBIGUOUS_SINGLE_SEPARATOR = Regex("""[+-]?\d+[.,]\d{3}""")
    private val EXCESS_DECIMALS = Regex("""[+-]?\d+[.,]\d{4,}""")

    private fun JsonObject.getIntOrNull(key: String): Int? {
        val el = get(key) ?: return null
        if (el is JsonPrimitive) {
            return el.intOrNull ?: el.content.toIntOrNull()
        }
        return null
    }
}
