package com.finsim.ingestion

/** Upper bound for a PDF kept in memory during on-device visual extraction. */
const val MAX_INGESTION_PDF_BYTES: Int = 25 * 1024 * 1024
internal const val MAX_RASTER_DIMENSION: Long = 12_000L
internal const val MAX_RASTER_PAGE_PIXELS: Long = 12_000_000L

internal fun validatePdfForInMemoryRasterization(pdfBytes: ByteArray) {
    require(pdfBytes.isNotEmpty()) { "El archivo PDF está vacío." }
    require(pdfBytes.size <= MAX_INGESTION_PDF_BYTES) {
        "El PDF supera el límite de ${MAX_INGESTION_PDF_BYTES / (1024 * 1024)} MiB para análisis visual en memoria."
    }
}

internal fun validateRasterDimensions(width: Long, height: Long) {
    require(width in 1..MAX_RASTER_DIMENSION && height in 1..MAX_RASTER_DIMENSION) {
        "La página del PDF excede el límite de ${MAX_RASTER_DIMENSION} píxeles por dimensión."
    }
    require(width * height <= MAX_RASTER_PAGE_PIXELS) {
        "La página del PDF excede el límite de 12 megapíxeles para rasterización en memoria."
    }
}

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
