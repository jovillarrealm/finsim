package com.finsim.ingestion

import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

class AndroidModelDownloader internal constructor(
    private val callFactory: Call.Factory
) : ModelDownloader {
    constructor() : this(DEFAULT_HTTP_CLIENT)

    override suspend fun download(storage: ModelStorage, onProgress: (Long) -> Unit): ModelDownloadResult {
        val request = Request.Builder()
            .url(GemmaModelArtifact.downloadUrl)
            .header("Accept-Encoding", "identity")
            .build()
        // Create the cancellable call before starting the blocking worker so cancellation can
        // always reach the same call, including the handoff into execute().
        val call = callFactory.newCall(request)
        val outcome = CompletableDeferred<Result<ModelDownloadResult>>()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val worker = workerScope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                call.execute().use { response ->
                    check(response.isSuccessful) {
                        "La descarga del modelo respondió HTTP ${response.code}."
                    }
                    val declaredSize = response.body.contentLength()
                    check(declaredSize == -1L || declaredSize == GemmaModelArtifact.sizeBytes) {
                        "El servidor anunció $declaredSize bytes; se esperaban ${GemmaModelArtifact.sizeBytes}."
                    }

                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloaded = 0L
                    response.body.byteStream().use { input ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            // Do not persist or publish a chunk delivered at the same time as cancel.
                            currentCoroutineContext().ensureActive()
                            if (count < 0) break
                            if (count == 0) continue
                            downloaded += count
                            check(downloaded <= GemmaModelArtifact.sizeBytes) {
                                "La descarga excedió el tamaño fijado del modelo."
                            }
                            digest.update(buffer, 0, count)
                            currentCoroutineContext().ensureActive()
                            storage.saveChunk(buffer, count)
                            currentCoroutineContext().ensureActive()
                            onProgress(downloaded)
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    Result.success(
                        ModelDownloadResult(
                            fileName = GemmaModelArtifact.fileName,
                            sizeBytes = downloaded,
                            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                        )
                    )
                }
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
            outcome.complete(result)
        }

        worker.start()
        return try {
            outcome.await().getOrThrow()
        } finally {
            try {
                if (!worker.isCompleted) call.cancel()
            } finally {
                worker.cancel()
                withContext(NonCancellable) { worker.join() }
                workerScope.cancel()
            }
        }
    }

    private companion object {
        val DEFAULT_HTTP_CLIENT = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
