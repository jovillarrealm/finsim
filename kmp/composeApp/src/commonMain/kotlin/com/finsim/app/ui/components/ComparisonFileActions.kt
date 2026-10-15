package com.finsim.app.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun ComparisonFileActions(
    fileName: String = "comparacion-finsim.json",
    exportJson: () -> Result<String>,
    importJson: (String) -> Boolean,
    onMessage: (String) -> Unit
)
