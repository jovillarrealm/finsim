package com.finsim.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.finsim.core.FinsimLog
import com.finsim.persistence.ScenarioStorage
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlin.js.js

// shortcut: Compose 1.9 loses browser focus when removing its input; remove after CMP-9388 is fixed and verified on our stack.
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun retainCanvasFocus(): Unit = js("""{
  function install() {
    const root = document.body.shadowRoot;
    if (!root) { requestAnimationFrame(install); return; }
    let focused;
    root.addEventListener('focusin', event => { focused = event.target; });
    new MutationObserver(() => {
        if (focused?.matches('input, textarea') && !focused.isConnected &&
            !root.activeElement && document.hasFocus()) {
            focused = null;
            root.querySelector('canvas')?.focus({preventScroll: true});
        }
    }).observe(root, {childList: true, subtree: true});
  }
  install();
}""")

class WasmScenarioStorage : ScenarioStorage {
    override fun getItem(key: String): String? = localStorage.getItem(key)
    override fun setItem(key: String, value: String) {
        localStorage.setItem(key, value)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    FinsimLog.ui.i { "Finsim Web (WasmJs) iniciada exitosamente" }
    val body = document.body ?: return
    val storage = WasmScenarioStorage()
    ComposeViewport(body) {
        App(storage = storage)
    }
    retainCanvasFocus()
}
