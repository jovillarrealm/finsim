package com.finsim.ingestion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class VlmInsuranceJson(
    val name: String = "",
    val amount: String = ""
)

@Serializable
data class VlmExtractionPayload(
    val bankName: String? = null,
    val principal: String? = null,
    val ratePercentage: String? = null,
    val rateKind: String? = null,
    val termMonths: Int? = null,
    val statementMonth: Int? = null,
    val statementCutDate: String? = null,
    val monthlyPayment: String? = null,
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
          "properties": {
            "bankName": { "type": ["string", "null"] },
            "principal": { "type": ["string", "number", "null"] },
            "ratePercentage": { "type": ["string", "number", "null"] },
            "rateKind": { "type": ["string", "null"], "enum": ["annual", "monthly", "nominal", null] },
            "termMonths": { "type": ["integer", "null"] },
            "statementMonth": { "type": ["integer", "null"] },
            "statementCutDate": { "type": ["string", "null"] },
            "monthlyPayment": { "type": ["string", "number", "null"] },
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
                  "amount": { "type": ["string", "number"] }
                },
                "required": ["name", "amount"]
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
        val principal = jsonElement.getStringOrNull("principal")?.let { DocumentTextParser.cleanCurrencyNumber(it) }
        val ratePercent = jsonElement.getStringOrNull("ratePercentage")?.replace(',', '.')
        val rawRateKind = jsonElement.getStringOrNull("rateKind")?.lowercase()
        val rateKind = when (rawRateKind) {
            "monthly", "month" -> "monthly"
            "nominal" -> "nominal"
            "annual", "ea", "tea" -> "annual"
            else -> framework.defaultRateKind
        }
        val rate = if (!ratePercent.isNullOrBlank()) {
            ExtractedRate(
                valuePercent = ratePercent,
                kind = rateKind,
                nominalFrequency = if (rateKind == "nominal") 12 else null
            )
        } else null

        val termMonths = jsonElement.getIntOrNull("termMonths")
        val statementMonth = jsonElement.getIntOrNull("statementMonth")
        val statementCutDate = jsonElement.getStringOrNull("statementCutDate")
        val monthlyPayment = jsonElement.getStringOrNull("monthlyPayment")?.let { DocumentTextParser.cleanCurrencyNumber(it) }
        val principalPaid = jsonElement.getStringOrNull("principalPaid")?.let { DocumentTextParser.cleanCurrencyNumber(it) }
        val interestPaid = jsonElement.getStringOrNull("interestPaid")?.let { DocumentTextParser.cleanCurrencyNumber(it) }
        val insurancePaid = jsonElement.getStringOrNull("insurancePaid")?.let { DocumentTextParser.cleanCurrencyNumber(it) }
        val finalBalance = jsonElement.getStringOrNull("finalBalance")?.let { DocumentTextParser.cleanCurrencyNumber(it) }

        val insurancesList = mutableListOf<ExtractedInsurance>()
        val insurancesArray = jsonElement["insurances"] as? JsonArray
        if (insurancesArray != null) {
            for (item in insurancesArray) {
                if (item is JsonObject) {
                    val name = item.getStringOrNull("name") ?: "Seguro"
                    val amountRaw = item.getStringOrNull("amount")
                    val amountClean = amountRaw?.let { DocumentTextParser.cleanCurrencyNumber(it) } ?: "0.00"
                    insurancesList.add(ExtractedInsurance(name = name, monthlyCost = amountClean))
                }
            }
        }

        var confidence = 0.85f
        if (principal != null || finalBalance != null) confidence += 0.05f
        if (rate != null) confidence += 0.05f
        if (termMonths != null) confidence += 0.03f

        return ExtractedDocumentData(
            sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
            analyzedPage = pageIndex,
            bankNameCandidate = bankName,
            statementMonth = statementMonth,
            statementCutDate = statementCutDate,
            closingBalance = finalBalance ?: principal,
            originalPrincipal = principal,
            termMonths = termMonths,
            rate = rate,
            monthlyPayment = monthlyPayment,
            principalPaid = principalPaid,
            interestCharged = interestPaid,
            totalInsurance = insurancePaid,
            insurances = insurancesList,
            rawExtractedText = rawOutput,
            confidence = confidence.coerceIn(0.0f, 1.0f)
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

    private fun JsonObject.getIntOrNull(key: String): Int? {
        val el = get(key) ?: return null
        if (el is JsonPrimitive) {
            return el.intOrNull ?: el.content.toIntOrNull()
        }
        return null
    }
}
