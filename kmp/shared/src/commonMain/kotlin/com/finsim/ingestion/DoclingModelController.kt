package com.finsim.ingestion

import kotlinx.coroutines.flow.StateFlow

const val GRANITE_DOCLING_MODEL_SIZE_BYTES: Long = 213_192_793L

sealed interface DoclingModelState {
    data object Checking : DoclingModelState
    data object NotInstalled : DoclingModelState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DoclingModelState
    data class Installed(val totalBytes: Long) : DoclingModelState
    data class Error(val message: String) : DoclingModelState
}

/** Explicit browser model lifecycle, kept separate from Android's Gemma downloader and RAM gate. */
interface DoclingModelController {
    val state: StateFlow<DoclingModelState>

    /** Rechecks every pinned cache artifact and reports installed only after integrity validation. */
    suspend fun refresh()

    /** Starts an explicit user-requested download; this must never be invoked during inference. */
    fun startDownload()

    fun cancelDownload()

    suspend fun deleteModelAsync(): Boolean
}
