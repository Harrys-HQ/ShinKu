package com.shinku.reader.source

import android.content.Context
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.domain.source.repository.StubSourceRepository
import com.shinku.reader.extension.ExtensionManager
import eu.kanade.tachiyomi.source.AndroidSourceManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class StoredSourcesTest {
    @Test
    fun `stored source metadata is available and updates without another database lookup`() = runBlocking {
        val saved = StubSource(901, "en", "Stored fixture source")
        val sources = MutableStateFlow(listOf(saved))
        val repository = mockk<StubSourceRepository> {
            every { subscribeAll() } returns sources
        }
        val extensions = mockk<ExtensionManager> {
            every { isInitialized } returns MutableStateFlow(false)
        }
        val manager = AndroidSourceManager(mockk<Context>(), extensions, repository)
        try {
            withTimeout(5000) {
                while (manager.getStubSources() != listOf(saved)) delay(10)
            }
            assertEquals(saved, manager.getOrStub(saved.id))
            val renamed = saved.copy(name = "Updated stored source")
            sources.value = listOf(renamed)
            withTimeout(5000) {
                while (manager.getOrStub(saved.id) != renamed) delay(10)
            }
            coVerify(exactly = 0) { repository.getStubSource(any()) }
        } finally {
            val scope = AndroidSourceManager::class.java.getDeclaredField("scope").apply { isAccessible = true }
            (scope.get(manager) as CoroutineScope).cancel()
        }
    }

    @Test
    fun `equal stored metadata avoids false source changes while real changes remain distinct`() {
        val saved = StubSource(901, "en", "Stored fixture source")
        assertEquals(saved, StubSource(901, "en", "Stored fixture source"))
        assertEquals(1, setOf(saved, saved.copy()).size)
        assertNotEquals(saved, saved.copy(name = "Renamed source"))
        assertNotEquals(saved, saved.copy(lang = "ja"))
        assertNotEquals(saved, saved.copy(id = 902))
    }
}
