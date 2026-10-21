package com.finsim.ingestion

import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidModelDownloaderCancellationTest {
    @Test
    fun cancellationCancelsHttpCallWhileResponseBodyReadIsBlocked() = runBlocking {
        val server = ServerSocket(0)
        val firstByteSent = CountDownLatch(1)
        val secondReadEntered = CountDownLatch(1)
        val stopServer = CountDownLatch(1)
        val serverThread = thread(isDaemon = true, name = "finsim-model-http-test") {
            val socket = server.accept()
            socket.use {
                val reader = it.getInputStream().bufferedReader()
                while (true) {
                    val header = reader.readLine() ?: error("HTTP client closed before request headers ended.")
                    if (header.isEmpty()) break
                }
                val output = it.getOutputStream()
                output.write(
                    "HTTP/1.1 200 OK\r\nContent-Length: ${GemmaModelArtifact.sizeBytes}\r\nConnection: keep-alive\r\n\r\n".toByteArray()
                )
                output.write(byteArrayOf(0x42))
                output.flush()
                firstByteSent.countDown()
                stopServer.await(5, TimeUnit.SECONDS)
            }
        }

        val localUrl = "http://127.0.0.1:${server.localPort}/model"
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val originalBody = response.body
                response.newBuilder().body(object : ResponseBody() {
                    private val trackedSource = object : ForwardingSource(originalBody.source()) {
                        private var reads = 0

                        override fun read(sink: Buffer, byteCount: Long): Long {
                            if (++reads == 2) secondReadEntered.countDown()
                            return super.read(sink, byteCount)
                        }
                    }.buffer()

                    override fun contentType() = originalBody.contentType()

                    override fun contentLength() = originalBody.contentLength()

                    override fun source(): BufferedSource = trackedSource
                }).build()
            }
            .build()
        var issuedCall: Call? = null
        val callFactory = Call.Factory { request: Request ->
            client.newCall(request.newBuilder().url(localUrl).build()).also { issuedCall = it }
        }
        val downloader = AndroidModelDownloader(callFactory)
        val directory = Files.createTempDirectory("finsim-model-read-cancel-").toFile()
        val storage = AndroidModelStorage(directory)
        storage.beginDownload()
        val firstProgress = CountDownLatch(1)
        val download = launch(Dispatchers.IO) {
            downloader.download(storage) { firstProgress.countDown() }
        }
        val downloadCompleted = CountDownLatch(1)
        download.invokeOnCompletion { downloadCompleted.countDown() }

        var cancellationReturnedPromptly = false
        var completedPromptly = false
        val cancellationReturned = CountDownLatch(1)
        var cancelThread: Thread? = null
        try {
            assertTrue(firstByteSent.await(2, TimeUnit.SECONDS))
            assertTrue(firstProgress.await(2, TimeUnit.SECONDS))
            assertTrue(secondReadEntered.await(2, TimeUnit.SECONDS))
            val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1_200)
            cancelThread = thread(isDaemon = true, name = "finsim-model-cancel-test") {
                download.cancel()
                cancellationReturned.countDown()
            }
            cancellationReturnedPromptly = awaitBefore(deadlineNanos, cancellationReturned)
            completedPromptly = awaitBefore(deadlineNanos, downloadCompleted)
        } finally {
            issuedCall?.cancel()
            stopServer.countDown()
            runCatching { server.close() }
            download.cancelAndJoin()
            cancelThread?.join(1_000)
            serverThread.join(1_000)
            storage.discardPartialDownload()
            directory.deleteRecursively()
        }

        assertTrue(
            cancellationReturnedPromptly && completedPromptly && issuedCall?.isCanceled() == true,
            "Within one 1.2-second cancellation window: cancelReturned=$cancellationReturnedPromptly, " +
                "downloadCompleted=$completedPromptly, callCanceled=${issuedCall?.isCanceled()}."
        )
    }

    private fun awaitBefore(deadlineNanos: Long, latch: CountDownLatch): Boolean {
        val remainingNanos = deadlineNanos - System.nanoTime()
        return remainingNanos > 0 && latch.await(remainingNanos, TimeUnit.NANOSECONDS)
    }
}
