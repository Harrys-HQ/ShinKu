package com.shinku.reader.data.coil

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import coil3.BitmapImage
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.shinku.reader.exh.source.ShinKuPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpscalingInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed()

        if (result is SuccessResult && Injekt.get<ShinKuPreferences>().aiUpscaling().get()) {
            val bitmap = (result.image as? BitmapImage)?.bitmap
            if (bitmap != null && (bitmap.width < 1500 || bitmap.height < 1500)) {
                val upscaledBitmap = upscale(bitmap)
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
            Bitmap.createBitmap(softwareBitmap, 0, 0, softwareBitmap.width, softwareBitmap.height, matrix, true) ?: bitmap
        } catch (e: Exception) {
            bitmap
        }
    }
}
