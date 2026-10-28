package com.finsim.app.ui.ingestion

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.finsim.ingestion.MAX_VISUAL_DOCUMENT_BYTES
import com.finsim.ingestion.VisualDocumentFormat
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.js

private fun openVisualDocument(
    maxBytes: Int,
    onSelected: (name: String, format: String, bytesBase64: String) -> Unit,
    onError: (String) -> Unit
): Unit = js("""{
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,application/pdf,.png,image/png,.jpg,.jpeg,image/jpeg';
    input.style.display = 'none';
    input.onchange = async () => {
        try {
            const file = input.files && input.files[0];
            if (!file) return;
            if (file.size === 0) { onError('El archivo seleccionado está vacío.'); return; }
            if (file.size > maxBytes) { onError('El archivo supera el límite de 25 MiB.'); return; }
            const name = String(file.name || 'Documento visual');
            const mime = String(file.type || '').toLowerCase();
            const lowerName = name.toLowerCase();
            const format = mime === 'application/pdf' || lowerName.endsWith('.pdf') ? 'PDF'
                : mime === 'image/png' || lowerName.endsWith('.png') ? 'PNG'
                : mime === 'image/jpeg' || lowerName.endsWith('.jpg') || lowerName.endsWith('.jpeg') ? 'JPEG'
                : null;
            if (!format) { onError('Formato no admitido; selecciona un PDF, PNG o JPEG.'); return; }
            const buffer = await file.arrayBuffer();
            const bytes = new Uint8Array(buffer);
            let binary = '';
            for (let offset = 0; offset < bytes.length; offset += 0x8000) {
                binary += String.fromCharCode(...bytes.subarray(offset, Math.min(offset + 0x8000, bytes.length)));
            }
            const encoded = btoa(binary);
            binary = '';
            onSelected(name, format, encoded);
        } catch (error) {
            onError('No se pudo leer el archivo seleccionado.');
        } finally { input.remove(); }
    };
    input.oncancel = () => input.remove();
    document.body.appendChild(input);
    input.click();
}""")

@Composable
@OptIn(ExperimentalEncodingApi::class)
actual fun VisualDocumentPickerButton(
    enabled: Boolean,
    onVisualDocumentSelected: (name: String, format: VisualDocumentFormat, bytes: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    OutlinedButton(
        onClick = {
            openVisualDocument(MAX_VISUAL_DOCUMENT_BYTES, { name, rawFormat, bytesBase64 ->
                val format = when (rawFormat) {
                    "PDF" -> VisualDocumentFormat.PDF
                    "PNG" -> VisualDocumentFormat.PNG
                    "JPEG" -> VisualDocumentFormat.JPEG
                    else -> {
                        onError("Formato no admitido; selecciona un PDF, PNG o JPEG.")
                        return@openVisualDocument
                    }
                }
                onVisualDocumentSelected(name, format, Base64.Default.decode(bytesBase64))
            }, onError)
        },
        enabled = enabled
    ) {
        Text("Seleccionar PDF, PNG o JPEG")
    }
}
