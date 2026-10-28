package com.finsim.ingestion

/**
 * Interface for managing local on-device persistence of VLM model weights and checkpoints.
 */
interface ModelStorage {
    fun isModelDownloaded(): Boolean
    /** May revalidate persisted artifacts asynchronously before a model is used. */
    suspend fun verifyModelDownloaded(): Boolean = isModelDownloaded()
    fun getModelSizeBytes(): Long
    fun getModelFilePath(): String? = null
    /** Returns storage state owned by one download attempt so late cleanup cannot touch a retry. */
    fun createDownloadAttempt(): ModelStorage = this
    fun beginDownload()
    fun saveChunk(bytes: ByteArray, length: Int = bytes.size)
    fun markDownloaded(sizeBytes: Long)
    fun discardPartialDownload()
    fun deleteModel(): Boolean
}

/**
 * In-memory implementation of [ModelStorage] for testing, preview, and headless environments.
 */
class InMemoryModelStorage(
    private var downloaded: Boolean = false,
    private var sizeBytes: Long = 0L
) : ModelStorage {
    private val buffer = mutableListOf<ByteArray>()

    override fun isModelDownloaded(): Boolean = downloaded

    override fun getModelSizeBytes(): Long = sizeBytes

    override fun beginDownload() {
        buffer.clear()
        downloaded = false
        sizeBytes = 0L
    }

    override fun saveChunk(bytes: ByteArray, length: Int) {
        require(length in 0..bytes.size)
        buffer.add(bytes.copyOf(length))
        sizeBytes += length
    }

    override fun markDownloaded(sizeBytes: Long) {
        this.sizeBytes = sizeBytes
        this.downloaded = true
    }

    override fun deleteModel(): Boolean {
        buffer.clear()
        sizeBytes = 0L
        downloaded = false
        return true
    }

    override fun discardPartialDownload() {
        if (!downloaded) {
            buffer.clear()
            sizeBytes = 0L
        }
    }
}
