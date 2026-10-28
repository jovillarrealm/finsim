package com.finsim.ingestion

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProgressInactivityWatchdogTest {
    @Test
    fun progressRenewsTheDeadlineInsteadOfApplyingATotalRuntimeLimit() = runTest {
        var timedOut = false
        val watchdog = ProgressInactivityWatchdog(backgroundScope, timeoutMillis = 1_000) {
            timedOut = true
        }

        watchdog.progress()
        advanceTimeBy(950)
        watchdog.progress()
        advanceTimeBy(950)
        runCurrent()
        assertEquals(false, timedOut)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(true, timedOut)
        watchdog.cancel()
    }

    @Test
    fun cancellationClearsTheInactivityDeadline() = runTest {
        var timedOut = false
        val watchdog = ProgressInactivityWatchdog(backgroundScope, timeoutMillis = 1_000) {
            timedOut = true
        }

        watchdog.progress()
        advanceTimeBy(500)
        watchdog.cancel()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(false, timedOut)
    }
}
