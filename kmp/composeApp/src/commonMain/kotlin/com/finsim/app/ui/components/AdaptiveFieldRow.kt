package com.finsim.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Places two form controls in one column on compact screens and a row on wide screens. */
@Composable
fun AdaptiveFieldRow(
    isCompact: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically
) {
    if (isCompact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = verticalAlignment
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
fun AdaptiveFieldRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        AdaptiveFieldRow(
            isCompact = maxWidth < 600.dp,
            first = first,
            second = second,
            spacing = spacing,
            verticalAlignment = verticalAlignment
        )
    }
}
