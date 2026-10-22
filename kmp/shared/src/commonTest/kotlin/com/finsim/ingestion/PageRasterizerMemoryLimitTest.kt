package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PageRasterizerMemoryLimitTest {
    @Test
    fun allowsTheConfiguredTwelveMegapixelBoundary() {
        validateRasterDimensions(3_000, 4_000)
        assertEquals(12_000_000L, MAX_RASTER_PAGE_PIXELS)
    }

    @Test
    fun rejectsRasterDimensionsThatExceedTheMemoryBound() {
        val failure = assertFailsWith<IllegalArgumentException> {
            validateRasterDimensions(3_000, 4_001)
        }
        assertTrue(failure.message.orEmpty().contains("12 megapíxeles"))
    }

    @Test
    fun rejectsAnOversizedSingleDimensionBeforeBitmapAllocation() {
        val failure = assertFailsWith<IllegalArgumentException> {
            validateRasterDimensions(MAX_RASTER_DIMENSION + 1, 1)
        }
        assertTrue(failure.message.orEmpty().contains("por dimensión"))
    }
}
