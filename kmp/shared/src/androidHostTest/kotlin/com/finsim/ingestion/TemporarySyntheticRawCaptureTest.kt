package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemporarySyntheticRawCaptureTest {
    @Test
    fun captureRequiresTheExactSyntheticSourceAndItsRasterAndCanRunOnlyOnce() {
        val raster = byteArrayOf(1, 2, 3)

        TemporarySyntheticRawCapture.armAfterRaster(TemporarySyntheticRawCapture.PDF_SHA256, raster)
        assertFalse(TemporarySyntheticRawCapture.consumeForInference(byteArrayOf(3, 2, 1)))
        assertFalse(TemporarySyntheticRawCapture.consumeForInference(raster))

        TemporarySyntheticRawCapture.armAfterRaster("0".repeat(64), raster)
        assertFalse(TemporarySyntheticRawCapture.consumeForInference(raster))

        TemporarySyntheticRawCapture.armAfterRaster(TemporarySyntheticRawCapture.PDF_SHA256, raster)
        assertTrue(TemporarySyntheticRawCapture.consumeForInference(raster))
        assertFalse(TemporarySyntheticRawCapture.consumeForInference(raster))
    }
}
