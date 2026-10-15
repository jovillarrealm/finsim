package com.finsim.core

import co.touchlab.kermit.Logger

/**
 * Centralized loggers for Finsim components.
 *
 * Automatically routes to Android Logcat (`android.util.Log`) on Android
 * and browser console (`console.log`, `console.warn`, `console.error`) on Web Wasm/JS.
 */
object FinsimLog {
    val engine: Logger = Logger.withTag("FinsimEngine")
    val rates: Logger = Logger.withTag("FinsimRates")
    val storage: Logger = Logger.withTag("FinsimStorage")
    val reconcile: Logger = Logger.withTag("FinsimReconcile")
    val ui: Logger = Logger.withTag("FinsimUI")
}
