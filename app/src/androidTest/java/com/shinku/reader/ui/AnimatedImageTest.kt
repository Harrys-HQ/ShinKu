package com.shinku.reader.ui

import android.graphics.drawable.AnimatedImageDrawable
import androidx.test.platform.app.InstrumentationRegistry
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.shinku.reader.core.common.util.system.ImageUtil
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class AnimatedImageTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun avifSequenceDetectionPreservesSourceAndRejectsStills() {
        for (name in listOf("two-frames.avif", "still.avif")) {
            instrumentation.context.assets.open("animated-images/$name").source().buffer().use {
                val expected = name.startsWith("two-")
                assertEquals(expected, ImageUtil.isAnimatedAvif(it))
                assertFalse(ImageUtil.isAnimatedAndSupported(it))
                assertEquals(0, it.readByte().toInt())
            }
        }
        okio.Buffer().writeUtf8("broken").let { assertFalse(ImageUtil.isAnimatedAvif(it)) }
    }

    @Test
    fun applicationPipelineDecodesAvifGifAndWebp() {
        for (extension in listOf("avif", "gif", "webp")) {
            val file = File(context.cacheDir, "animation-test.$extension")
            try {
                instrumentation.context.assets.open("animated-images/two-frames.$extension").use { input ->
                    file.outputStream().use(input::copyTo)
                }
                assertDecodes(file, shouldAnimate = extension != "avif")
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun reportedAvifCoverDecodesCleanly() {
        val path = InstrumentationRegistry.getArguments().getString("coverPath")
        assumeTrue("Supply the privately downloaded issue30 fixture", path != null)
        assertDecodes(File(path!!), shouldAnimate = false)
    }

    private fun assertDecodes(file: File, shouldAnimate: Boolean) = runBlocking {
        val result = context.imageLoader.execute(ImageRequest.Builder(context).data(file).build())
        assertTrue("Image load must succeed: $result", result is SuccessResult)
        val wrapped = (result as SuccessResult).image.asDrawable(context.resources)
        val drawable = (wrapped as? coil3.size.ScaleDrawable)?.child ?: wrapped
        assertTrue(drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
        if (shouldAnimate) {
            assertTrue("Expected animation for ${file.name}: ${drawable.javaClass}", drawable is AnimatedImageDrawable)
            instrumentation.runOnMainSync {
                (drawable as AnimatedImageDrawable).start()
                assertTrue(drawable.isRunning)
                drawable.stop()
            }
        }
    }
}
