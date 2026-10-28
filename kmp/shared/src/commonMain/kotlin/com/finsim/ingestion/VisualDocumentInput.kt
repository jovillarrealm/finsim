package com.finsim.ingestion

/** Accepted visual inputs. Image bytes remain binary and are never sent to the text parser. */
enum class VisualDocumentFormat {
    PDF,
    PNG,
    JPEG
}

data class RasterImageDimensions(val width: Long, val height: Long)

const val MAX_VISUAL_DOCUMENT_BYTES: Int = MAX_INGESTION_PDF_BYTES

internal fun validateImageForInMemoryInference(
    imageBytes: ByteArray,
    expectedFormat: VisualDocumentFormat? = null
): RasterImageDimensions {
    require(imageBytes.isNotEmpty()) { "La imagen seleccionada está vacía." }
    require(imageBytes.size <= MAX_VISUAL_DOCUMENT_BYTES) {
        "La imagen supera el límite de ${MAX_VISUAL_DOCUMENT_BYTES / (1024 * 1024)} MiB para análisis visual en memoria."
    }

    val actualFormat = when {
        imageBytes.hasPngSignature() -> VisualDocumentFormat.PNG
        imageBytes.hasJpegSignature() -> VisualDocumentFormat.JPEG
        else -> throw IllegalArgumentException("Formato de imagen no admitido; selecciona PNG o JPEG válido.")
    }
    require(expectedFormat == null || expectedFormat == actualFormat) {
        "El contenido de imagen no coincide con el formato seleccionado."
    }

    val dimensions = when (actualFormat) {
        VisualDocumentFormat.PNG -> imageBytes.readPngDimensions()
        VisualDocumentFormat.JPEG -> imageBytes.readJpegDimensions()
        VisualDocumentFormat.PDF -> error("Un PDF no es una entrada de imagen rasterizada.")
    }

    validateRasterDimensions(dimensions.width, dimensions.height)
    return dimensions
}

private fun ByteArray.hasPngSignature(): Boolean {
    val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    return size >= signature.size && signature.indices.all { this[it] == signature[it] }
}

private fun ByteArray.readPngDimensions(): RasterImageDimensions {
    require(size >= 24) { "La imagen PNG está truncada." }
    require(this[12] == 'I'.code.toByte() && this[13] == 'H'.code.toByte() &&
        this[14] == 'D'.code.toByte() && this[15] == 'R'.code.toByte()
    ) { "La imagen PNG no contiene un encabezado IHDR válido." }
    require(readUInt32BigEndian(8) == 13L) { "La imagen PNG contiene un encabezado IHDR inválido." }
    return RasterImageDimensions(
        width = readUInt32BigEndian(16),
        height = readUInt32BigEndian(20)
    )
}

private fun ByteArray.hasJpegSignature(): Boolean =
    size >= 4 && unsignedAt(0) == 0xff && unsignedAt(1) == 0xd8 && unsignedAt(2) == 0xff

private fun ByteArray.readJpegDimensions(): RasterImageDimensions {
    var offset = 2
    while (offset < size) {
        while (offset < size && unsignedAt(offset) != 0xff) offset++
        require(offset < size) { "La imagen JPEG está truncada antes de sus dimensiones." }
        while (offset < size && unsignedAt(offset) == 0xff) offset++
        require(offset < size) { "La imagen JPEG está truncada antes de sus dimensiones." }

        val marker = unsignedAt(offset++)
        if (marker == 0xd9 || marker == 0xda) break
        if (marker == 0x01 || marker in 0xd0..0xd7) continue
        require(offset + 1 < size) { "La imagen JPEG contiene un segmento truncado." }

        val segmentLength = (unsignedAt(offset) shl 8) or unsignedAt(offset + 1)
        require(segmentLength >= 2 && offset.toLong() + segmentLength <= size.toLong()) {
            "La imagen JPEG contiene un segmento inválido."
        }

        if (marker.isStartOfFrame()) {
            require(segmentLength >= 7) { "La imagen JPEG contiene dimensiones truncadas." }
            return RasterImageDimensions(
                width = readUInt16BigEndian(offset + 5),
                height = readUInt16BigEndian(offset + 3)
            )
        }
        offset += segmentLength
    }
    throw IllegalArgumentException("La imagen JPEG no contiene dimensiones válidas.")
}

private fun Int.isStartOfFrame(): Boolean = this in setOf(
    0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7, 0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf
)

private fun ByteArray.unsignedAt(index: Int): Int = this[index].toInt() and 0xff

private fun ByteArray.readUInt16BigEndian(offset: Int): Long =
    ((unsignedAt(offset).toLong() shl 8) or unsignedAt(offset + 1).toLong())

private fun ByteArray.readUInt32BigEndian(offset: Int): Long =
    (unsignedAt(offset).toLong() shl 24) or
        (unsignedAt(offset + 1).toLong() shl 16) or
        (unsignedAt(offset + 2).toLong() shl 8) or
        unsignedAt(offset + 3).toLong()
