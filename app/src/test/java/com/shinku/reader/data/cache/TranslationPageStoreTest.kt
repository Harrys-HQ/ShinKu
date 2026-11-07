package com.shinku.reader.data.cache

import com.shinku.reader.ui.reader.model.TranslatedBlock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TranslationPageStoreTest {
    @TempDir
    lateinit var directory: File

    private fun blocks(text: String) = listOf(TranslatedBlock("original", text, null, bgColor = -1, textColor = 0))

    @Test
    fun `saved pages survive restart and invalidation removes only the selected language page`() {
        val store = TranslationPageStore(directory)
        store.putPage(1, 0, "en", blocks("English"))
        store.putPage(1, 0, "fr", blocks("French"))
        store.putPage(1, 1, "en", blocks("next"))
        assertEquals("English", TranslationPageStore(directory).getPage(1, 0, "en")?.single()?.translatedText)
        store.removePage(1, 0, "en")
        val restarted = TranslationPageStore(directory)
        assertNull(restarted.getPage(1, 0, "en"))
        assertEquals("French", restarted.getPage(1, 0, "fr")?.single()?.translatedText)
        assertEquals("next", restarted.getPage(1, 1, "en")?.single()?.translatedText)
    }

    @Test
    fun `overlapping page saves retain all results`() {
        val store = TranslationPageStore(directory)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val tasks = (0 until 80).map { page -> executor.submit { store.putPage(1, page, "en", blocks("page $page")) } }
            tasks.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        val restarted = TranslationPageStore(directory)
        assertEquals(80, restarted.getChapter(1, "en")?.size)
        (0 until 80).forEach { page ->
            assertEquals("page $page", restarted.getPage(1, page, "en")?.single()?.translatedText)
        }
    }

    @Test
    fun `legacy chapters migrate without reviving an invalidated page`() {
        val legacy = File(directory, "1_en.json")
        legacy.writeText(
            Json.encodeToString(
                ChapterTranslationCache(
                    1, "en",
                    pages = mapOf(
                        0 to blocks("old").map(SerializedTranslatedBlock::from),
                        1 to blocks("keep").map(SerializedTranslatedBlock::from),
                    ),
                ),
            ),
        )
        val store = TranslationPageStore(directory)
        store.removePage(1, 0, "en")
        assertFalse(legacy.exists())
        assertNull(TranslationPageStore(directory).getPage(1, 0, "en"))
        assertEquals("keep", TranslationPageStore(directory).getPage(1, 1, "en")?.single()?.translatedText)
    }

    @Test
    fun `corrupt page and interrupted temporary write do not hide healthy pages`() {
        val store = TranslationPageStore(directory)
        store.putPage(1, 0, "en", blocks("healthy"))
        File(directory, "1_en.json.pages/1.json").writeText("{broken")
        File(directory, "1_en.json.pages/0.json.tmp").writeText("{interrupted")
        val restarted = TranslationPageStore(directory)
        assertNull(restarted.getPage(1, 1, "en"))
        assertEquals("healthy", restarted.getPage(1, 0, "en")?.single()?.translatedText)
        assertEquals(setOf(0), restarted.getChapter(1, "en")?.keys)
    }

    @Test
    fun `saving a later page does not rewrite earlier pages`() {
        val store = TranslationPageStore(directory)
        store.putPage(1, 0, "en", blocks("first"))
        val first = File(directory, "1_en.json.pages/0.json")
        assertTrue(first.setLastModified(1_000_000))
        (1..150).forEach { store.putPage(1, it, "en", blocks("$it")) }
        assertEquals(1_000_000L, first.lastModified())
        assertEquals(151, TranslationPageStore(directory).getChapter(1, "en")?.size)
    }

    @Test
    fun `failed replacement preserves the previously published page`() {
        val store = TranslationPageStore(directory)
        store.putPage(1, 0, "en", blocks("published"))
        assertTrue(File(directory, "1_en.json.pages/0.json.tmp").mkdir())
        store.putPage(1, 0, "en", blocks("unpublished"))
        assertEquals("published", TranslationPageStore(directory).getPage(1, 0, "en")?.single()?.translatedText)
    }

    @Test
    fun `chapter clear preserves other chapters and removes all languages`() {
        val store = TranslationPageStore(directory)
        store.putPage(1, 0, "en", blocks("one"))
        store.putPage(1, 0, "fr", blocks("un"))
        store.putPage(10, 0, "en", blocks("ten"))
        store.clearChapter(1)
        assertFalse(store.hasChapterTranslation(1, "en"))
        assertFalse(store.hasChapterTranslation(1, "fr"))
        assertEquals("ten", store.getPage(10, 0, "en")?.single()?.translatedText)
    }
}
