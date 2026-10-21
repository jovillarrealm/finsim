package com.finsim.ingestion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AndroidModelDownloader : ModelDownloader {
    override suspend fun download(storage: ModelStorage, onProgress: (Long) -> Unit): ModelDownloadResult =
        withContext(Dispatchers.IO) {
            val connection = URL(GemmaModelArtifact.downloadUrl).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.setRequestProperty("Accept-Encoding", "identity")
                val responseCode = connection.responseCode
                check(responseCode in 200..299) { "La descarga del modelo respondió HTTP $responseCode." }
                val declaredSize = connection.contentLengthLong
                check(declaredSize == -1L || declaredSize == GemmaModelArtifact.sizeBytes) {
                    "El servidor anunció $declaredSize bytes; se esperaban ${GemmaModelArtifact.sizeBytes}."
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                connection.inputStream.use { input ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        downloaded += count
                        check(downloaded <= GemmaModelArtifact.sizeBytes) { "La descarga excedió el tamaño fijado del modelo." }
                        digest.update(buffer, 0, count)
                        storage.saveChunk(buffer, count)
                        onProgress(downloaded)
                    }
                }
                currentCoroutineContext().ensureActive()
                ModelDownloadResult(
                    fileName = GemmaModelArtifact.fileName,
                    sizeBytes = downloaded,
                    sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                )
            } finally {
                connection.disconnect()
            }
        }
}
