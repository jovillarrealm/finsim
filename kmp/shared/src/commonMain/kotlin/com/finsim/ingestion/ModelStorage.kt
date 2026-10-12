package com.finsim.ingestion

/**
 * Interface for managing local on-device persistence of VLM model weights and checkpoints.
 */
interface ModelStorage {
    fun isModelDownloaded(): Boolean
    fun getModelSizeBytes(): Long
    fun saveChunk(bytes: ByteArray)
    fun markDownloaded(sizeBytes: Long)
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

    override fun saveChunk(bytes: ByteArray) {
        buffer.add(bytes)
        sizeBytes += bytes.size
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
}

/**
 * Persistent model storage representation for managing local checkpoints in the application files directory.
 */
class PersistentDiskModelStorage(
    val modelDirectoryPath: String = "models/gemma4-e2b-int4",
    private var downloaded: Boolean = false,
    private var sizeBytes: Long = 0L
) : ModelStorage {

    override fun isModelDownloaded(): Boolean = downloaded

    override fun getModelSizeBytes(): Long = sizeBytes

    override fun saveChunk(bytes: ByteArray) {
        sizeBytes += bytes.size
    }

    override fun markDownloaded(sizeBytes: Long) {
        this.sizeBytes = sizeBytes
        this.downloaded = true
    }

    override fun deleteModel(): Boolean {
        sizeBytes = 0L
        downloaded = false
        return true
    }
}
