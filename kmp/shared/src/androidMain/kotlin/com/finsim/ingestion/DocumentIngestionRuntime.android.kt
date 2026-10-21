package com.finsim.ingestion

actual fun getDocumentIngestionRuntime(): DocumentIngestionRuntime {
    val context = AndroidIngestionContext.require()
    val storage = AndroidModelStorage(context)
    val runner = AndroidGemmaVisionRunner(context, storage)
    return DocumentIngestionRuntime(
        supportsGemma = true,
        modelStorage = storage,
        modelDownloader = AndroidModelDownloader(),
        capabilityGate = AndroidDeviceCapabilityGate(context),
        pageRasterizer = AndroidPdfRasterizer(context),
        vlmRunner = runner,
        closeResources = runner::close
    )
}
