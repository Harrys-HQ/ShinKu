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
        val matrix = Matrix()
        matrix.postScale(2f, 2f)

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
            val scaled = Bitmap.createBitmap(softwareBitmap, 0, 0, softwareBitmap.width, softwareBitmap.height, matrix, true)
            val sharpened = applyUnsharpMask(scaled)
            if (scaled != softwareBitmap && scaled != sharpened) {
                scaled.recycle()
            }
            if (softwareBitmap != bitmap && softwareBitmap != sharpened) {
                softwareBitmap.recycle()
            }
            sharpened
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun applyUnsharpMask(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        System.arraycopy(pixels, 0, outPixels, 0, width * height)
        
        // Optimized 3x3 Sharpening filter:
        // [ 0, -1,  0 ]
        // [-1,  5, -1 ]
        // [ 0, -1,  0 ]
        for (y in 1 until height - 1) {
            val offset = y * width
            for (x in 1 until width - 1) {
                val idx = offset + x
                
                val c00 = pixels[idx - width] // Top
                val c10 = pixels[idx - 1]     // Left
                val c11 = pixels[idx]         // Center
                val c12 = pixels[idx + 1]     // Right
                val c22 = pixels[idx + width] // Bottom
                
                val r00 = (c00 shr 16) and 0xFF
                val g00 = (c00 shr 8) and 0xFF
                val b00 = c00 and 0xFF
                
                val r10 = (c10 shr 16) and 0xFF
                val g10 = (c10 shr 8) and 0xFF
                val b10 = c10 and 0xFF
                
                val r11 = (c11 shr 16) and 0xFF
                val g11 = (c11 shr 8) and 0xFF
                val b11 = c11 and 0xFF
                
                val r12 = (c12 shr 16) and 0xFF
                val g12 = (c12 shr 8) and 0xFF
                val b12 = c12 and 0xFF
                
                val r22 = (c22 shr 16) and 0xFF
                val g22 = (c22 shr 8) and 0xFF
                val b22 = c22 and 0xFF
                
                val r = (r11 * 5) - r00 - r10 - r12 - r22
                val g = (g11 * 5) - g00 - g10 - g12 - g22
                val b = (b11 * 5) - b00 - b10 - b12 - b22
                
                val nr = r.coerceIn(0, 255)
                val ng = g.coerceIn(0, 255)
                val nb = b.coerceIn(0, 255)
                val alpha = (c11 shr 24) and 0xFF
                
                outPixels[idx] = (alpha shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        
        dest.setPixels(outPixels, 0, width, 0, 0, width, height)
        return dest
    }
}
