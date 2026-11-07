package com.shinku.reader.data.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DownloadRunStateTest {
    @Test
    fun `running follows engine start and natural completion`() = runTest {
        val state = DownloadRunState()
        val finish = CompletableDeferred<Unit>()
        assertFalse(state.isRunning.value)
        state.launch(backgroundScope) { finish.await() }
        assertTrue(state.isRunning.value)
        runCurrent()
        finish.complete(Unit)
        runCurrent()
        assertFalse(state.isRunning.value)
    }

    @Test
    fun `repeated starts do not duplicate active work`() = runTest {
        val state = DownloadRunState()
        var starts = 0
        repeat(3) {
            state.launch(backgroundScope) {
                starts++
                awaitCancellation()
            }
        }
        runCurrent()
        assertEquals(1, starts)
        state.cancel()
        assertFalse(state.isRunning.value)
    }

    @Test
    fun `old cancellation finishing cannot mark a replacement paused`() = runTest {
        val state = DownloadRunState()
        val oldCleanup = CompletableDeferred<Unit>()
        state.launch(backgroundScope) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { oldCleanup.await() }
            }
        }
        runCurrent()
        state.cancel()
        runCurrent()
        assertFalse(state.isRunning.value)
        state.launch(backgroundScope) { awaitCancellation() }
        runCurrent()
        oldCleanup.complete(Unit)
        runCurrent()
        assertTrue(state.isRunning.value)
        state.cancel()
    }

    @Test
    fun `cancelled owner never reports running or starts work`() = runTest {
        val state = DownloadRunState()
        val owner = Job().apply { cancel() }
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + owner)
        var started = false
        state.launch(scope) { started = true }
        runCurrent()
        assertFalse(started)
        assertFalse(state.isRunning.value)
    }
}
