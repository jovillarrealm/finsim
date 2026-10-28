package com.finsim.ingestion

import kotlinx.coroutines.CoroutineScope

/** Platform-owned dependencies for document ingestion. Android is the only target with Gemma. */
class DocumentIngestionRuntime internal constructor(
    val supportsGemma: Boolean,
    val modelStorage: ModelStorage,
    val modelDownloader: ModelDownloader,
    val capabilityGate: DeviceCapabilityGate,
    val pageRasterizer: PageRasterizer,
    val vlmRunner: VlmVisionRunner,
    private val closeResources: () -> Unit = {},
    val supportsDocling: Boolean = false,
    val doclingModelController: DoclingModelController? = null,
    private val enforceVlmHardwareGate: Boolean = true,
    private val supportsDirectImageInputs: Boolean = false
) {
    fun createOrchestrator(): HybridDocumentOrchestrator = HybridDocumentOrchestrator(
        rasterizer = pageRasterizer,
        vlmRunner = vlmRunner,
        capabilityGate = capabilityGate,
        modelStorage = modelStorage,
        enforceHardwareGate = enforceVlmHardwareGate,
        supportsDirectImageInputs = supportsDirectImageInputs
    )

    fun createModelDownloadManager(scope: CoroutineScope): ModelDownloadManager = ModelDownloadManager(
        storage = modelStorage,
        capabilityGate = capabilityGate,
        downloader = modelDownloader,
        coroutineScope = scope
    )

    fun close() = closeResources()
}

expect fun getDocumentIngestionRuntime(): DocumentIngestionRuntime

internal fun createUnavailableDocumentIngestionRuntime(): DocumentIngestionRuntime = DocumentIngestionRuntime(
    supportsGemma = false,
    modelStorage = InMemoryModelStorage(),
    modelDownloader = ModelDownloader { _, _ ->
        error("Gemma solo está disponible en Android.")
    },
    capabilityGate = getDeviceCapabilityGate(),
    pageRasterizer = getDefaultPageRasterizer(),
    vlmRunner = FakeVlmVisionRunner()
)
