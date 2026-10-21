package com.finsim.ingestion

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import com.google.ai.edge.litertlm.MessageCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGemmaCancellationTest {
    @Test
    fun cancellationDuringDispatchWaitsForNativeStartAndTerminalCallbackBeforeCleanup() = runBlocking {
        val dispatchEntered = CountDownLatch(1)
        val allowDispatchReturn = CountDownLatch(1)
        val nativeCancellationRequested = CountDownLatch(1)
        val cancellationCallReturned = CountDownLatch(1)
        val conversationClosed = CountDownLatch(1)
        val callbackRef = AtomicReference<MessageCallback>()
        val nativeInferenceStarted = AtomicBoolean(false)
        val cancelSawStartedInference = AtomicBoolean(false)

        val analysis = launch(Dispatchers.IO) {
            try {
                cancellableNativeConversationCall(
                    cancelProcess = {
                        cancelSawStartedInference.set(nativeInferenceStarted.get())
                        nativeCancellationRequested.countDown()
                    }
                ) { callback ->
                    callbackRef.set(callback)
                    dispatchEntered.countDown()
                    check(allowDispatchReturn.await(5, TimeUnit.SECONDS)) {
                        "Test did not release the controlled native dispatch."
                    }
                    nativeInferenceStarted.set(true)
                }
            } finally {
                conversationClosed.countDown()
            }
        }

        try {
            assertTrue(dispatchEntered.await(2, TimeUnit.SECONDS))
            Thread {
                analysis.cancel()
                cancellationCallReturned.countDown()
            }.apply { isDaemon = true; start() }

            val cancellationWasLostBeforeNativeStart = nativeCancellationRequested.await(150, TimeUnit.MILLISECONDS)
            assertFalse(cancellationWasLostBeforeNativeStart)
            allowDispatchReturn.countDown()
            assertTrue(nativeCancellationRequested.await(2, TimeUnit.SECONDS))
            assertTrue(cancellationCallReturned.await(2, TimeUnit.SECONDS))
            assertTrue(cancelSawStartedInference.get())
            val conversationClosedBeforeTerminalCallback = conversationClosed.await(150, TimeUnit.MILLISECONDS)
            assertFalse(conversationClosedBeforeTerminalCallback)
            callbackRef.get().onError(CancellationException("controlled native cancellation"))
            analysis.cancelAndJoin()
            assertTrue(conversationClosed.await(2, TimeUnit.SECONDS))
        } finally {
            allowDispatchReturn.countDown()
            callbackRef.get()?.onError(CancellationException("test cleanup"))
            analysis.cancelAndJoin()
        }
    }
}
