package com.shinku.reader.ui.reader

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReaderProgressTest {
    @Test
    fun `recreated reader restores the saved chapter and page before applying the launch intent`() {
        val savedState = SavedStateHandle(mapOf<String, Any>("chapter_id" to 12L, "page_index" to 7))
        val restored = ReaderPosition(savedState)

        assertEquals(12L, restored.chapterId)
        assertEquals(7, restored.initialPage(intentChapterId = 11L, intentPage = 3))
    }

    @Test
    fun `a new chapter starts at its own requested page and does not reuse the previous page`() {
        val savedState = SavedStateHandle()
        val position = ReaderPosition(savedState)
        assertEquals(3, position.initialPage(intentChapterId = 11L, intentPage = 3))

        position.save(chapterId = 11L, pageIndex = 7)
        position.save(chapterId = 12L, pageIndex = null)

        assertNull(position.initialPage(intentChapterId = 11L, intentPage = 3))
        assertEquals(4, position.initialPage(intentChapterId = 12L, intentPage = 4))

        position.save(chapterId = 12L, pageIndex = 0)
        val restored = ReaderPosition(savedState)
        assertEquals(12L, restored.chapterId)
        assertEquals(0, restored.initialPage(intentChapterId = 11L, intentPage = 3))
    }

    @Test
    fun `rapid turns persist in viewer order even when the reader closes before writes finish`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val readerJob = Job(coroutineContext[Job])
        val writer = ReaderProgressWriter(CoroutineScope(dispatcher + readerJob), dispatcher)
        val firstWriteStarted = CompletableDeferred<Unit>()
        val finishFirstWrite = CompletableDeferred<Unit>()
        val persistedPages = mutableListOf<Int>()
        writer.enqueue {
            firstWriteStarted.complete(Unit)
            finishFirstWrite.await()
            persistedPages.add(8)
        }
        firstWriteStarted.await()

        writer.enqueue { persistedPages.add(9) }
        writer.enqueue { persistedPages.add(6) }
        readerJob.cancel()
        assertEquals(emptyList<Int>(), persistedPages)

        finishFirstWrite.complete(Unit)
        readerJob.join()
        assertEquals(listOf(8, 9, 6), persistedPages)
    }
}
