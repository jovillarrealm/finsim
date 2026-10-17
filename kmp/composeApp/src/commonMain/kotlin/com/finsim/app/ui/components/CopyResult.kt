package com.finsim.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.app.ui.formatMoney
import kotlinx.coroutines.delay

internal fun clipboardMoney(value: String) = Decimal.parse(value).toFixed(2).replace('.', ',')

@Composable
internal expect fun rememberResultCopier(): (String, (Boolean) -> Unit) -> Unit

@Composable
internal fun CopyResult(label: String, value: String, clipboardValue: String = value, modifier: Modifier = Modifier) {
    val copy = rememberResultCopier()
    var feedback by remember(label, value) { mutableStateOf("") }
    var copying by remember { mutableStateOf(false) }
    var copiedSuccess by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(copiedSuccess) {
        if (copiedSuccess) {
            delay(1500)
            copiedSuccess = false
        }
    }

    Column(modifier) {
        TextButton(
            enabled = !copying,
            onClick = {
                copying = true
                copy(clipboardValue) { success ->
                    copying = false
                    if (success) {
                        copiedSuccess = true
                        feedback = "Copiado"
                    } else {
                        copiedSuccess = false
                        feedback = "No se pudo copiar. Inténtalo de nuevo."
                    }
                }
            },
            interactionSource = interactionSource,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (isHovered || copiedSuccess) BrandMintLight else androidx.compose.ui.graphics.Color.Transparent,
                contentColor = TextMain
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            modifier = Modifier.semantics {
                contentDescription = "Copiar $label"
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (copiedSuccess) BrandEmerald else if (isHovered) BrandForest else TextMain
                )
                if (copiedSuccess) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandEmerald
                    )
                } else if (isHovered) {
                    Canvas(Modifier.size(11.dp)) {
                        val strokeW = 1.2f.dp.toPx()
                        val r = 1.5f.dp.toPx()
                        // Back rect
                        drawRoundRect(
                            color = BrandEmerald,
                            topLeft = Offset(3.dp.toPx(), 0f),
                            size = Size(7.dp.toPx(), 8.dp.toPx()),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = strokeW)
                        )
                        // Front rect
                        drawRoundRect(
                            color = BrandEmerald,
                            topLeft = Offset(0f, 3.dp.toPx()),
                            size = Size(7.dp.toPx(), 8.dp.toPx()),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = strokeW)
                        )
                    }
                }
            }
        }
        if (feedback.isNotEmpty() && !copiedSuccess) {
            Text(
                feedback,
                fontSize = 11.sp,
                color = BrandAmber,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
internal fun CopyMoney(label: String, amount: String, currency: Currency, modifier: Modifier = Modifier, signed: Boolean = false) {
    val sign = if (signed && Decimal.parse(amount).isPositive) "+" else ""
    CopyResult(label, sign + formatMoney(amount, currency), sign + clipboardMoney(amount), modifier)
}
