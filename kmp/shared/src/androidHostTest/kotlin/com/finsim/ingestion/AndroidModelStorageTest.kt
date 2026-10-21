package com.finsim.ingestion

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
}
