import com.shinku.reader.data.cache.ChapterTranslationCache
import com.shinku.reader.data.cache.SerializedRect
import com.shinku.reader.data.cache.SerializedTranslatedBlock
import com.shinku.reader.ui.reader.model.TranslatedBlock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TranslationCacheTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    @Test
    fun verifySerializedRectRoundtrip() {
        val rect = SerializedRect(left = 10, top = 20, right = 150, bottom = 200)
        val encoded = json.encodeToString(rect)
        val decoded = json.decodeFromString<SerializedRect>(encoded)

        assertEquals(10, decoded.left)
        assertEquals(20, decoded.top)
        assertEquals(150, decoded.right)
        assertEquals(200, decoded.bottom)

        val androidRect = decoded.toAndroidRect()
        assertEquals(10, androidRect.left)
        assertEquals(200, androidRect.bottom)
    }

    @Test
    fun verifySerializedTranslatedBlockConversion() {
        val originalBlock = TranslatedBlock(
            originalText = "こんにちは",
            translatedText = "Hello",
            boundingBox = android.graphics.Rect(50, 60, 200, 180),
            bubbleRect = android.graphics.Rect(40, 50, 220, 190),
            sourceWidth = 1080,
            sourceHeight = 1920,
            bgColor = -1, // Color.WHITE
            textColor = -16777216, // Color.BLACK
        )

        val serialized = SerializedTranslatedBlock.from(originalBlock)
        val encoded = json.encodeToString(serialized)
        val decoded = json.decodeFromString<SerializedTranslatedBlock>(encoded)
        val restoredBlock = decoded.toTranslatedBlock()

        assertEquals("こんにちは", restoredBlock.originalText)
        assertEquals("Hello", restoredBlock.translatedText)
        assertEquals(1080, restoredBlock.sourceWidth)
        assertEquals(1920, restoredBlock.sourceHeight)
        assertEquals(-1, restoredBlock.bgColor)
        assertEquals(-16777216, restoredBlock.textColor)

        assertNotNull(restoredBlock.boundingBox)
        assertEquals(50, restoredBlock.boundingBox?.left)
        assertEquals(180, restoredBlock.boundingBox?.bottom)

        assertNotNull(restoredBlock.bubbleRect)
        assertEquals(40, restoredBlock.bubbleRect?.left)
        assertEquals(190, restoredBlock.bubbleRect?.bottom)
    }

    @Test
    fun verifyChapterTranslationCachePersistence() {
        val blockPage1 = SerializedTranslatedBlock(
            originalText = "これはテストです",
            translatedText = "This is a test",
            boundingBox = SerializedRect(100, 150, 300, 250),
            sourceWidth = 1200,
            sourceHeight = 1800,
            bgColor = 0xFFFFFF,
            textColor = 0x000000,
        )

        val blockPage2 = SerializedTranslatedBlock(
            originalText = "さようなら",
            translatedText = "Goodbye",
            boundingBox = SerializedRect(50, 80, 250, 180),
            sourceWidth = 1200,
            sourceHeight = 1800,
            bgColor = 0xFFFFFF,
            textColor = 0x000000,
        )

        val chapterCache = ChapterTranslationCache(
            chapterId = 98765L,
            targetLanguage = "English",
            timestamp = 1700000000000L,
            pages = mapOf(
                0 to listOf(blockPage1),
                1 to listOf(blockPage2),
            ),
        )

        val jsonString = json.encodeToString(chapterCache)
        val parsed = json.decodeFromString<ChapterTranslationCache>(jsonString)

        assertEquals(98765L, parsed.chapterId)
        assertEquals("English", parsed.targetLanguage)
        assertEquals(2, parsed.pages.size)
        assertEquals("This is a test", parsed.pages[0]?.get(0)?.translatedText)
        assertEquals("Goodbye", parsed.pages[1]?.get(0)?.translatedText)
    }
}
