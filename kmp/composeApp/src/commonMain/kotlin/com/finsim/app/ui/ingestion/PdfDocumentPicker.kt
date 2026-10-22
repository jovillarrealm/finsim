package com.finsim.app.ui.ingestion

import androidx.compose.runtime.Composable

@Composable
expect fun PdfDocumentPickerButton(
    enabled: Boolean,
    onPdfSelected: (name: String, bytes: ByteArray) -> Unit,
    onError: (String) -> Unit
)
