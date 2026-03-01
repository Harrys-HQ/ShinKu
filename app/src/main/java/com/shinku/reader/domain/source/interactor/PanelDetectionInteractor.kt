package com.shinku.reader.domain.source.interactor

import android.graphics.Bitmap
import android.graphics.RectF
import com.shinku.reader.util.image.PanelDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PanelDetectionInteractor {

    private val detector = PanelDetector()

    suspend fun detectPanels(bitmap: Bitmap): List<RectF> = withContext(Dispatchers.Default) {
        try {
            detector.detect(bitmap)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
