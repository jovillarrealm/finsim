package com.finsim.app.ui.ingestion

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun PdfDocumentPickerButton(
    enabled: Boolean,
    onPdfSelected: (name: String, bytes: ByteArray) -> Unit,
    onError: (String) -> Unit
) {
    OutlinedButton(onClick = {}, enabled = false) {
        Text("PDF visual: solo Android")
    }
}
