package com.finsim.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.finsim.core.FinsimLog
import com.finsim.persistence.ScenarioStorage
import kotlinx.browser.document
import kotlinx.browser.localStorage

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
}
