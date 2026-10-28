package com.finsim.ingestion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Resets an inactivity deadline on worker progress; it does not impose a total run-time cap. */
internal class ProgressInactivityWatchdog(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
    private val onTimeout: () -> Unit
) {
    private var timeoutJob: Job? = null

    init {
        require(timeoutMillis > 0)
    }

    fun progress() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeoutMillis)
            onTimeout()
        }
    }

    fun cancel() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
