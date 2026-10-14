package com.finsim.ingestion

/**
 * Seam for executing local on-device Vision-Language Model inference on rasterized document pages.
 */
interface VlmVisionRunner {
    val modelName: String get() = "Gemma 4 E2B INT4"
    suspend fun isModelReady(): Boolean
    suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode = ImportTargetMode.SCENARIO
    ): ExtractedDocumentData
}

/**
 * Fake implementation of [VlmVisionRunner] for unit tests, previews, and headless environments.
 */
class FakeVlmVisionRunner(
    var isReady: Boolean = true,
    var customResult: ExtractedDocumentData? = null
) : VlmVisionRunner {
    var callCount: Int = 0
        private set
    var lastAnalyzedImageBytes: ByteArray? = null
        private set
    var lastFramework: RegulatoryFramework? = null
        private set
    var lastTargetMode: ImportTargetMode? = null
        private set

    override suspend fun isModelReady(): Boolean = isReady

    override suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData {
        callCount++
        lastAnalyzedImageBytes = imageBytes
        lastFramework = framework
        lastTargetMode = targetMode

        customResult?.let { return it }

        val textPayload = try {
            imageBytes.decodeToString()
        } catch (_: Exception) {
            ""
        }
        val trimmed = textPayload.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("```")) {
            val parsed = VlmJsonGrammar.parseVlmResponse(textPayload, framework, targetMode)
            if (parsed.confidence > 0.0f) {
                return parsed
            }
        }

        return ExtractedDocumentData(
            sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
            analyzedPage = 1,
            bankNameCandidate = "Banco Demo VLM",
            statementMonth = 1,
            statementCutDate = "2026-10",
            closingBalance = "25000000.00",
            originalPrincipal = "30000000.00",
            termMonths = 36,
            rate = ExtractedRate(valuePercent = "18.50", kind = framework.defaultRateKind),
            monthlyPayment = "850000.00",
            principalPaid = "450000.00",
            interestCharged = "380000.00",
            totalInsurance = "20000.00",
            insurances = listOf(ExtractedInsurance(name = "Seguro deudores", monthlyCost = "20000.00")),
            confidence = 0.95f
        )
    }
}
