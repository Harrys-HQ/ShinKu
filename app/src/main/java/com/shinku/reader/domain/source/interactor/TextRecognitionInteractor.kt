package com.shinku.reader.domain.source.interactor

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognitionInteractor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): List<TextRecognitionResult> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            result.textBlocks.map { block ->
                TextRecognitionResult(
                    text = block.text,
                    boundingBox = block.boundingBox,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class TextRecognitionResult(
        val text: String,
        val boundingBox: android.graphics.Rect?,
    )
}
