package com.lifemash.coroutines

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest {
    @Test
    fun nextEmissionWaitsForConsumerWithoutDroppingValues() = runTest {
        val releaseFirstValue = CompletableDeferred<Unit>()
        val firstEmissionReturned = CompletableDeferred<Unit>()
        val secondEmissionReturned = CompletableDeferred<Unit>()
        val received = mutableListOf<Int>()
        val collection = launch {
            flowOf(Unit).flatMapLatestUnbuffered {
                flow {
                    emit(1)
                    firstEmissionReturned.complete(Unit)
                    emit(2)
                    secondEmissionReturned.complete(Unit)
                    emit(3)
                }
            }.collect { value ->
                received += value
                if (value == 1) releaseFirstValue.await()
            }
        }

        runCurrent()
        assertEquals(listOf(1), received)
        assertTrue(firstEmissionReturned.isCompleted)
        assertFalse(secondEmissionReturned.isCompleted)

        releaseFirstValue.complete(Unit)
        collection.join()
        assertEquals(listOf(1, 2, 3), received)
        assertTrue(secondEmissionReturned.isCompleted)
    }

    @Test
    fun upstreamChangeCancelsPendingEmissionButNotCurrentConsumerWork() = runTest {
        val accounts = MutableStateFlow("A")
        val releaseFirstValue = CompletableDeferred<Unit>()
        val oldSubscriptionCancelled = CompletableDeferred<Unit>()
        val pendingEmissionStarted = CompletableDeferred<Unit>()
        val pendingEmissionReturned = CompletableDeferred<Unit>()
        val received = mutableListOf<String>()
        val completed = mutableListOf<String>()
        val collection = launch {
            accounts.flatMapLatestUnbuffered { account ->
                flow {
                    if (account == "A") {
                        try {
                            emit("A1")
                            pendingEmissionStarted.complete(Unit)
                            emit("A2")
                            pendingEmissionReturned.complete(Unit)
                        } finally {
                            oldSubscriptionCancelled.complete(Unit)
                        }
                    } else {
                        emit("B1")
                    }
                }
            }.collect { value ->
                received += value
                if (value == "A1") releaseFirstValue.await()
                completed += value
            }
        }

        runCurrent()
        assertTrue(pendingEmissionStarted.isCompleted)
        assertFalse(pendingEmissionReturned.isCompleted)
        accounts.value = "B"
        runCurrent()
        assertTrue(oldSubscriptionCancelled.isCompleted)
        assertEquals(listOf("A1"), received)
        assertTrue(completed.isEmpty())

        releaseFirstValue.complete(Unit)
        runCurrent()
        assertEquals(listOf("A1", "B1"), received)
        assertEquals(listOf("A1", "B1"), completed)
        assertFalse(pendingEmissionReturned.isCompleted)
        collection.cancelAndJoin()
    }

    @Test
    fun cancellingCollectorCancelsBothUpstreamAndInnerSubscription() = runTest {
        val upstreamCancelled = CompletableDeferred<Unit>()
        val innerCancelled = CompletableDeferred<Unit>()
        val received = mutableListOf<Int>()
        val collection = launch {
            flow {
                try {
                    emit(Unit)
                    awaitCancellation()
                } finally {
                    upstreamCancelled.complete(Unit)
                }
            }.flatMapLatestUnbuffered {
                flow {
                    try {
                        emit(1)
                        awaitCancellation()
                    } finally {
                        innerCancelled.complete(Unit)
                    }
                }
            }.collect { received += it }
        }
        runCurrent()
        assertEquals(listOf(1), received)
        collection.cancelAndJoin()
        assertTrue(upstreamCancelled.isCompleted)
        assertTrue(innerCancelled.isCompleted)
    }

    @Test
    fun innerFailureReachesCollector() = runTest {
        val failure = IOException("실패 전파 검증")
        try {
            flowOf(Unit).flatMapLatestUnbuffered {
                flow<Int> { throw failure }
            }.collect()
            fail("내부 Flow의 예외가 전달되어야 합니다.")
        } catch (actual: IOException) {
            assertEquals(failure.message, actual.message)
        }
    }
}
