package com.finsim.ingestion

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
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

@OptIn(InternalCoroutinesApi::class)
class ModelDownloadManager(
    val storage: ModelStorage,
    val capabilityGate: DeviceCapabilityGate,
    private val downloader: ModelDownloader,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private data class Snapshot(
        val attemptToken: Any?,
        val attemptJob: Job?,
        val state: DownloadState
    )

    private val snapshot = MutableStateFlow(
        Snapshot(
            attemptToken = null,
            attemptJob = null,
            state = if (storage.isModelDownloaded()) {
                DownloadState.Completed(storage.getModelSizeBytes())
            } else {
                DownloadState.Idle
            }
        )
    )

    /** Exposes immediate state while keeping the attempt token and Job private. */
    val state: StateFlow<DownloadState> = object : StateFlow<DownloadState> {
        override val value: DownloadState get() = snapshot.value.state
        override val replayCache: List<DownloadState> get() = listOf(value)

        override suspend fun collect(collector: FlowCollector<DownloadState>): Nothing {
            var hasPrevious = false
            var previous: DownloadState? = null
            snapshot.collect { current ->
                if (!hasPrevious || previous != current.state) {
                    collector.emit(current.state)
                    previous = current.state
                    hasPrevious = true
                }
            }
        }
    }

    fun isModelDownloaded(): Boolean = storage.isModelDownloaded()

    fun startDownload() {
        while (true) {
            val current = snapshot.value
            if (current.attemptToken != null) return

            if (!capabilityGate.isEligibleForVlm) {
                val blocked = current.copy(
                    state = DownloadState.Error(
                        capabilityGate.ineligibilityReason
                            ?: "Gemma requiere Android, 8 GiB de RAM total y memoria disponible."
                    )
                )
                if (snapshot.compareAndSet(current, blocked)) return
                continue
            }

            if (storage.isModelDownloaded()) {
                val completed = current.copy(state = DownloadState.Completed(storage.getModelSizeBytes()))
                if (snapshot.compareAndSet(current, completed)) return
                continue
            }

            val token = Any()
            val starting = Snapshot(
                attemptToken = token,
                attemptJob = null,
                state = DownloadState.Downloading(
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = GemmaModelArtifact.sizeBytes
                )
            )
            if (!snapshot.compareAndSet(current, starting)) continue

            val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
                runDownloadAttempt(token)
            }
            while (true) {
                val reserved = snapshot.value
                if (reserved.attemptToken !== token) {
                    job.cancel()
                    return
                }
                if (snapshot.compareAndSet(reserved, reserved.copy(attemptJob = job))) {
                    job.start()
                    if (!job.isActive) {
                        publishTerminal(
                            token,
                            DownloadState.Error("No se pudo iniciar la descarga del modelo.")
                        )
                    }
                    return
                }
            }
        }
    }

    fun cancelDownload() {
        while (true) {
            val current = snapshot.value
            val token = current.attemptToken ?: return
            val cancelled = Snapshot(attemptToken = null, attemptJob = null, state = DownloadState.Idle)
            if (snapshot.compareAndSet(current, cancelled)) {
                current.attemptJob?.cancel()
                return
            }
            if (snapshot.value.attemptToken !== token) return
        }
    }

    fun deleteModel(): Boolean {
        while (true) {
            val current = snapshot.value
            val idle = Snapshot(attemptToken = null, attemptJob = null, state = DownloadState.Idle)
            if (snapshot.compareAndSet(current, idle)) {
                current.attemptJob?.cancel()
                return storage.deleteModel()
            }
        }
    }

    private suspend fun runDownloadAttempt(token: Any) {
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
                if (attemptIsActive) publishProgress(token, downloaded)
            }
            currentCoroutineContext().ensureActive()
            GemmaModelArtifact.validationError(result.fileName, result.sizeBytes, result.sha256)?.let {
                error(it)
            }
            attempt.markDownloaded(result.sizeBytes)
            currentCoroutineContext().ensureActive()
            publishTerminal(token, DownloadState.Completed(result.sizeBytes))
        } catch (cancelled: CancellationException) {
            attemptStorage?.discardPartialDownload()
            throw cancelled
        } catch (failure: Exception) {
            attemptStorage?.discardPartialDownload()
            publishTerminal(
                token,
                DownloadState.Error(failure.message ?: "No se pudo descargar y verificar Gemma.")
            )
        }
    }

    private fun publishProgress(token: Any, downloaded: Long) {
        while (true) {
            val current = snapshot.value
            if (current.attemptToken !== token || current.state !is DownloadState.Downloading) return
            val progress = current.copy(
                state = DownloadState.Downloading(
                    progress = (downloaded.toFloat() / GemmaModelArtifact.sizeBytes).coerceIn(0f, 1f),
                    downloadedBytes = downloaded,
                    totalBytes = GemmaModelArtifact.sizeBytes
                )
            )
            if (snapshot.compareAndSet(current, progress)) return
        }
    }

    private fun publishTerminal(token: Any, state: DownloadState) {
        while (true) {
            val current = snapshot.value
            if (current.attemptToken !== token) return
            val terminal = Snapshot(attemptToken = null, attemptJob = null, state = state)
            if (snapshot.compareAndSet(current, terminal)) return
        }
    }
}
