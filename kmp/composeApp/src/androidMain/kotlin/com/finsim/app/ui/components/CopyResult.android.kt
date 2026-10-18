package com.finsim.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberResultCopier(): (String, (Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) { { text, complete ->
        val success = runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Resultado Finsim", text))
            check(clipboard.hasPrimaryClip())
        }.isSuccess
        complete(success)
    } }
}
