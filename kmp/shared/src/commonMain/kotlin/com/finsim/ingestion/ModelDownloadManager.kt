package com.finsim.ingestion

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModelDownloadResult(val fileName: String, val sizeBytes: Long, val sha256: String)

fun interface ModelDownloader {
    suspend fun download(storage: ModelStorage, onProgress: (Long) -> Unit): ModelDownloadResult
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data class Completed(val totalSizeBytes: Long) : DownloadState
    data class Error(val message: String) : DownloadState
}

val DownloadState.isCompleted: Boolean get() = this is DownloadState.Completed
val DownloadState.isDownloading: Boolean get() = this is DownloadState.Downloading

class ModelDownloadManager(
    val storage: ModelStorage,
    val capabilityGate: DeviceCapabilityGate,
    private val downloader: ModelDownloader,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow<DownloadState>(
        if (storage.isModelDownloaded()) DownloadState.Completed(storage.getModelSizeBytes()) else DownloadState.Idle
    )
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    fun isModelDownloaded(): Boolean = storage.isModelDownloaded()

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        if (!capabilityGate.isEligibleForVlm) {
            _state.value = DownloadState.Error(
                capabilityGate.ineligibilityReason ?: "Gemma requiere Android, 8 GiB de RAM total y memoria disponible."
            )
            return
        }
        if (storage.isModelDownloaded()) {
            _state.value = DownloadState.Completed(storage.getModelSizeBytes())
            return
        }

        downloadJob = coroutineScope.launch {
            var attemptStorage: ModelStorage? = null
            try {
                val attempt = storage.createDownloadAttempt()
                attemptStorage = attempt
                attempt.beginDownload()
                val attemptContext = currentCoroutineContext()
                val result = downloader.download(attempt) { downloaded ->
                    val attemptIsActive = try {
                        attemptContext.ensureActive()
                        true
                    } catch (_: CancellationException) {
                        false
                    }
                    if (attemptIsActive) {
                        _state.value = DownloadState.Downloading(
                            progress = (downloaded.toFloat() / GemmaModelArtifact.sizeBytes).coerceIn(0f, 1f),
                            downloadedBytes = downloaded,
                            totalBytes = GemmaModelArtifact.sizeBytes
                        )
                    }
                }
                currentCoroutineContext().ensureActive()
                GemmaModelArtifact.validationError(result.fileName, result.sizeBytes, result.sha256)?.let {
                    error(it)
                }
                attempt.markDownloaded(result.sizeBytes)
                _state.value = DownloadState.Completed(result.sizeBytes)
            } catch (cancelled: CancellationException) {
                attemptStorage?.discardPartialDownload()
                throw cancelled
            } catch (failure: Exception) {
                attemptStorage?.discardPartialDownload()
                _state.value = DownloadState.Error(failure.message ?: "No se pudo descargar y verificar Gemma.")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        if (_state.value is DownloadState.Downloading) _state.value = DownloadState.Idle
    }

    fun deleteModel(): Boolean {
        downloadJob?.cancel()
        downloadJob = null
        val deleted = storage.deleteModel()
        _state.value = DownloadState.Idle
        return deleted
    }
}
