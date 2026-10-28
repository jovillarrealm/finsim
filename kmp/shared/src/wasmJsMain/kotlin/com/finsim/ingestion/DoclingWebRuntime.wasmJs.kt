@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.finsim.ingestion

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.JsAny
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.js

private const val WORKER_INACTIVITY_TIMEOUT_MS: Long = 180_000L
private val workerJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

@Serializable
private data class DoclingWorkerRequest(
    val id: String,
    val type: String,
    val bytesBase64: String? = null,
    val format: String? = null,
    val cancelId: String? = null
)

@Serializable
private data class DoclingWorkerMessage(
    val id: String = "",
    val kind: String = "",
    val stage: String? = null,
    val message: String? = null,
    val installed: Boolean? = null,
    val totalBytes: Long? = null,
    val downloadedBytes: Long? = null,
    val bytesBase64: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val docTags: String? = null,
    val device: String? = null,
    val outputTokens: Int? = null,
    val maxGenerationTokens: Int? = null,
    val stoppedAtTokenLimit: Boolean? = null,
    val processorMs: Long? = null,
    val modelLoadMs: Long? = null,
    val processorAndInferenceMs: Long? = null,
    val promptTokens: Int? = null,
    val artifactIndex: Int? = null,
    val artifactCount: Int? = null
)

private data class PendingDoclingRequest(
    val continuation: CancellableContinuation<DoclingWorkerMessage>,
    val onProgress: ((DoclingWorkerMessage) -> Unit)?,
    val watchdog: ProgressInactivityWatchdog
)

/** One in-memory Worker client shared by the model cache, PDF rasterizer, and vision runner. */
private class DoclingBrowserWorkerClient {
    private val requestMutex = Mutex()
    private val watchdogScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pending = mutableMapOf<String, PendingDoclingRequest>()
    private var nextRequestId = 0
    private var activeRequestId: String? = null
    private var closed = false
    private var worker: JsAny? = null

    suspend fun request(
        type: String,
        bytes: ByteArray? = null,
        format: VisualDocumentFormat? = null,
        onProgress: ((DoclingWorkerMessage) -> Unit)? = null
    ): DoclingWorkerMessage = requestMutex.withLock {
        check(!closed) { "El Worker de Granite-Docling ya se cerró." }
        val currentWorker = getWorker()
        val requestId = "docling-${++nextRequestId}"
        val message = DoclingWorkerRequest(
            id = requestId,
            type = type,
            bytesBase64 = bytes?.encodeBase64ForWorker(),
            format = format?.name
        )
        activeRequestId = requestId
        try {
            suspendCancellableCoroutine { continuation ->
                val watchdog = ProgressInactivityWatchdog(
                    scope = watchdogScope,
                    timeoutMillis = WORKER_INACTIVITY_TIMEOUT_MS
                ) {
                    val expired = pending.remove(requestId)
                    if (expired != null) {
                        activeRequestId = null
                        terminateWorker()
                        if (expired.continuation.isActive) {
                            expired.continuation.resumeWithException(
                                IllegalStateException("El Worker local no informó progreso durante 180 segundos; se canceló esta solicitud.")
                            )
                        }
                    }
                }
                val pendingRequest = PendingDoclingRequest(continuation, onProgress, watchdog)
                pending[requestId] = pendingRequest
                watchdog.progress()
                continuation.invokeOnCancellation {
                    pending.remove(requestId)?.watchdog?.cancel()
                    if (activeRequestId == requestId) {
                        activeRequestId = null
                        terminateWorker()
                    }
                }
                postWorkerMessage(currentWorker, workerJson.encodeToString(message))
            }
        } finally {
            if (activeRequestId == requestId) activeRequestId = null
        }
    }

    fun cancelActiveRequest() {
        val requestId = activeRequestId ?: return
        val currentWorker = worker
        if (currentWorker == null) return
        val message = DoclingWorkerRequest(id = "cancel-$requestId", type = "cancel", cancelId = requestId)
        postWorkerMessage(currentWorker, workerJson.encodeToString(message))
    }

    fun close() {
        if (closed) return
        closed = true
        pending.values.toList().forEach { it.continuation.cancel(CancellationException("Se cerró el diálogo.")) }
        pending.clear()
        watchdogScope.cancel()
        terminateWorker()
    }

    private fun getWorker(): JsAny {
        worker?.let { return it }
        val created = createDoclingWorker(::handleMessage)
        worker = created
        return created
    }

    private fun handleMessage(serialized: String) {
        val message = runCatching { workerJson.decodeFromString<DoclingWorkerMessage>(serialized) }
            .getOrElse { failure ->
                val active = activeRequestId ?: return
                val request = pending.remove(active) ?: return
                request.continuation.resumeWithException(
                    IllegalStateException("Respuesta inválida del Worker: ${failure.message ?: "JSON desconocido"}")
                )
                return
            }
        val request = pending[message.id]
        if (message.id == "__worker_error") {
            val active = activeRequestId ?: return
            val activeRequest = pending.remove(active) ?: return
            activeRequest.continuation.resumeWithException(
                IllegalStateException(message.message ?: "Falló el Worker local de Granite-Docling.")
            )
            return
        }
        if (request == null) return
        when (message.kind) {
            "progress" -> {
                request.watchdog.progress()
                request.onProgress?.invoke(message)
            }
            "result" -> {
                pending.remove(message.id)
                request.watchdog.cancel()
                request.continuation.resume(message)
            }
            "error" -> {
                pending.remove(message.id)
                request.watchdog.cancel()
                request.continuation.resumeWithException(
                    IllegalStateException(message.message ?: "Error sin detalle en el Worker local.")
                )
            }
        }
    }

    private fun terminateWorker() {
        val currentWorker = worker
        worker = null
        if (currentWorker != null) terminateDoclingWorker(currentWorker)
    }
}

private class BrowserDoclingModelStorage(
    private val client: DoclingBrowserWorkerClient
) : ModelStorage, DoclingModelController {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<DoclingModelState>(DoclingModelState.Checking)
    override val state: StateFlow<DoclingModelState> = mutableState
    private var verifiedInstalled = false
    private var verifiedBytes = 0L
    private var downloadJob: Job? = null

    override fun isModelDownloaded(): Boolean = verifiedInstalled

    override suspend fun verifyModelDownloaded(): Boolean {
        try {
            val response = client.request("status")
            verifiedInstalled = response.installed == true &&
                response.totalBytes == GRANITE_DOCLING_MODEL_SIZE_BYTES
            verifiedBytes = if (verifiedInstalled) GRANITE_DOCLING_MODEL_SIZE_BYTES else 0L
            if (downloadJob?.isActive != true) {
                mutableState.value = if (verifiedInstalled) {
                    DoclingModelState.Installed(GRANITE_DOCLING_MODEL_SIZE_BYTES)
                } else {
                    DoclingModelState.NotInstalled
                }
            }
            return verifiedInstalled
        } catch (failure: Exception) {
            verifiedInstalled = false
            verifiedBytes = 0L
            if (downloadJob?.isActive != true) {
                mutableState.value = DoclingModelState.Error(
                    failure.message ?: "No se pudo comprobar el almacenamiento de Granite-Docling."
                )
            }
            throw failure
        }
    }

    override fun getModelSizeBytes(): Long = verifiedBytes

    override fun beginDownload(): Unit = error("Usa la descarga explícita de Granite-Docling para este navegador.")
    override fun saveChunk(bytes: ByteArray, length: Int): Unit = error("Los artefactos del navegador se verifican antes de almacenarlos.")
    override fun markDownloaded(sizeBytes: Long): Unit = error("No se marca el modelo sin verificar todos sus archivos.")
    override fun discardPartialDownload() = Unit
    override fun deleteModel(): Boolean = false

    override suspend fun refresh() {
        mutableState.value = DoclingModelState.Checking
        try {
            if (verifyModelDownloaded()) {
                mutableState.value = DoclingModelState.Installed(GRANITE_DOCLING_MODEL_SIZE_BYTES)
            } else {
                mutableState.value = DoclingModelState.NotInstalled
            }
        } catch (failure: Exception) {
            verifiedInstalled = false
            verifiedBytes = 0L
            mutableState.value = DoclingModelState.Error(
                failure.message ?: "No se pudo comprobar el almacenamiento de Granite-Docling."
            )
        }
    }

    override fun startDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = coroutineScope.launch {
            mutableState.value = DoclingModelState.Downloading(0f, 0L, GRANITE_DOCLING_MODEL_SIZE_BYTES)
            try {
                val response = client.request("install", onProgress = { progress ->
                    val total = progress.totalBytes ?: GRANITE_DOCLING_MODEL_SIZE_BYTES
                    val downloaded = (progress.downloadedBytes ?: 0L).coerceIn(0L, total)
                    mutableState.value = DoclingModelState.Downloading(
                        progress = if (total == 0L) 0f else downloaded.toFloat() / total,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                })
                check(response.installed == true && response.totalBytes == GRANITE_DOCLING_MODEL_SIZE_BYTES) {
                    "El Worker terminó sin verificar todos los artefactos de Granite-Docling."
                }
                verifiedInstalled = true
                verifiedBytes = GRANITE_DOCLING_MODEL_SIZE_BYTES
                mutableState.value = DoclingModelState.Installed(GRANITE_DOCLING_MODEL_SIZE_BYTES)
            } catch (cancelled: CancellationException) {
                verifiedInstalled = false
                verifiedBytes = 0L
                mutableState.value = DoclingModelState.NotInstalled
                throw cancelled
            } catch (failure: Exception) {
                verifiedInstalled = false
                verifiedBytes = 0L
                mutableState.value = DoclingModelState.Error(
                    failure.message ?: "No se pudo descargar y verificar Granite-Docling."
                )
            }
        }
    }

    override fun cancelDownload() {
        if (downloadJob?.isActive != true) return
        client.cancelActiveRequest()
        downloadJob?.cancel()
        verifiedInstalled = false
        verifiedBytes = 0L
        mutableState.value = DoclingModelState.NotInstalled
    }

    override suspend fun deleteModelAsync(): Boolean {
        downloadJob?.cancel()
        return try {
            client.request("delete")
            verifiedInstalled = false
            verifiedBytes = 0L
            mutableState.value = DoclingModelState.NotInstalled
            true
        } catch (failure: Exception) {
            mutableState.value = DoclingModelState.Error(
                failure.message ?: "No se pudo eliminar el modelo local."
            )
            false
        }
    }

    fun close() {
        downloadJob?.cancel()
        coroutineScope.cancel()
        client.close()
    }
}

private class BrowserDoclingPageRasterizer(
    private val client: DoclingBrowserWorkerClient
) : PageRasterizer {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun rasterizePage(pdfBytes: ByteArray, pageIndex: Int, dpi: Int): ByteArray {
        require(pageIndex == 1) { "El análisis web procesa únicamente la página 1 del PDF." }
        require(dpi == 150) { "El análisis web rasteriza a 150 DPI." }
        validatePdfForInMemoryRasterization(pdfBytes)
        val result = client.request("rasterize-pdf", bytes = pdfBytes)
        val width = result.width ?: error("El Worker no informó las dimensiones de la página PDF.")
        val height = result.height ?: error("El Worker no informó las dimensiones de la página PDF.")
        validateRasterDimensions(width.toLong(), height.toLong())
        val imageBytes = Base64.Default.decode(result.bytesBase64 ?: error("El Worker no devolvió el raster PNG."))
        val dimensions = validateImageForInMemoryInference(imageBytes, VisualDocumentFormat.PNG)
        require(dimensions.width == width.toLong() && dimensions.height == height.toLong()) {
            "Las dimensiones del raster PDF no coinciden con el PNG recibido."
        }
        return imageBytes
    }
}

private class BrowserDoclingVisionRunner(
    private val client: DoclingBrowserWorkerClient,
    private val modelStorage: BrowserDoclingModelStorage
) : VlmVisionRunner {
    override val modelName: String = "Granite-Docling 258M"
    override val extractionEngine: ExtractionEngine = ExtractionEngine.TIER_2_LOCAL_DOCLING

    override suspend fun isModelReady(): Boolean = modelStorage.verifyModelDownloaded()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData {
        return analyzeImage(imageBytes, VisualDocumentFormat.PNG, framework, targetMode)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyzeImage(
        imageBytes: ByteArray,
        format: VisualDocumentFormat,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData {
        validateImageForInMemoryInference(imageBytes, format)
        val response = client.request("analyze-image", bytes = imageBytes, format = format)
        val docTags = response.docTags ?: error("La inferencia no devolvió DocTags.")
        return DoclingDocTagsParser.parse(docTags, framework, pageIndex = 1)
    }
}

actual fun getDocumentIngestionRuntime(): DocumentIngestionRuntime {
    val client = DoclingBrowserWorkerClient()
    val storage = BrowserDoclingModelStorage(client)
    val runner = BrowserDoclingVisionRunner(client, storage)
    return DocumentIngestionRuntime(
        supportsGemma = false,
        modelStorage = storage,
        modelDownloader = ModelDownloader { _, _ ->
            error("Gemma solo está disponible en Android; Granite-Docling usa su instalador web explícito.")
        },
        capabilityGate = getDeviceCapabilityGate(),
        pageRasterizer = BrowserDoclingPageRasterizer(client),
        vlmRunner = runner,
        closeResources = storage::close,
        supportsDocling = true,
        doclingModelController = storage,
        enforceVlmHardwareGate = false,
        supportsDirectImageInputs = true
    )
}

private fun createDoclingWorker(onMessage: (String) -> Unit): JsAny = js("""{
    const worker = new Worker(new URL('docling/worker.mjs', document.baseURI), { type: 'module' });
    worker.onmessage = event => onMessage(typeof event.data === 'string' ? event.data : JSON.stringify(event.data));
    worker.onerror = event => onMessage(JSON.stringify({ id: '__worker_error', kind: 'error', message: event.message || 'Falló el Worker local.' }));
    return worker;
}""")

private fun postWorkerMessage(worker: JsAny, serialized: String): Unit = js("worker.postMessage(serialized)")

private fun terminateDoclingWorker(worker: JsAny): Unit = js("worker.terminate()")

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.encodeBase64ForWorker(): String = Base64.Default.encode(this)
