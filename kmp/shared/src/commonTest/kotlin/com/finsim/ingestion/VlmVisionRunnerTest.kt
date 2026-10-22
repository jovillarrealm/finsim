package com.finsim.ingestion

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VlmVisionRunnerTest {
    @Test
    fun fakeRunnerNeverTreatsRasterBytesAsInference() = runTest {
        val runner = FakeVlmVisionRunner()
        val image = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)

        assertFalse(runner.isModelReady())
        val failure = try {
            runner.analyzePage(image, RegulatoryFramework.COLOMBIA_SFC)
            null
        } catch (error: IllegalStateException) {
            error
        }

        assertNotNull(failure)
        assertTrue(failure.message.orEmpty().contains("no realiza inferencia"))
        assertEquals(1, runner.callCount)
        assertTrue(runner.lastAnalyzedImageBytes!!.contentEquals(image))
    }
}
