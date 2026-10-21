package com.finsim.ingestion

import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidModelStorageTest {
    @Test
    fun lateCleanupFromOlderAttemptPreservesNewAttemptPartialFile() {
        val directory = Files.createTempDirectory("finsim-model-download-").toFile()
        val storage = AndroidModelStorage(directory)
        val olderAttempt = storage.createDownloadAttempt()
        val newerAttempt = storage.createDownloadAttempt()

        try {
            olderAttempt.beginDownload()
            olderAttempt.saveChunk(byteArrayOf(1), 1)
            newerAttempt.beginDownload()
            newerAttempt.saveChunk(byteArrayOf(2), 1)

            olderAttempt.discardPartialDownload()

            newerAttempt.saveChunk(byteArrayOf(3), 1)
            val partialFiles = directory.listFiles { file -> file.name.endsWith(".part") }.orEmpty()
            assertEquals(1, partialFiles.size)
            assertContentEquals(byteArrayOf(2, 3), partialFiles.single().readBytes())
        } finally {
            olderAttempt.deleteModel()
            newerAttempt.discardPartialDownload()
            directory.deleteRecursively()
        }
    }

    @Test
    fun startupAndDeleteRemoveUuidOrphansButPreserveActiveAttempts() {
        val directory = Files.createTempDirectory("finsim-model-orphans-").toFile()
        val activeStorage = AndroidModelStorage(directory)

        try {
            activeStorage.beginDownload()
            activeStorage.saveChunk(byteArrayOf(4, 5), 2)
            val activePart = directory.listFiles { file -> file.name.endsWith(".part") }.orEmpty().single()

            val staleId = UUID.randomUUID().toString()
            val stalePart = directory.resolve("${GemmaModelArtifact.fileName}.$staleId.part")
            val staleMarker = directory.resolve("${GemmaModelArtifact.fileName}.verified.$staleId.tmp")
            stalePart.writeBytes(byteArrayOf(8))
            staleMarker.writeText("stale marker")

            AndroidModelStorage(directory)

            assertFalse(stalePart.exists())
            assertFalse(staleMarker.exists())
            assertTrue(activePart.exists())
            assertContentEquals(byteArrayOf(4, 5), activePart.readBytes())

            val deleteCoordinator = AndroidModelStorage(directory)
            val deleteId = UUID.randomUUID().toString()
            val lateOrphanPart = directory.resolve("${GemmaModelArtifact.fileName}.$deleteId.part")
            val lateOrphanMarker = directory.resolve("${GemmaModelArtifact.fileName}.verified.$deleteId.tmp")
            lateOrphanPart.writeBytes(byteArrayOf(9))
            lateOrphanMarker.writeText("late orphan")

            assertTrue(deleteCoordinator.deleteModel())
            assertFalse(lateOrphanPart.exists())
            assertFalse(lateOrphanMarker.exists())
            assertTrue(activePart.exists())
            assertContentEquals(byteArrayOf(4, 5), activePart.readBytes())
        } finally {
            activeStorage.discardPartialDownload()
            directory.deleteRecursively()
        }
    }
}
