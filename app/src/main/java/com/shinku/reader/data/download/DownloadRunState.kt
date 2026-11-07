package com.shinku.reader.data.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tracks the engine job independently of the WorkManager job that scheduled it. */
internal class DownloadRunState {
    private val lock = Any()
    private var job: Job? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        synchronized(lock) {
            if (job?.isActive == true) return
            val next = scope.launch(start = CoroutineStart.LAZY, block = block)
            job = next
            next.invokeOnCompletion {
                synchronized(lock) {
                    // An old job can finish cancellation after a replacement has started.
                    if (job === next) {
                        job = null
                        _isRunning.value = false
                    }
                }
            }
            next.start()
            _isRunning.value = next.isActive
        }
    }

    fun cancel() {
        synchronized(lock) {
            val previous = job
            job = null
            _isRunning.value = false
            previous?.cancel()
        }
    }
}
