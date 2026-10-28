package com.finsim.app.ui.ingestion

import androidx.compose.runtime.Composable
import com.finsim.ingestion.VisualDocumentFormat

@Composable
expect fun VisualDocumentPickerButton(
    enabled: Boolean,
    onVisualDocumentSelected: (name: String, format: VisualDocumentFormat, bytes: ByteArray) -> Unit,
    onError: (String) -> Unit
)
