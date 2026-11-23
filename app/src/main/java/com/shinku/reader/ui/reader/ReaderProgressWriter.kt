package com.shinku.reader.ui.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ReaderProgressWriter(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val writeLock = Mutex()

    fun enqueue(write: suspend () -> Unit) {
        // Enter or queue for the lock in viewer callback order, before switching to IO.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                writeLock.withLock {
                    withContext(ioDispatcher) { write() }
                }
            }
        }
    }
}
