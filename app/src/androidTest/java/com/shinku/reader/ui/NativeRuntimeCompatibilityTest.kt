package com.shinku.reader.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.decoder.ImageDecoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NativeRuntimeCompatibilityTest {
    @Before
    fun verifyRequestedRuntimePageSize() {
        InstrumentationRegistry.getArguments().getString("expectedPageSize")?.let {
            assertEquals("Actual native runtime page size", it.toLong(), Os.sysconf(OsConstants._SC_PAGESIZE))
        }
    }

    @Test
    fun bundledOcrRecognizesSyntheticTextLocally() {
        val bitmap = Bitmap.createBitmap(1000, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 90f
            typeface = Typeface.DEFAULT_BOLD
        }
        Canvas(bitmap).drawText("NATIVE TEST", 40f, 160f, paint)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
            val text = result.text.uppercase(Locale.ROOT)
            assertTrue("Bundled OCR reads both fixture words: $text", "NATIVE" in text && "TEST" in text)
        } finally {
            recognizer.close()
            bitmap.recycle()
        }
    }

    @Test
    fun decoderRetainsFullRegionAndSampledDecodingAcrossFormats() {
        val source = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(48, 96, 160)) }
        try {
            val webp = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
            for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.JPEG, webp)) {
                val encoded = ByteArrayOutputStream().use { output ->
                    assertTrue(source.compress(format, 100, output))
                    output.toByteArray()
                }
                val decoder = checkNotNull(ImageDecoder.newInstance(encoded.inputStream()))
                try {
                    assertEquals(64, decoder.width)
                    assertEquals(48, decoder.height)
                    val full = checkNotNull(decoder.decode())
                    assertEquals(64, full.width)
                    assertEquals(48, full.height)
                    full.recycle()
                    val region = checkNotNull(decoder.decode(Rect(16, 8, 48, 40), 2))
                    assertEquals(16, region.width)
                    assertEquals(16, region.height)
                    assertTrue(Color.blue(region.getPixel(0, 0)) > Color.red(region.getPixel(0, 0)))
                    region.recycle()
                } finally {
                    decoder.recycle()
                }
                assertTrue(decoder.isRecycled)
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun sentenceEncoderLoadsAndReturnsStableFiniteNormalizedEmbeddings() {
        val modelPath = InstrumentationRegistry.getArguments().getString("embeddingModelPath")
        assumeTrue("Provide the official sentence encoder fixture explicitly", modelPath != null)
        val model = File(checkNotNull(modelPath))
        assertTrue("Model fixture exists", model.isFile && model.length() > 0)
        val options = TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(model.absolutePath).build())
            .setL2Normalize(true)
            .build()
        val embedder = TextEmbedder.createFromOptions(InstrumentationRegistry.getInstrumentation().targetContext, options)
        try {
            val first = embedder.embed("A reader opens a book.").embeddingResult().embeddings().first().floatEmbedding()
            val second = embedder.embed("A reader opens a book.").embeddingResult().embeddings().first().floatEmbedding()
            assertTrue("Embedding has meaningful dimensions", first.size >= 64)
            assertEquals(first.size, second.size)
            assertTrue("All values are finite", first.all { it.isFinite() })
            assertEquals(1.0, first.sumOf { it.toDouble() * it }, 0.001)
            assertTrue("Repeated input is stable", first.indices.all { kotlin.math.abs(first[it] - second[it]) < 0.0001f })
        } finally {
            embedder.close()
        }
    }
}
