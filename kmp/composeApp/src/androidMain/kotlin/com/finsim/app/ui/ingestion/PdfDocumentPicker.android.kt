package com.finsim.app.ui.ingestion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.finsim.ingestion.MAX_INGESTION_PDF_BYTES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Composable
actual fun PdfDocumentPickerButton(
    enabled: Boolean,
    onPdfSelected: (name: String, bytes: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readPdfWithinLimit() }
                            ?: error("Android no pudo abrir el PDF seleccionado.")
                    }
                    check(bytes.isNotEmpty()) { "El PDF seleccionado está vacío." }
                    val name = uri.lastPathSegment
                        ?.substringAfterLast(':')
                        ?.substringAfterLast('/')
                        ?.takeIf(String::isNotBlank)
                        ?: "Documento PDF"
                    onPdfSelected(name, bytes)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    onError(failure.message ?: "No se pudo leer el PDF seleccionado.")
                }
            }
        }
    }

    OutlinedButton(
        onClick = { picker.launch(arrayOf("application/pdf")) },
        enabled = enabled
    ) {
        Text("Seleccionar PDF")
    }
}

private suspend fun InputStream.readPdfWithinLimit(): ByteArray {
    val output = ByteArrayOutputStream(64 * 1024)
    val buffer = ByteArray(64 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = read(buffer)
        if (count < 0) break
        require(output.size().toLong() + count <= MAX_INGESTION_PDF_BYTES) {
            "El PDF supera el límite de ${MAX_INGESTION_PDF_BYTES / (1024 * 1024)} MiB para análisis visual en memoria."
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
