package com.finsim.app.ui.components

import androidx.compose.runtime.Composable
import kotlin.js.js

private fun copyText(text: String, complete: (Boolean) -> Unit): Unit = js("""{
    try {
        navigator.clipboard.writeText(text).then(() => complete(true), () => complete(false));
    } catch (error) { complete(false); }
}""")

@Composable
internal actual fun rememberResultCopier(): (String, (Boolean) -> Unit) -> Unit = ::copyText
