package com.shinku.reader.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.ui.reader.model.ReaderPage
import com.shinku.reader.ui.reader.model.TranslatedBlock
import com.shinku.reader.ui.reader.viewer.TranslationOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationOverlayMappingTest {
    @Test fun smallBubblesDoNotDrawTextAcrossThePage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val image = ImageView(instrumentation.targetContext).apply {
                setImageDrawable(object : ColorDrawable(Color.GRAY) {
                    override fun getIntrinsicWidth() = 100
                    override fun getIntrinsicHeight() = 100
                })
                layout(0, 0, 100, 100)
            }
            val fixture = ReaderPage(0).apply {
                showTranslation = true
                translationBlocks = listOf(TranslatedBlock("Fixture", "Long translated dialogue ".repeat(20), Rect(40, 40, 50, 52), sourceWidth = 100, sourceHeight = 100, bgColor = Color.RED, textColor = Color.WHITE))
            }
            val overlay = TranslationOverlay(instrumentation.targetContext).apply {
                page = fixture
                pageView = image
                layout(0, 0, 100, 100)
            }
            val output = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            overlay.draw(Canvas(output))
            var outside = 0
            for (y in 0 until 100) {
                for (x in 0 until 100) {
                    if ((x !in 39..51 || y !in 39..53) && Color.alpha(output.getPixel(x, y)) > 0) outside++
                }
            }
            assertEquals("Text must stay within its bubble, allowing the antialiased border", 0, outside)
            output.recycle()
        }
    }

    @Test fun fittedImageUsesItsLetterboxAndOriginalOcrDimensions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for ((sourceScale, padding) in listOf(1 to 0, 2 to 0, 2 to 10)) {
                val image = ImageView(instrumentation.targetContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(padding, padding * 2, padding, padding * 2)
                    setImageDrawable(object : ColorDrawable(Color.GRAY) {
                        override fun getIntrinsicWidth() = 100
                        override fun getIntrinsicHeight() = 200
                    })
                    layout(0, 0, 200, 200)
                }
                val fixture = ReaderPage(0).apply {
                    showTranslation = true
                    translationBlocks = listOf(TranslatedBlock("Fixture", "Hi", Rect(10 * sourceScale, 20 * sourceScale, 40 * sourceScale, 60 * sourceScale), sourceWidth = 100 * sourceScale, sourceHeight = 200 * sourceScale, bgColor = Color.RED, textColor = Color.WHITE))
                }
                val overlay = TranslationOverlay(instrumentation.targetContext).apply {
                    page = fixture
                    pageView = image
                    layout(0, 0, 200, 200)
                }
                val output = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                overlay.draw(Canvas(output))
                assertTrue("Mask follows fitted image, including its 50px left inset", Color.alpha(output.getPixel(85, 40)) > 0)
                assertEquals("No mask in the letterbox", 0, Color.alpha(output.getPixel(45, 40)))
                fixture.showTranslation = false
                output.eraseColor(Color.TRANSPARENT)
                overlay.draw(Canvas(output))
                assertEquals(0, Color.alpha(output.getPixel(85, 40)))
                output.recycle()
            }
        }
    }
}
