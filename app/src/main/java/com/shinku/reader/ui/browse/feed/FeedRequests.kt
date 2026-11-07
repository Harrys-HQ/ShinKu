package com.shinku.reader.ui.browse.feed

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/** Holds permits across suspensions, including the entire network request. */
internal class FeedRequestLimiter(
    maxConcurrent: Int = 6,
    private val maxConcurrentPerSource: Int = 3,
    private val timeoutMillis: Long = 8_000L,
) {
    private val global = Semaphore(maxConcurrent)
    private val sources = ConcurrentHashMap<Long, Semaphore>()

    init {
        require(maxConcurrentPerSource > 0)
        require(timeoutMillis > 0)
    }

    suspend fun <T> request(sourceId: Long, block: suspend () -> T): T? {
        // Waiting for a busy source must not occupy the global network budget.
        return sources.getOrPut(sourceId) { Semaphore(maxConcurrentPerSource) }.withPermit {
            global.withPermit {
                // Queueing time does not consume the source's response timeout.
                withTimeoutOrNull(timeoutMillis) {
                    block().also { currentCoroutineContext().ensureActive() }
                }
            }
        }
    }
}

/** Owns one replaceable operation and all work launched as its children. Called on the UI thread. */
internal class LatestFeedRequest(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var job: Job? = null

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        job = scope.launch(dispatcher, start = CoroutineStart.LAZY, block = block).also { it.start() }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

/** Also checks inside CAS retries, so cancelled requests cannot publish late results or cleanup. */
internal suspend fun <T> MutableStateFlow<T>.updateIfActive(transform: (T) -> T) {
    val context = currentCoroutineContext()
    update {
        context.ensureActive()
        transform(it)
    }
}

internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/** Shared by Discover and its drill-down screens, including when both models remain alive. */
internal val sharedFeedRequests = FeedRequestLimiter()
