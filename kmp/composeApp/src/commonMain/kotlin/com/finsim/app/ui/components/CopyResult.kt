package com.finsim.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.sp
import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.app.ui.formatMoney

internal fun clipboardMoney(value: String) = Decimal.parse(value).toFixed(2).replace('.', ',')

@Composable
internal expect fun rememberResultCopier(): (String, (Boolean) -> Unit) -> Unit

@Composable
internal fun CopyResult(label: String, value: String, clipboardValue: String = value, modifier: Modifier = Modifier) {
    val copy = rememberResultCopier()
    var feedback by remember(label, value) { mutableStateOf("") }
    var copying by remember { mutableStateOf(false) }
    Column(modifier) {
        TextButton(enabled = !copying, onClick = {
            copying = true
            copy(clipboardValue) { success ->
                copying = false
                feedback = if (success) "Copiado" else "No se pudo copiar. Inténtalo de nuevo."
            }
        }, modifier = Modifier.semantics { contentDescription = "Copiar $label" }) {
            Text("$value · Copiar", fontSize = 12.sp)
        }
        if (feedback.isNotEmpty()) Text(feedback, fontSize = 11.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
internal fun CopyMoney(label: String, amount: String, currency: Currency, modifier: Modifier = Modifier) =
    CopyResult(label, formatMoney(amount, currency), clipboardMoney(amount), modifier)
