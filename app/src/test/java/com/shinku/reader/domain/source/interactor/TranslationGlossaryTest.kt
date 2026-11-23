package com.shinku.reader.domain.source.interactor

import com.shinku.reader.data.cache.TranslationPageStore
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch.TranslationBlockInput
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch.TranslationBlockOutput
import com.shinku.reader.ui.reader.model.TranslatedBlock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TranslationGlossaryTest {
    @TempDir lateinit var directory: File

    private val glossary = TranslationGlossary(
        entries = listOf(
            TranslationGlossary.Entry("Hana", "Hanaa"),
            TranslationGlossary.Entry("Hana Kato", "Hanaa Katō"),
            TranslationGlossary.Entry("Unrelated", "Private draft"),
        ),
        preserveHonorifics = true,
    )

    @Test
    fun `on-device substitution preserves preferred names and honorifics across page blocks`() = runTest {
        val result = glossary.translateLocally(
            listOf(
                TranslationBlockInput(4, "Hana Kato-san greets Hanako."),
                TranslationBlockInput(7, "Hello, Hana!"),
            ),
        ) { parts ->
            assertFalse(parts.any { "Hana Kato" in it.text || "-san" in it.text })
            parts.map { TranslationBlockOutput(it.id, it.text.uppercase()) }
        }
        assertEquals(
            listOf(
                TranslationBlockOutput(4, "Hanaa Katō-san GREETS HANAKO."),
                TranslationBlockOutput(7, "HELLO, Hanaa!"),
            ),
            result,
        )
    }

    @Test
    fun `cloud instructions contain only matching terms and scrub sensitive text before preview`() {
        val edited = glossary.copy(
            entries = glossary.entries + TranslationGlossary.Entry("greets", "https://example.test/?token=secret"),
        )
        val instructions = edited.cloudInstructions(listOf(TranslationBlockInput(1, "Hana Kato greets the reader")))
        assertTrue(instructions.contains("Hanaa"))
        assertTrue(instructions.contains("Preserve honorific suffixes"))
        assertFalse(instructions.contains("Private draft"))
        assertFalse(instructions.contains("secret"))
    }

    @Test
    fun `automatic offline fallback uses the local glossary after a provider failure`() = runTest {
        val result = translateReaderContext(
            engine = "auto",
            cloudConfigured = { true },
            translateLocally = {
                glossary.translateLocally(listOf(TranslationBlockInput(1, "Hana-san"))) {
                    error("The glossary and honorific already cover the full block")
                }
            },
            translateRemotely = { throw java.io.IOException("Offline fixture") },
        )
        assertEquals(listOf(TranslationBlockOutput(1, "Hanaa-san")), result)
    }

    @Test
    fun `glossary and provider variants do not overwrite legacy or other saved translations`() {
        val local = translationCacheKey("Japanese", "English", "mlkit", glossary)
        val remote = translationCacheKey("Japanese", "English", "hostA|model1", glossary)
        val otherModel = translationCacheKey("Japanese", "English", "hostA|model2", glossary)
        val otherProvider = translationCacheKey("Japanese", "English", "hostB|model1", glossary)
        val otherLanguage = translationCacheKey("Korean", "English", "hostA|model1", glossary)
        val otherTarget = translationCacheKey("Japanese", "French", "hostA|model1", glossary)
        val edited = translationCacheKey("Japanese", "English", "hostA|model1", glossary.copy(preserveHonorifics = false))
        assertEquals(7, setOf(local, remote, otherModel, otherProvider, otherLanguage, otherTarget, edited).size)
        assertNotEquals(remote, translationCacheKey("Japanese", "English", "hostA|model1", glossary.copy(entries = emptyList())))
        val store = TranslationPageStore(directory)
        store.putPage(15, 0, "English", blocks("legacy"))
        store.putPage(15, 0, remote, blocks("cloud"))
        store.putPage(15, 0, local, blocks("local fallback"))
        val restarted = TranslationPageStore(directory)
        assertNull(restarted.getPage(15, 0, edited))
        assertNull(restarted.getPage(15, 0, otherModel))
        assertEquals("legacy", restarted.getPage(15, 0, "English")?.single()?.translatedText)
        assertEquals("cloud", restarted.getPage(15, 0, remote)?.single()?.translatedText)
        assertEquals("local fallback", restarted.getPage(15, 0, local)?.single()?.translatedText)
    }

    private fun blocks(value: String) = listOf(TranslatedBlock("raw", value, null, bgColor = -1, textColor = 0))
}
