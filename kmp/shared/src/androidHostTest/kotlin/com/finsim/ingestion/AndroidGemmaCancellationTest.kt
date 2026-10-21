package com.finsim.ingestion

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGemmaCancellationTest {
    @Test
    fun cancellationInterruptsNativeCallAndWaitsBeforeConversationCleanup() = runBlocking {
        val nativeCallStarted = CountDownLatch(1)
        val nativeCancellationRequested = CountDownLatch(1)
        val finishNativeCall = CountDownLatch(1)
        var conversationClosed = false

        val analysis = launch(Dispatchers.IO) {
            try {
                cancellableNativeConversationCall(
                    cancelProcess = { nativeCancellationRequested.countDown() }
                ) {
                    nativeCallStarted.countDown()
                    check(finishNativeCall.await(5, TimeUnit.SECONDS)) { "Test did not release the controlled native call." }
                    Unit
                }
            } finally {
                conversationClosed = true
            }
        }

        try {
            assertTrue(nativeCallStarted.await(2, TimeUnit.SECONDS))
            analysis.cancel()
            assertTrue(nativeCancellationRequested.await(2, TimeUnit.SECONDS))
            assertFalse(analysis.isCompleted)
            assertFalse(conversationClosed)
        } finally {
            finishNativeCall.countDown()
        }

        analysis.cancelAndJoin()
        assertTrue(conversationClosed)
    }
}
