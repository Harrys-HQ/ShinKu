package com.shinku.reader.ui.browse.feed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeedRequestsTest {
    @Test
    fun `suspended requests respect global and per source limits`() = runTest {
        val limiter = FeedRequestLimiter()
        var active = 0
        var peak = 0
        val perSource = mutableMapOf<Long, Int>()
        val jobs = List(24) { index ->
            async {
                val sourceId = (index % 3).toLong()
                limiter.request(sourceId) {
                    active++
                    peak = maxOf(peak, active)
                    perSource[sourceId] = perSource.getOrDefault(sourceId, 0) + 1
                    assertTrue(active <= 6)
                    assertTrue(perSource.getValue(sourceId) <= 3)
                    delay(100)
                    active--
                    perSource[sourceId] = perSource.getValue(sourceId) - 1
                    index
                }
            }
        }

        assertEquals((0 until 24).toList(), jobs.awaitAll())
        assertEquals(6, peak)
        assertEquals(0, active)
    }

    @Test
    fun `busy source does not occupy other sources global permits`() = runTest {
        val limiter = FeedRequestLimiter()
        val release = CompletableDeferred<Unit>()
        val started = mutableListOf<Long>()
        val jobs = (List(8) { 1L } + listOf(2L, 3L, 4L)).map { source ->
            launch {
                limiter.request(source) {
                    started.add(source)
                    release.await()
                }
            }
        }

        runCurrent()
        assertEquals(listOf(1L, 1L, 1L, 2L, 3L, 4L), started)
        release.complete(Unit)
        jobs.forEach { it.join() }
    }

    @Test
    fun `queue time does not consume response timeout`() = runTest {
        val limiter = FeedRequestLimiter(maxConcurrent = 1, timeoutMillis = 100)
        val results = List(2) { index ->
            async {
                limiter.request(index.toLong()) {
                    delay(75)
                    index
                }
            }
        }.awaitAll()

        assertEquals(listOf(0, 1), results)
        assertEquals(150L, testScheduler.currentTime)
    }

    @Test
    fun `timeout releases permits for subsequent requests`() = runTest {
        val limiter = FeedRequestLimiter(maxConcurrent = 1, maxConcurrentPerSource = 1, timeoutMillis = 100)
        assertNull(limiter.request(1) { delay(200) })
        assertEquals("ready", limiter.request(1) { "ready" })
    }

    @Test
    fun `cancelling queued and active requests releases permits`() = runTest {
        val limiter = FeedRequestLimiter(maxConcurrent = 1, maxConcurrentPerSource = 1)
        val active = launch { limiter.request(1) { awaitCancellation() } }
        runCurrent()
        val queued = launch { limiter.request(2) { error("Cancelled queued request started") } }
        runCurrent()

        queued.cancelAndJoin()
        active.cancelAndJoin()
        assertEquals("ready", limiter.request(2) { "ready" })
        assertEquals("ready", limiter.request(1) { "ready" })
    }

    @Test
    fun `source failure releases permits and preserves the exception`() = runTest {
        val limiter = FeedRequestLimiter(maxConcurrent = 1, maxConcurrentPerSource = 1)
        val failure = IllegalStateException("Source unavailable")
        val result = runCatchingCancellable { limiter.request(1) { throw failure } }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(failure.message, result.exceptionOrNull()?.message)
        assertEquals("ready", limiter.request(1) { "ready" })
    }

    @Test
    fun `source swallowing cancellation cannot return a late result`() = runTest {
        val limiter = FeedRequestLimiter()
        var published = false
        val request = launch {
            limiter.request(1) {
                try {
                    awaitCancellation()
                } catch (_: CancellationException) {
                    "late"
                }
            }
            published = true
        }

        runCurrent()
        request.cancelAndJoin()
        assertFalse(published)
    }

    @Test
    fun `replacement cancels background children and rejects old cleanup`() = runTest {
        val request = LatestFeedRequest(this, StandardTestDispatcher(testScheduler))
        val state = MutableStateFlow("loading old")
        var childCancelled = false
        request.launch {
            launch {
                try {
                    awaitCancellation()
                } finally {
                    childCancelled = true
                }
            }
            try {
                awaitCancellation()
            } finally {
                state.updateIfActive { "old cleanup" }
            }
        }
        runCurrent()

        state.value = "loading new"
        request.launch {
            delay(100)
            state.updateIfActive { "new result" }
        }
        runCurrent()
        assertTrue(childCancelled)
        assertEquals("loading new", state.value)
        advanceUntilIdle()
        assertEquals("new result", state.value)
    }

    @Test
    fun `clearing a selection cancels its pending result`() = runTest {
        val request = LatestFeedRequest(this, StandardTestDispatcher(testScheduler))
        val state = MutableStateFlow<String?>("Action")
        request.launch {
            try {
                delay(100)
            } catch (_: CancellationException) {
                // Some extensions catch cancellation; publishing must still be rejected.
            }
            state.updateIfActive { "stale Action titles" }
        }
        runCurrent()

        request.cancel()
        state.value = null
        advanceTimeBy(200)
        runCurrent()
        assertNull(state.value)
    }

    @Test
    fun `fallbacks propagate cancellation`() {
        var cancelled = false
        try {
            runCatchingCancellable { throw CancellationException("replaced") }
                .getOrDefault("fallback")
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
