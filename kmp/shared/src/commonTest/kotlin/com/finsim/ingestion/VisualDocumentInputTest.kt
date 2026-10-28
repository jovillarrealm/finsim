package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VisualDocumentInputTest {
    @Test
    fun acceptsPngBySignatureAndReadsDimensions() {
        val dimensions = validateImageForInMemoryInference(png(width = 800, height = 600))

        assertEquals(800L, dimensions.width)
        assertEquals(600L, dimensions.height)
    }

    @Test
    fun acceptsJpegBySignatureAndReadsStartOfFrameDimensions() {
        val dimensions = validateImageForInMemoryInference(jpeg(width = 1024, height = 768))

        assertEquals(1024L, dimensions.width)
        assertEquals(768L, dimensions.height)
    }

    @Test
    fun rejectsUnsupportedOrTruncatedImagesBeforeInference() {
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference("not an image".encodeToByteArray())
        }
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47))
        }
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference(jpeg(width = 0, height = 768))
        }
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference(png(width = 800, height = 600), VisualDocumentFormat.JPEG)
        }
    }

    @Test
    fun rejectsImagesExceedingDimensionsOrPixelLimit() {
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference(png(width = 12_001, height = 1))
        }
        assertFailsWith<IllegalArgumentException> {
            validateImageForInMemoryInference(png(width = 4_000, height = 4_000))
        }
    }

    private fun png(width: Int, height: Int): ByteArray = ByteArray(24).apply {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        signature.copyInto(this)
        this[11] = 13
        "IHDR".encodeToByteArray().copyInto(this, 12)
        putUInt32(16, width)
        putUInt32(20, height)
    }

    private fun jpeg(width: Int, height: Int): ByteArray = ByteArray(21).apply {
        this[0] = 0xff.toByte()
        this[1] = 0xd8.toByte()
        this[2] = 0xff.toByte()
        this[3] = 0xc0.toByte()
        this[4] = 0
        this[5] = 17
        this[6] = 8
        putUInt16(7, height)
        putUInt16(9, width)
        this[11] = 3
    }

    private fun ByteArray.putUInt32(offset: Int, value: Int) {
        for (index in 0..3) this[offset + index] = (value ushr ((3 - index) * 8)).toByte()
    }

    private fun ByteArray.putUInt16(offset: Int, value: Int) {
        this[offset] = (value ushr 8).toByte()
        this[offset + 1] = value.toByte()
    }
}
