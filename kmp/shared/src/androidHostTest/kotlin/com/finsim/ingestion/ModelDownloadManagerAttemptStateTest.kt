package com.finsim.ingestion

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelDownloadManagerAttemptStateTest {
    @Test
    fun lateFailureFromCancelledAttemptCannotOverwriteActiveRetry() = runBlocking {
        val firstEnteredNonCancellableCleanup = CompletableDeferred<Unit>()
        val releaseFirstFailure = CompletableDeferred<Unit>()
        val firstAttemptDiscarded = CompletableDeferred<Unit>()
        val firstAttempt = CompletableDeferred<Job>()
        val downloaderCalls = AtomicInteger()
        val storage = AttemptStorageFactory(firstAttemptDiscarded = firstAttemptDiscarded)
        val scopeJob = SupervisorJob()
        val scope = CoroutineScope(scopeJob + Dispatchers.IO)
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            downloader = ModelDownloader { _, onProgress ->
                when (downloaderCalls.incrementAndGet()) {
                    1 -> {
                        firstAttempt.complete(checkNotNull(currentCoroutineContext()[Job]))
                        onProgress(1L)
                        try {
                            awaitCancellation()
                        } catch (_: kotlinx.coroutines.CancellationException) {
                            firstEnteredNonCancellableCleanup.complete(Unit)
                            withContext(NonCancellable) { releaseFirstFailure.await() }
                            throw IOException("late failure from canceled attempt")
                        }
                    }
                    else -> {
                        onProgress(2L)
                        awaitCancellation()
                    }
                }
            },
            coroutineScope = scope
        )

        try {
            manager.startDownload()
            awaitCondition { downloaderCalls.get() == 1 }
            val firstAttemptJob = firstAttempt.await()
            manager.cancelDownload()
            assertEquals(DownloadState.Idle, manager.state.value)
            firstEnteredNonCancellableCleanup.await()

            manager.startDownload()
            awaitCondition { downloaderCalls.get() == 2 }
            val retryState = assertIs<DownloadState.Downloading>(manager.state.value)
            releaseFirstFailure.complete(Unit)
            firstAttemptDiscarded.await()
            withTimeout(2_000) { firstAttemptJob.join() }

            assertEquals(retryState, manager.state.value)
        } finally {
            releaseFirstFailure.complete(Unit)
            manager.cancelDownload()
            scope.cancel()
        }
    }

    @Test
    fun completionFromAttemptCancelledDuringCommitCannotReplaceRetryState() = runBlocking {
        val firstMarkEntered = CountDownLatch(1)
        val releaseFirstMark = CountDownLatch(1)
        val firstMarkReturned = CountDownLatch(1)
        val secondDownloadStarted = CountDownLatch(1)
        val downloaderCalls = AtomicInteger()
        val firstAttempt = CompletableDeferred<Job>()
        val storage = DelayedFirstMarkStorage(firstMarkEntered, releaseFirstMark, firstMarkReturned)
        val scopeJob = SupervisorJob()
        val scope = CoroutineScope(scopeJob + Dispatchers.IO)
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            downloader = ModelDownloader { _, onProgress ->
                when (downloaderCalls.incrementAndGet()) {
                    1 -> {
                        firstAttempt.complete(checkNotNull(currentCoroutineContext()[Job]))
                        ModelDownloadResult(
                            GemmaModelArtifact.fileName,
                            GemmaModelArtifact.sizeBytes,
                            GemmaModelArtifact.sha256
                        )
                    }
                    else -> {
                        secondDownloadStarted.countDown()
                        onProgress(2L)
                        awaitCancellation()
                    }
                }
            },
            coroutineScope = scope
        )

        try {
            manager.startDownload()
            assertTrue(firstMarkEntered.await(2, TimeUnit.SECONDS))
            val firstAttemptJob = firstAttempt.await()
            manager.cancelDownload()
            assertEquals(DownloadState.Idle, manager.state.value)
            manager.startDownload()
            assertTrue(secondDownloadStarted.await(2, TimeUnit.SECONDS))
            val retryState = assertIs<DownloadState.Downloading>(manager.state.value)

            releaseFirstMark.countDown()
            assertTrue(firstMarkReturned.await(2, TimeUnit.SECONDS))
            withTimeout(2_000) { firstAttemptJob.join() }

            assertEquals(retryState, manager.state.value)
        } finally {
            releaseFirstMark.countDown()
            manager.cancelDownload()
            scope.cancel()
        }
    }

    private suspend fun awaitCondition(predicate: () -> Boolean) {
        withTimeout(2_000) {
            while (!predicate()) delay(10)
        }
    }

    private class AttemptStorageFactory(
        private val firstAttemptDiscarded: CompletableDeferred<Unit>
    ) : ModelStorage by InMemoryModelStorage() {
        private val attemptCounter = AtomicInteger()

        override fun createDownloadAttempt(): ModelStorage {
            val attemptNumber = attemptCounter.incrementAndGet()
            val delegate = InMemoryModelStorage()
            return object : ModelStorage by delegate {
                override fun discardPartialDownload() {
                    delegate.discardPartialDownload()
                    if (attemptNumber == 1) firstAttemptDiscarded.complete(Unit)
                }
            }
        }
    }

    private class DelayedFirstMarkStorage(
        private val firstMarkEntered: CountDownLatch,
        private val releaseFirstMark: CountDownLatch,
        private val firstMarkReturned: CountDownLatch
    ) : ModelStorage {
        private val attemptCounter = AtomicInteger()
        private val downloaded = AtomicBoolean(false)
        private val sizeBytes = AtomicInteger()

        override fun isModelDownloaded(): Boolean = downloaded.get()
        override fun getModelSizeBytes(): Long = sizeBytes.get().toLong()
        override fun beginDownload() = Unit
        override fun saveChunk(bytes: ByteArray, length: Int) = Unit
        override fun markDownloaded(sizeBytes: Long) = Unit
        override fun discardPartialDownload() = Unit
        override fun deleteModel(): Boolean {
            downloaded.set(false)
            sizeBytes.set(0)
            return true
        }

        override fun createDownloadAttempt(): ModelStorage {
            val attemptNumber = attemptCounter.incrementAndGet()
            return object : ModelStorage by this {
                override fun createDownloadAttempt(): ModelStorage = this

                override fun markDownloaded(sizeBytes: Long) {
                    if (attemptNumber == 1) {
                        firstMarkEntered.countDown()
                        check(releaseFirstMark.await(5, TimeUnit.SECONDS)) { "Test did not release the first markDownloaded call." }
                        firstMarkReturned.countDown()
                    }
                    this@DelayedFirstMarkStorage.sizeBytes.set(sizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    downloaded.set(true)
                }
            }
        }
    }
}
