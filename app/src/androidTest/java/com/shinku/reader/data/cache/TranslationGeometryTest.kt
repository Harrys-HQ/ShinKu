package com.shinku.reader.data.cache

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shinku.reader.ui.reader.model.TranslatedBlock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android geometry constructors are not implemented by the local JVM Android stubs. */
@RunWith(AndroidJUnit4::class)
class TranslationGeometryTest {
    @Test
    fun serializedRectConvertsToAndroidAndBack() {
        val serialized = SerializedRect(10, 20, 150, 200)
        val decoded = Json.decodeFromString<SerializedRect>(Json.encodeToString(serialized))
        val rect = decoded.toAndroidRect()

        assertEquals(Rect(10, 20, 150, 200), rect)
        assertEquals(serialized, SerializedRect.fromAndroidRect(rect))
    }

    @Test
    fun translatedBlockPreservesGeometryAcrossSerialization() {
        val original = TranslatedBlock(
            originalText = "こんにちは",
            translatedText = "Hello",
            boundingBox = Rect(50, 60, 200, 180),
            bubbleRect = Rect(40, 50, 220, 190),
            sourceWidth = 1080,
            sourceHeight = 1920,
            bgColor = -1,
            textColor = -16777216,
        )
        val serialized = SerializedTranslatedBlock.from(original)
        val restored = Json.decodeFromString<SerializedTranslatedBlock>(Json.encodeToString(serialized))
            .toTranslatedBlock()

        assertEquals(original.boundingBox, restored.boundingBox)
        assertEquals(original.bubbleRect, restored.bubbleRect)
        assertEquals(original.originalText, restored.originalText)
        assertEquals(original.translatedText, restored.translatedText)
        assertEquals(original.sourceWidth, restored.sourceWidth)
        assertEquals(original.sourceHeight, restored.sourceHeight)
        assertEquals(original.bgColor, restored.bgColor)
        assertEquals(original.textColor, restored.textColor)
    }
}
