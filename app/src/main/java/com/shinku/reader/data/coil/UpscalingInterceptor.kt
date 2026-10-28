package com.shinku.reader.data.coil

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import coil3.BitmapImage
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaCover
import com.shinku.reader.domain.manga.model.PagePreview
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpscalingInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        // Fast paths: Skip if global preference is disabled or request is not marked for reader upscaling
        if (!Injekt.get<ShinKuPreferences>().aiUpscaling().get()) {
            return chain.proceed()
        }

        val request = chain.request
        if (!request.allowUpscaling) {
            return chain.proceed()
        }

        // Safeguard: Never upscale covers or previews
        val data = request.data
        if (data is MangaCover || data is Manga || data is PagePreview) {
            return chain.proceed()
        }

        val result = chain.proceed()

        if (result is SuccessResult) {
            val bitmap = (result.image as? BitmapImage)?.bitmap
            // Only upscale true low-resolution manga pages (between 100px and 1500px on both dimensions)
            if (bitmap != null &&
                bitmap.width in 100..<1500 &&
                bitmap.height in 100..<1500 &&
                maxOf(bitmap.width, bitmap.height) < 1800
            ) {
                val upscaledBitmap = withContext(Dispatchers.Default) {
                    upscale(bitmap)
                }
                return result.copy(image = upscaledBitmap.asImage())
            }
        }

        return result
    }

    private fun upscale(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
        val scale = (2048f / maxDim.toFloat()).coerceIn(1.25f, 2.0f)
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        // Hardware bitmaps cannot be used with Matrix scaling on some devices/versions
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            Bitmap.Config.ARGB_8888
        } else {
            bitmap.config ?: Bitmap.Config.ARGB_8888
        }

        val softwareBitmap = if (config != bitmap.config) {
            bitmap.copy(config, false)
        } else {
            bitmap
        } ?: return bitmap

        return try {
            val scaled = Bitmap.createBitmap(
                softwareBitmap,
                0,
                0,
                softwareBitmap.width,
                softwareBitmap.height,
                matrix,
                true,
            )
            val sharpened = applyUnsharpMask(scaled)
            if (scaled != softwareBitmap && scaled != sharpened) {
                scaled.recycle()
            }
            if (softwareBitmap != bitmap && softwareBitmap != sharpened) {
                softwareBitmap.recycle()
            }
            sharpened
        } catch (_: Throwable) {
            bitmap
        }
    }

    /**
     * Memory-safe 3x3 Unsharp Mask using a 3-row rolling scanline buffer (~32 KB total)
     * instead of allocating two full-frame IntArray(width * height) buffers (~62 MB).
     */
    private fun applyUnsharpMask(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        if (width < 3 || height < 3) return src

        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)

        var rowPrev = IntArray(width)
        var rowCurr = IntArray(width)
        var rowNext = IntArray(width)
        val outRow = IntArray(width)

        src.getPixels(rowPrev, 0, width, 0, 0, width, 1)
        src.getPixels(rowCurr, 0, width, 0, 1, width, 1)

        // Copy top border row unchanged
        dest.setPixels(rowPrev, 0, width, 0, 0, width, 1)

        for (y in 1 until height - 1) {
            src.getPixels(rowNext, 0, width, 0, y + 1, width, 1)

            // Preserve left and right border pixels
            outRow[0] = rowCurr[0]
            outRow[width - 1] = rowCurr[width - 1]

            for (x in 1 until width - 1) {
                val c00 = rowPrev[x]     // Top
                val c10 = rowCurr[x - 1] // Left
                val c11 = rowCurr[x]     // Center
                val c12 = rowCurr[x + 1] // Right
                val c22 = rowNext[x]     // Bottom

                val r = ((c11 shr 16 and 0xFF) * 5) -
                    (c00 shr 16 and 0xFF) -
                    (c10 shr 16 and 0xFF) -
                    (c12 shr 16 and 0xFF) -
                    (c22 shr 16 and 0xFF)
                val g = ((c11 shr 8 and 0xFF) * 5) -
                    (c00 shr 8 and 0xFF) -
                    (c10 shr 8 and 0xFF) -
                    (c12 shr 8 and 0xFF) -
                    (c22 shr 8 and 0xFF)
                val b = ((c11 and 0xFF) * 5) -
                    (c00 and 0xFF) -
                    (c10 and 0xFF) -
                    (c12 and 0xFF) -
                    (c22 and 0xFF)

                val nr = r.coerceIn(0, 255)
                val ng = g.coerceIn(0, 255)
                val nb = b.coerceIn(0, 255)
                val alpha = (c11 ushr 24) and 0xFF

                outRow[x] = (alpha shl 24) or (nr shl 16) or (ng shl 8) or nb
            }

            dest.setPixels(outRow, 0, width, 0, y, width, 1)

            // Rotate row buffers without allocating new arrays
            val tmp = rowPrev
            rowPrev = rowCurr
            rowCurr = rowNext
            rowNext = tmp
        }

        // Copy bottom border row unchanged
        dest.setPixels(rowCurr, 0, width, 0, height - 1, width, 1)
        return dest
    }
}
