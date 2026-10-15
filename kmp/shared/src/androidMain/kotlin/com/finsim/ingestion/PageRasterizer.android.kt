package com.finsim.ingestion

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android native rasterizer implementation using [PdfRenderer] at 150 DPI ARGB_8888.
 */
class AndroidPdfRasterizer : PageRasterizer {

    override suspend fun rasterizePage(pdfBytes: ByteArray, pageIndex: Int, dpi: Int): ByteArray = withContext(Dispatchers.IO) {
        if (pdfBytes.isEmpty()) return@withContext ByteArray(0)

        val tempFile = File.createTempFile("finsim_raster_", ".pdf")
        try {
            tempFile.writeBytes(pdfBytes)
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount == 0) return@withContext ByteArray(0)
                    val targetPage = (pageIndex - 1).coerceIn(0, renderer.pageCount - 1)
                    val page = renderer.openPage(targetPage)
                    try {
                        val scale = dpi.toFloat() / 72f
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        stream.toByteArray()
                    } finally {
                        page.close()
                    }
                }
            }
        } finally {
            tempFile.delete()
        }
    }
}

actual fun getDefaultPageRasterizer(): PageRasterizer = AndroidPdfRasterizer()
