package com.shinku.reader.data.coil

import android.graphics.Bitmap
import android.graphics.Matrix
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
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
