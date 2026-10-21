package com.finsim.ingestion

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

class AndroidModelStorage internal constructor(private val directory: File) : ModelStorage {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "models"))

    private val modelFile = File(directory, GemmaModelArtifact.fileName)
    private val markerFile = File(directory, "${GemmaModelArtifact.fileName}.verified")
    private var partialFile: File? = null
    private var markerTempFile: File? = null
    private var output: FileOutputStream? = null
    private var digest: MessageDigest? = null

    override fun isModelDownloaded(): Boolean =
        modelFile.isFile && modelFile.length() == GemmaModelArtifact.sizeBytes &&
            runCatching { markerFile.readText() == markerContents }.getOrDefault(false)

    override fun getModelSizeBytes(): Long = if (isModelDownloaded()) modelFile.length() else 0L

    override fun getModelFilePath(): String? = modelFile.absolutePath.takeIf { isModelDownloaded() }

    override fun createDownloadAttempt(): ModelStorage = AndroidModelStorage(directory)

    @Synchronized
    override fun beginDownload() {
        discardCurrentAttempt()
        check(directory.exists() || directory.mkdirs()) { "No se pudo crear el directorio privado del modelo." }
        val attemptId = UUID.randomUUID().toString()
        val partial = File(directory, "${GemmaModelArtifact.fileName}.$attemptId.part")
        val markerTemp = File(directory, "${markerFile.name}.$attemptId.tmp")
        partialFile = partial
        markerTempFile = markerTemp
        output = FileOutputStream(partial, false)
        digest = MessageDigest.getInstance("SHA-256")
    }

    @Synchronized
    override fun saveChunk(bytes: ByteArray, length: Int) {
        require(length in 0..bytes.size)
        (output ?: error("La descarga del modelo no está abierta.")).write(bytes, 0, length)
        (digest ?: error("La verificación del modelo no está abierta.")).update(bytes, 0, length)
    }

    @Synchronized
    override fun markDownloaded(sizeBytes: Long) {
        val stream = output ?: error("La descarga del modelo no está abierta.")
        val partial = partialFile ?: error("No se encontró el archivo parcial de esta descarga.")
        val markerTemp = markerTempFile ?: error("No se encontró el marcador temporal de esta descarga.")
        stream.fd.sync()
        stream.close()
        output = null
        val actualDigest = checkNotNull(digest).digest().joinToString("") { "%02x".format(it) }
        digest = null
        check(sizeBytes == GemmaModelArtifact.sizeBytes && partial.length() == sizeBytes) {
            "El archivo descargado no coincide con el tamaño fijado para Gemma."
        }
        check(actualDigest.equals(GemmaModelArtifact.sha256, ignoreCase = true)) {
            "El SHA-256 del archivo persistido no coincide con la revisión fijada de Gemma."
        }

        markerTemp.writeText(markerContents)
        Files.move(partial.toPath(), modelFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Files.move(markerTemp.toPath(), markerFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        partialFile = null
        markerTempFile = null
    }

    @Synchronized
    override fun discardPartialDownload() {
        discardCurrentAttempt()
    }

    @Synchronized
    override fun deleteModel(): Boolean {
        discardCurrentAttempt()
        var deleted = true
        for (file in listOf(modelFile, markerFile)) {
            val removed = runCatching { !file.exists() || file.delete() }.getOrDefault(false)
            deleted = removed && deleted
        }
        return deleted
    }

    private fun discardCurrentAttempt() {
        runCatching { output?.close() }
        output = null
        digest = null
        partialFile?.let { runCatching { !it.exists() || it.delete() } }
        markerTempFile?.let { runCatching { !it.exists() || it.delete() } }
        partialFile = null
        markerTempFile = null
    }

    private val markerContents: String
        get() = "${GemmaModelArtifact.fileName}\n${GemmaModelArtifact.sizeBytes}\n${GemmaModelArtifact.revision}\n${GemmaModelArtifact.sha256}"
}
