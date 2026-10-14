package com.finsim.ingestion

/**
 * Seam for rendering PDF pages into bitmap/image byte arrays at a target DPI.
 */
interface PageRasterizer {
    /**
     * Renders a specific page (1-indexed) of a PDF to an image byte array.
     */
    suspend fun rasterizePage(pdfBytes: ByteArray, pageIndex: Int, dpi: Int = 150): ByteArray
}

/**
 * In-memory / mock implementation of [PageRasterizer] for tests, headless environments,
 * and cross-platform fallbacks.
 */
class InMemoryPageRasterizer(
    private val pageMap: Map<Int, ByteArray> = emptyMap(),
    private val defaultPayload: ByteArray = ByteArray(1024) { (it % 256).toByte() }
) : PageRasterizer {
    var callCount: Int = 0
        private set
    var lastRasterizedPage: Int? = null
        private set
    var lastDpi: Int? = null
        private set

    override suspend fun rasterizePage(pdfBytes: ByteArray, pageIndex: Int, dpi: Int): ByteArray {
        callCount++
        lastRasterizedPage = pageIndex
        lastDpi = dpi
        return pageMap[pageIndex] ?: defaultPayload
    }
}

/**
 * Platform expect declaration to provide native rasterizer implementations where available.
 */
expect fun getDefaultPageRasterizer(): PageRasterizer
