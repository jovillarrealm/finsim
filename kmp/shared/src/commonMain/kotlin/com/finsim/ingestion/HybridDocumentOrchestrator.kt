package com.finsim.ingestion

import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

/**
 * Result outcomes from running the hybrid document ingestion cascade.
 */
sealed interface OrchestrationResult {
    data class Success(
        val data: ExtractedDocumentData,
        val tierUsed: ExtractionEngine
    ) : OrchestrationResult

    data class RequiresModelDownload(
        val message: String
    ) : OrchestrationResult

    data class HardwareRestricted(
        val reason: String
    ) : OrchestrationResult

    data class Error(
        val message: String
    ) : OrchestrationResult
}

/**
 * Hybrid orchestrator cascading between fast Tier 1 digital text parsing (0 MB memory / CPU)
 * and deep on-device Tier 2 local vision extraction (Gemma 4 E2B INT4) with hardware gating,
 * model availability check, and thermal pacing.
 */
class HybridDocumentOrchestrator(
    val densityScanner: PageDensityScanner = PageDensityScanner,
    val textParser: DocumentTextParser = DocumentTextParser,
    val rasterizer: PageRasterizer = InMemoryPageRasterizer(),
    val vlmRunner: VlmVisionRunner = FakeVlmVisionRunner(),
    val capabilityGate: DeviceCapabilityGate,
    val modelStorage: ModelStorage
) {

    /**
     * Executes the hybrid ingestion cascade on the incoming document.
     */
    suspend fun processDocument(
        pdfBytes: ByteArray,
        rawText: String,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode = ImportTargetMode.SCENARIO,
        forceTier2: Boolean = false
    ): OrchestrationResult {
        return try {
            // Tier 1 consumes only text the user pasted. A selected PDF is an image source and
            // must reach the platform rasterizer without decoding its bytes as text.
            val pages = splitPastedTextPages(rawText)

            val bestPageIndex = if (pages.isNotEmpty()) {
                densityScanner.findBestPageIndex(pages)
            } else {
                1
            }

            // 1. Attempt Tier 1 if not forced to Tier 2
            if (!forceTier2 && pages.isNotEmpty() && pages.any { it.isNotBlank() }) {
                val bestPageText = pages.getOrElse(bestPageIndex - 1) { pages.first() }
                val tier1Data = textParser.parse(bestPageText, framework, bestPageIndex)

                val hasPrincipal = !tier1Data.originalPrincipal.isNullOrBlank() ||
                    (tier1Data.balanceMeaning == BalanceMeaning.PRINCIPAL &&
                        !(tier1Data.capitalBalance ?: tier1Data.closingBalance).isNullOrBlank())
                val hasRate = tier1Data.rate != null

                if (tier1Data.confidence >= 0.8f && hasPrincipal && hasRate) {
                    return OrchestrationResult.Success(
                        data = tier1Data,
                        tierUsed = ExtractionEngine.TIER_1_DIGITAL_TEXT
                    )
                }
            }

            // 2. Cascade to Tier 2 (Local VLM)
            // A. Check Hardware Eligibility (< 8 GiB RAM blocked)
            if (!capabilityGate.isEligibleForVlm) {
                return OrchestrationResult.HardwareRestricted(
                    reason = capabilityGate.ineligibilityReason
                        ?: "Memoria RAM insuficiente para ejecutar el modelo de visión local Gemma 4 E2B (se requieren >= 8.0 GB de RAM)."
                )
            }

            // B. Check Model Download Status
            if (!modelStorage.isModelDownloaded()) {
                return OrchestrationResult.RequiresModelDownload(
                    message = "El modelo de visión local Gemma 4 E2B (2.58 GB) no se encuentra descargado en este dispositivo."
                )
            }

            // C. Rasterize best page at 150 DPI
            validatePdfForInMemoryRasterization(pdfBytes)
            val imageBytes = rasterizer.rasterizePage(
                pdfBytes = pdfBytes,
                pageIndex = bestPageIndex,
                dpi = 150
            )

            // D. Thermal cooldown delay (500 ms) to prevent thermal throttling on multi-page docs
            delay(500)

            // E. Local VLM Vision Analysis
            val rawVlmData = vlmRunner.analyzePage(
                imageBytes = imageBytes,
                framework = framework,
                targetMode = targetMode
            )

            val finalData = rawVlmData.copy(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                analyzedPage = bestPageIndex
            )

            OrchestrationResult.Success(
                data = finalData,
                tierUsed = ExtractionEngine.TIER_2_LOCAL_VLM
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            OrchestrationResult.Error(
                message = e.message ?: "Error inesperado al procesar el documento con el orquestador híbrido."
            )
        }
    }

    private fun splitPastedTextPages(text: String): List<String> = text
        .split(Regex("(?:\\u000C|---+\\s*(?:Page|Página)\\s*\\d+\\s*---+)", RegexOption.IGNORE_CASE))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
