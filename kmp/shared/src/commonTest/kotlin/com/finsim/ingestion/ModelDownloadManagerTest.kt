package com.finsim.ingestion

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelDownloadManagerTest {
    @Test
    fun downloadManagerAcceptsOnlyPinnedMetadataFromItsDownloader() = runTest {
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            downloader = ModelDownloader { target, onProgress ->
                target.saveChunk(byteArrayOf(1, 2, 3))
                onProgress(GemmaModelArtifact.sizeBytes / 2)
                ModelDownloadResult(GemmaModelArtifact.fileName, GemmaModelArtifact.sizeBytes, GemmaModelArtifact.sha256)
            },
            coroutineScope = backgroundScope
        )

        manager.startDownload()
        runCurrent()

        assertIs<DownloadState.Completed>(manager.state.value)
        assertEquals(GemmaModelArtifact.sizeBytes, storage.getModelSizeBytes())
        assertTrue(manager.isModelDownloaded())
    }

    @Test
    fun wrongArtifactHashLeavesNoDownloadedModel() = runTest {
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            downloader = ModelDownloader { _, _ ->
                ModelDownloadResult(GemmaModelArtifact.fileName, GemmaModelArtifact.sizeBytes, "wrong")
            },
            coroutineScope = backgroundScope
        )

        manager.startDownload()
        runCurrent()

        val error = assertIs<DownloadState.Error>(manager.state.value)
        assertTrue(error.message.contains("SHA-256"))
        assertFalse(manager.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())
    }

    @Test
    fun lowTotalRamBlocksDownloadBeforeCallingDownloader() = runTest {
        var downloadCalled = false
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 4.0),
            downloader = ModelDownloader { _, _ ->
                downloadCalled = true
                error("The blocked download must not start.")
            },
            coroutineScope = backgroundScope
        )

        manager.startDownload()
        runCurrent()

        assertFalse(downloadCalled)
        assertTrue(assertIs<DownloadState.Error>(manager.state.value).message.contains("8.0 GiB"))
        assertFalse(manager.isModelDownloaded())
    }

    @Test
    fun deletingVerifiedModelClearsStorageAndState() {
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val manager = ModelDownloadManager(
            storage = storage,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 12.0),
            downloader = ModelDownloader { _, _ -> error("No download expected.") }
        )

        assertIs<DownloadState.Completed>(manager.state.value)
        assertTrue(manager.deleteModel())
        assertEquals(DownloadState.Idle, manager.state.value)
        assertFalse(manager.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())
    }

    @Test
    fun pinnedArtifactIncludesExactExtensionSizeAndDigest() {
        assertEquals(null, GemmaModelArtifact.validationError(
            GemmaModelArtifact.fileName,
            GemmaModelArtifact.sizeBytes,
            GemmaModelArtifact.sha256
        ))
        assertTrue(GemmaModelArtifact.validationError("gemma.bin", GemmaModelArtifact.sizeBytes, GemmaModelArtifact.sha256)
            ?.contains("filename", ignoreCase = true) == true)
        assertTrue(GemmaModelArtifact.validationError(GemmaModelArtifact.fileName, GemmaModelArtifact.sizeBytes - 1, GemmaModelArtifact.sha256)
            ?.contains("size", ignoreCase = true) == true)
        assertTrue(GemmaModelArtifact.validationError(GemmaModelArtifact.fileName, GemmaModelArtifact.sizeBytes, "bad")
            ?.contains("SHA-256") == true)
    }
}
