package com.finsim.app.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun ComparisonFileActions(
    exportJson: () -> Result<String>,
    importJson: (String) -> Boolean,
    onMessage: (String) -> Unit
)
