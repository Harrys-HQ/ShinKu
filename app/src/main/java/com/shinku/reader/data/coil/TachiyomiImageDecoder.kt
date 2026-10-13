package com.shinku.reader.data.coil

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import com.hippo.unifile.UniFile
import com.shinku.reader.util.storage.CbzCrypto
import com.shinku.reader.util.storage.CbzCrypto.getCoverStream
import com.shinku.reader.core.common.archive.archiveReader
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.BufferedSource
import com.shinku.reader.core.common.util.system.ImageUtil
import tachiyomi.decoder.ImageDecoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.BufferedInputStream
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A [Decoder] that uses built-in [ImageDecoder] to decode images that is not supported by the system.
 */
class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) : Decoder {
    private val context = Injekt.get<Application>()

    /**
     * A page cut into immutable strips of about [STRIP_BYTES] each. The renderer uploads an
     * immutable bitmap only when it is drawn, so only the strips on screen reach the GPU instead of
     * the whole page as one texture.
     */
    class StripsImage(val strips: List<Bitmap>) : Image {
        override val size: Long = strips.sumOf { it.allocationByteCount.toLong() }
        override val width: Int = strips.firstOrNull()?.width ?: 0
        override val height: Int = strips.sumOf { it.height }
        override val shareable: Boolean get() = false
        override fun draw(canvas: Canvas) {}
    }

    override suspend fun decode(): DecodeResult {
        if (options.webtoonStrips) return decodeStrips()

        // SY -->
        var coverStream: BufferedInputStream? = null
        if (resources.sourceOrNull()?.peek()?.use { CbzCrypto.detectCoverImageArchive(it.inputStream()) } == true) {
            if (resources.source().peek().use { ImageUtil.findImageType(it.inputStream()) == null }) {
                coverStream = UniFile.fromFile(resources.file().toFile())
                    ?.archiveReader(context = context)
                    ?.getCoverStream()
            }
        }
        val decoder = resources.sourceOrNull()?.use {
            coverStream.use { coverStream ->
                ImageDecoder.newInstance(coverStream ?: it.inputStream(), options.cropBorders, displayProfile)
            }
        }
        // SY <--

        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder" }

        val srcWidth = decoder.width
        val srcHeight = decoder.height

        val dstWidth = options.size.widthPx(options.scale) { srcWidth }
        val dstHeight = options.size.heightPx(options.scale) { srcHeight }

        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        var bitmap = decoder.decode(sampleSize = sampleSize)
        decoder.recycle()

        check(bitmap != null) { "Failed to decode image" }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            options.bitmapConfig == Bitmap.Config.HARDWARE &&
            ImageUtil.canUseHardwareBitmap(bitmap)
        ) {
            val hwBitmap = bitmap.copy(Bitmap.Config.HARDWARE, false)
            if (hwBitmap != null) {
                bitmap.recycle()
                bitmap = hwBitmap
            }
        }

        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = sampleSize > 1,
        )
    }

    private suspend fun decodeStrips(): DecodeResult = stripDecodes.withPermit {
        val peekStream = resources.source().peek().inputStream()
        val regionDecoder = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(peekStream)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(peekStream, false)
            }
        } catch (_: Throwable) {
            null
        }

        if (regionDecoder != null && regionDecoder.width > 0 && regionDecoder.height > 0) {
            try {
                val srcWidth = regionDecoder.width
                val srcHeight = regionDecoder.height
                val viewWidth = options.size.widthPx(options.scale) { srcWidth }
                val sampleSize = DecodeUtils.calculateInSampleSize(
                    srcWidth = srcWidth,
                    srcHeight = srcHeight,
                    dstWidth = viewWidth,
                    dstHeight = options.size.heightPx(options.scale) { srcHeight },
                    scale = options.scale,
                )
                val budgetWidth = sqrt(MAX_PAGE_BYTES.toDouble() / 4 * srcWidth / srcHeight).toInt()
                val targetWidth = maxOf(viewWidth, min(srcWidth / sampleSize, budgetWidth))
                    .coerceAtMost(MAX_STRIP_WIDTH)
                    .coerceIn(1, srcWidth)

                val stripRows = (STRIP_BYTES / (targetWidth * 4)).coerceAtLeast(1)
                val bandRows = (stripRows * sampleSize).coerceIn(1, srcHeight)

                val strips = mutableListOf<Bitmap>()
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                for (top in 0 until srcHeight step bandRows) {
                    val bottom = min(top + bandRows, srcHeight)
                    val rect = Rect(0, top, srcWidth, bottom)
                    val strip = regionDecoder.decodeRegion(rect, decodeOptions)
                    if (strip != null) {
                        strips.add(strip)
                    }
                }

                if (strips.isNotEmpty()) {
                    return@withPermit DecodeResult(
                        image = StripsImage(strips),
                        isSampled = sampleSize > 1,
                    )
                }
            } finally {
                regionDecoder.recycle()
            }
        }

        // Fallback for formats not supported by BitmapRegionDecoder (e.g. AVIF, JXL)
        var coverStream: BufferedInputStream? = null
        if (resources.sourceOrNull()?.peek()?.use { CbzCrypto.detectCoverImageArchive(it.inputStream()) } == true) {
            if (resources.source().peek().use { ImageUtil.findImageType(it.inputStream()) == null }) {
                coverStream = UniFile.fromFile(resources.file().toFile())
                    ?.archiveReader(context = context)
                    ?.getCoverStream()
            }
        }
        val decoder = resources.sourceOrNull()?.use {
            coverStream.use { coverStream ->
                ImageDecoder.newInstance(coverStream ?: it.inputStream(), options.cropBorders, displayProfile)
            }
        }
        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder" }

        val srcWidth = decoder.width
        val srcHeight = decoder.height
        val viewWidth = options.size.widthPx(options.scale) { srcWidth }
        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = viewWidth,
            dstHeight = options.size.heightPx(options.scale) { srcHeight },
            scale = options.scale,
        )

        val fullBitmap = decoder.decode(sampleSize = sampleSize)
        decoder.recycle()
        check(fullBitmap != null) { "Failed to decode image" }

        val stripRows = (STRIP_BYTES / (fullBitmap.width * 4)).coerceAtLeast(1)
        val strips = mutableListOf<Bitmap>()
        for (top in 0 until fullBitmap.height step stripRows) {
            val rows = min(stripRows, fullBitmap.height - top)
            val strip = Bitmap.createBitmap(fullBitmap, 0, top, fullBitmap.width, rows)
            strips.add(strip)
        }
        fullBitmap.recycle()

        DecodeResult(
            image = StripsImage(strips),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            return if (options.customDecoder || options.webtoonStrips || isApplicable(result.source.source())) {
                TachiyomiImageDecoder(result.source, options)
            } else {
                null
            }
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().buffered().use { stream ->
                ImageUtil.findImageType(stream)
            }
            // SY -->
            source.peek().inputStream().use { stream ->
                if (CbzCrypto.detectCoverImageArchive(stream)) return true
            }
            // SY <--
            return when (type) {
                ImageUtil.ImageType.AVIF, ImageUtil.ImageType.JXL -> true
                ImageUtil.ImageType.HEIF -> Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        var displayProfile: ByteArray? = null
    }
}

private const val STRIP_BYTES = 4 * 1024 * 1024
private const val MAX_PAGE_BYTES = 128 * 1024 * 1024
private const val MAX_STRIP_WIDTH = 4096
private val stripDecodes = Semaphore(1)
