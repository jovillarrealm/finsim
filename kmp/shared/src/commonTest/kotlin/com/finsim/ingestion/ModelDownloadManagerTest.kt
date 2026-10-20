package com.finsim.ingestion

import kotlin.test.*

class ModelDownloadManagerTest {

    @Test
    fun testDownloadStateLifecycleIdleToDownloadingToCompleted() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(storage = storage, capabilityGate = gate)

        // 1. Initially Idle
        assertEquals(DownloadState.Idle, manager.state.value)
        assertFalse(manager.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())

        // 2. Step 1 -> Downloading
        manager.startDownload(simulateStep = true)
        val downloadingState = manager.state.value
        assertTrue(downloadingState is DownloadState.Downloading)
        assertEquals(0.5f, downloadingState.progress)
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES / 2, downloadingState.downloadedBytes)
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES, downloadingState.totalBytes)
        assertEquals("15 MB/s", downloadingState.speedFormatted)

        // 3. Step 2 -> Completed
        manager.startDownload(simulateStep = true)
        val completedState = manager.state.value
        assertTrue(completedState is DownloadState.Completed)
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES, completedState.totalSizeBytes)
        assertTrue(manager.isModelDownloaded())
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES, storage.getModelSizeBytes())
    }

    @Test
    fun testIneligibilityRejectionWhenRamLessThan8GiB() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 4.0)
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(storage = storage, capabilityGate = gate)

        assertEquals(DownloadState.Idle, manager.state.value)

        // Must reject download and throw IllegalStateException or set Error state
        val error = assertFailsWith<IllegalStateException> {
            manager.startDownload(simulateStep = true)
        }

        assertTrue(error.message?.contains("LMKD") == true || error.message?.contains("8") == true)
        val state = manager.state.value
        assertTrue(state is DownloadState.Error)
        assertFalse(manager.isModelDownloaded())
    }

    @Test
    fun testModelDeletionFreesStorageAndResetsState() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 12.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GEMMA_4_E2B_INT4_SIZE_BYTES)
        val manager = ModelDownloadManager(storage = storage, capabilityGate = gate)

        // State initialized as Completed when storage already has model
        assertTrue(manager.state.value is DownloadState.Completed)
        assertTrue(manager.isModelDownloaded())

        // Delete model
        val deleted = manager.deleteModel()
        assertTrue(deleted)
        assertEquals(DownloadState.Idle, manager.state.value)
        assertFalse(manager.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())
    }

    @Test
    fun testCancelDownloadResetsToIdle() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(storage = storage, capabilityGate = gate)

        manager.startDownload(simulateStep = true)
        assertTrue(manager.state.value is DownloadState.Downloading)

        manager.cancelDownload()
        assertEquals(DownloadState.Idle, manager.state.value)
        assertFalse(manager.isModelDownloaded())
    }

    @Test
    fun testCompleteDownloadExplicitly() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage()
        val manager = ModelDownloadManager(storage = storage, capabilityGate = gate)

        manager.completeDownload()
        assertTrue(manager.state.value is DownloadState.Completed)
        assertTrue(manager.isModelDownloaded())
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES, storage.getModelSizeBytes())
    }

    @Test
    fun testPersistentDiskModelStorageLifecycle() {
        val storage = PersistentDiskModelStorage("models/gemma4-e2b-int4")
        assertFalse(storage.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())

        storage.saveChunk(ByteArray(1024))
        assertEquals(1024L, storage.getModelSizeBytes())

        storage.markDownloaded(GEMMA_4_E2B_INT4_SIZE_BYTES)
        assertTrue(storage.isModelDownloaded())
        assertEquals(GEMMA_4_E2B_INT4_SIZE_BYTES, storage.getModelSizeBytes())

        val deleted = storage.deleteModel()
        assertTrue(deleted)
        assertFalse(storage.isModelDownloaded())
        assertEquals(0L, storage.getModelSizeBytes())
    }
}
