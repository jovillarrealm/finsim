package com.finsim.ingestion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

object AndroidIngestionContext {
    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
    }

    internal fun require(): Context = checkNotNull(appContext) {
        "Android ingestion requires the application context."
    }
}

class AndroidPdfRasterizer(private val context: Context) : PageRasterizer {
    override suspend fun rasterizePage(pdfBytes: ByteArray, pageIndex: Int, dpi: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val sourcePdfSha256 = TemporarySyntheticRawCapture.beginPdf(pdfBytes)
            validatePdfForInMemoryRasterization(pdfBytes)
            require(pageIndex > 0) { "El número de página debe empezar en 1." }
            require(dpi > 0) { "La resolución debe ser positiva." }
            val storageManager = checkNotNull(context.applicationContext.getSystemService(StorageManager::class.java)) {
                "Android no permite crear un descriptor PDF en memoria."
            }
            val callbackThread = HandlerThread("finsim-pdf-memory").apply { start() }
            try {
                val descriptor = storageManager.openProxyFileDescriptor(
                    ParcelFileDescriptor.MODE_READ_ONLY,
                    ByteArrayPdfDescriptor(pdfBytes),
                    Handler(callbackThread.looper)
                )
                val renderer = try {
                    PdfRenderer(descriptor)
                } catch (failure: Exception) {
                    descriptor.close()
                    throw failure
                }
                val rasterBytes = renderer.use {
                    require(pageIndex <= it.pageCount) {
                        "La página $pageIndex no existe; el PDF tiene ${it.pageCount} páginas."
                    }
                    val page = it.openPage(pageIndex - 1)
                    try {
                        currentCoroutineContext().ensureActive()
                        val scale = dpi.toFloat() / 72f
                        val width = ceil(page.width * scale).toLong().coerceAtLeast(1L)
                        val height = ceil(page.height * scale).toLong().coerceAtLeast(1L)
                        validateRasterDimensions(width, height)
                        try {
                            val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
                            try {
                                currentCoroutineContext().ensureActive()
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                currentCoroutineContext().ensureActive()
                                ByteArrayOutputStream().use { output ->
                                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                                        "Android no pudo codificar la página rasterizada."
                                    }
                                    output.toByteArray()
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        } catch (oom: OutOfMemoryError) {
                            throw IllegalStateException(
                                "Android no tiene memoria suficiente para rasterizar esta página. Cierra otras apps o usa una página más pequeña.",
                                oom
                            )
                        }
                    } finally {
                        page.close()
                    }
                }
                TemporarySyntheticRawCapture.armAfterRaster(sourcePdfSha256, rasterBytes)
                rasterBytes
            } finally {
                callbackThread.quitSafely()
            }
        }
}

private class ByteArrayPdfDescriptor(private val bytes: ByteArray) : ProxyFileDescriptorCallback() {
    override fun onGetSize(): Long = bytes.size.toLong()

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        if (offset < 0 || size < 0) throw ErrnoException("read", OsConstants.EINVAL)
        if (offset >= bytes.size) return 0
        val count = minOf(size, bytes.size - offset.toInt())
        bytes.copyInto(data, destinationOffset = 0, startIndex = offset.toInt(), endIndex = offset.toInt() + count)
        return count
    }

    override fun onWrite(offset: Long, size: Int, data: ByteArray): Int = throw ErrnoException("write", OsConstants.EROFS)

    override fun onRelease() = Unit
}

actual fun getDefaultPageRasterizer(): PageRasterizer = AndroidPdfRasterizer(AndroidIngestionContext.require())
