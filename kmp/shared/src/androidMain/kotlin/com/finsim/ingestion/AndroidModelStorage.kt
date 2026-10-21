package com.finsim.ingestion

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class AndroidModelStorage(context: Context) : ModelStorage {
    private val directory = File(context.applicationContext.filesDir, "models")
    private val modelFile = File(directory, GemmaModelArtifact.fileName)
    private val partialFile = File(directory, "${GemmaModelArtifact.fileName}.part")
    private val markerFile = File(directory, "${GemmaModelArtifact.fileName}.verified")
    private var output: FileOutputStream? = null
    private var digest: MessageDigest? = null

    override fun isModelDownloaded(): Boolean =
        modelFile.isFile && modelFile.length() == GemmaModelArtifact.sizeBytes &&
            runCatching { markerFile.readText() == markerContents }.getOrDefault(false)

    override fun getModelSizeBytes(): Long = if (isModelDownloaded()) modelFile.length() else 0L

    override fun getModelFilePath(): String? = modelFile.absolutePath.takeIf { isModelDownloaded() }

    @Synchronized
    override fun beginDownload() {
        output?.close()
        output = null
        check(directory.exists() || directory.mkdirs()) { "No se pudo crear el directorio privado del modelo." }
        partialFile.delete()
        markerFile.resolveSibling("${markerFile.name}.tmp").delete()
        output = FileOutputStream(partialFile, false)
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
        stream.fd.sync()
        stream.close()
        output = null
        val actualDigest = checkNotNull(digest).digest().joinToString("") { "%02x".format(it) }
        digest = null
        check(sizeBytes == GemmaModelArtifact.sizeBytes && partialFile.length() == sizeBytes) {
            "El archivo descargado no coincide con el tamaño fijado para Gemma."
        }
        check(actualDigest.equals(GemmaModelArtifact.sha256, ignoreCase = true)) {
            "El SHA-256 del archivo persistido no coincide con la revisión fijada de Gemma."
        }

        val markerTemp = markerFile.resolveSibling("${markerFile.name}.tmp")
        markerTemp.writeText(markerContents)
        Files.move(partialFile.toPath(), modelFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Files.move(markerTemp.toPath(), markerFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    @Synchronized
    override fun discardPartialDownload() {
        output?.close()
        output = null
        digest = null
        partialFile.delete()
        markerFile.resolveSibling("${markerFile.name}.tmp").delete()
    }

    @Synchronized
    override fun deleteModel(): Boolean {
        output?.close()
        output = null
        digest = null
        var deleted = true
        for (file in listOf(modelFile, partialFile, markerFile, markerFile.resolveSibling("${markerFile.name}.tmp"))) {
            val removed = runCatching { !file.exists() || file.delete() }.getOrDefault(false)
            deleted = removed && deleted
        }
        return deleted
    }

    private val markerContents: String
        get() = "${GemmaModelArtifact.fileName}\n${GemmaModelArtifact.sizeBytes}\n${GemmaModelArtifact.revision}\n${GemmaModelArtifact.sha256}"
}
