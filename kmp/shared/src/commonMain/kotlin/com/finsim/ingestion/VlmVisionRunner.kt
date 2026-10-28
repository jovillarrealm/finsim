package com.finsim.ingestion

/**
 * Seam for executing local on-device Vision-Language Model inference on rasterized document pages.
 */
interface VlmVisionRunner {
    val modelName: String get() = "Gemma 4 E2B INT4"
    val extractionEngine: ExtractionEngine get() = ExtractionEngine.TIER_2_LOCAL_VLM
    suspend fun isModelReady(): Boolean
    suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode = ImportTargetMode.SCENARIO
    ): ExtractedDocumentData

    /** Processes a selected raster image directly; Android runners retain their PDF-only entry path. */
    suspend fun analyzeImage(
        imageBytes: ByteArray,
        format: VisualDocumentFormat,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode = ImportTargetMode.SCENARIO
    ): ExtractedDocumentData {
        require(format == VisualDocumentFormat.PNG || format == VisualDocumentFormat.JPEG) {
            "Selecciona una imagen PNG o JPEG válida."
        }
        return analyzePage(imageBytes, framework, targetMode)
    }
}

/**
 * Test double that never reports a model or invents inference output. Tests that exercise
 * orchestration success should provide a test-local runner with a controlled literal result.
 */
class FakeVlmVisionRunner : VlmVisionRunner {
    var callCount: Int = 0
        private set
    var lastAnalyzedImageBytes: ByteArray? = null
        private set
    var lastFramework: RegulatoryFramework? = null
        private set
    var lastTargetMode: ImportTargetMode? = null
        private set

    override suspend fun isModelReady(): Boolean = false

    override suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData {
        callCount++
        lastAnalyzedImageBytes = imageBytes
        lastFramework = framework
        lastTargetMode = targetMode

        throw IllegalStateException(
            "El runner de prueba no realiza inferencia; usa LiteRT-LM en Android para analizar píxeles reales."
        )
    }
}
