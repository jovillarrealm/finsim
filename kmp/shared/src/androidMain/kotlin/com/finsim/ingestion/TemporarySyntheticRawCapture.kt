package com.finsim.ingestion

import java.security.MessageDigest

// Temporary: raw model output is enabled only for this exact synthetic PDF and its raster.
internal object TemporarySyntheticRawCapture {
    const val PDF_SHA256 = "2324b53f2250c8627a5f30c4a115712d5e92a5b5c549114b20ef6c843f84c5a0"

    private var pendingRasterSha256: String? = null

    @Synchronized
    fun beginPdf(pdfBytes: ByteArray): String {
        pendingRasterSha256 = null
        return sha256(pdfBytes)
    }

    @Synchronized
    fun armAfterRaster(sourcePdfSha256: String, rasterBytes: ByteArray) {
        pendingRasterSha256 = if (sourcePdfSha256.equals(PDF_SHA256, ignoreCase = true)) {
            sha256(rasterBytes)
        } else {
            null
        }
    }

    @Synchronized
    fun consumeForInference(rasterBytes: ByteArray): Boolean {
        val expectedRasterSha256 = pendingRasterSha256
        pendingRasterSha256 = null
        return expectedRasterSha256 != null && expectedRasterSha256.equals(sha256(rasterBytes), ignoreCase = true)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
