package com.shinku.reader.domain.source.interactor

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

class TextRecognitionInteractor {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(bitmap: Bitmap): List<TextRecognitionResult> = kotlinx.coroutines.coroutineScope {
        val image = InputImage.fromBitmap(bitmap, 0)

        val latinJob = async {
            try { latinRecognizer.process(image).await().textBlocks } catch (e: Exception) { emptyList() }
        }
        val japaneseJob = async {
            try { japaneseRecognizer.process(image).await().textBlocks } catch (e: Exception) { emptyList() }
        }
        val chineseJob = async {
            try { chineseRecognizer.process(image).await().textBlocks } catch (e: Exception) { emptyList() }
        }
        val koreanJob = async {
            try { koreanRecognizer.process(image).await().textBlocks } catch (e: Exception) { emptyList() }
        }

        val allBlocks = latinJob.await() + japaneseJob.await() + chineseJob.await() + koreanJob.await()

        val uniqueBlocks = mutableListOf<com.google.mlkit.vision.text.Text.TextBlock>()
        for (block in allBlocks) {
            var isDuplicate = false
            var duplicateIndexToReplace = -1
            
            for (idx in uniqueBlocks.indices) {
                val existing = uniqueBlocks[idx]
                val existingBox = existing.boundingBox
                val newBox = block.boundingBox
                if (existingBox != null && newBox != null) {
                    val intersectionLeft = maxOf(existingBox.left, newBox.left)
                    val intersectionTop = maxOf(existingBox.top, newBox.top)
                    val intersectionRight = minOf(existingBox.right, newBox.right)
                    val intersectionBottom = minOf(existingBox.bottom, newBox.bottom)

                    if (intersectionLeft < intersectionRight && intersectionTop < intersectionBottom) {
                        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
                        val existingArea = (existingBox.right - existingBox.left) * (existingBox.bottom - existingBox.top)
                        val newArea = (newBox.right - newBox.left) * (newBox.bottom - newBox.top)

                        // If boxes overlap by more than 50%
                        val overlapRatio = intersectionArea.toFloat() / minOf(existingArea, newArea).toFloat()
                        if (overlapRatio > 0.5f) {
                            isDuplicate = true
                            // Quality Check: Keep the one with the longer text (preserves full English sentence over single-character artifacts)
                            if (block.text.length > existing.text.length) {
                                duplicateIndexToReplace = idx
                            }
                            break
                        }
                    }
                } else if (existing.text == block.text) {
                    isDuplicate = true
                    break
                }
            }
            
            if (block.text.isNotBlank()) {
                if (duplicateIndexToReplace != -1) {
                    uniqueBlocks[duplicateIndexToReplace] = block
                } else if (!isDuplicate) {
                    uniqueBlocks.add(block)
                }
            }
        }

        // Proximity-based block merging (combines nearby lines belonging to the same bubble)
        val mergedResults = mutableListOf<TextRecognitionResult>()
        val visited = BooleanArray(uniqueBlocks.size)

        for (i in uniqueBlocks.indices) {
            if (visited[i]) continue
            visited[i] = true
            val currentBlock = uniqueBlocks[i]
            var currentText = currentBlock.text
            val currentBox = currentBlock.boundingBox?.let { android.graphics.Rect(it) } ?: android.graphics.Rect(0, 0, 0, 0)
            val cluster = mutableListOf(i)

            // Keep finding and merging overlapping/closely neighboring blocks in a loop
            var expanded = true
            while (expanded) {
                expanded = false
                for (j in uniqueBlocks.indices) {
                    if (visited[j]) continue
                    val targetBlock = uniqueBlocks[j]
                    val targetBox = targetBlock.boundingBox ?: continue

                    // Calculate horizontal and vertical distances between current cluster bounding box and target box
                    val horizontalDist = maxOf(0, maxOf(currentBox.left - targetBox.right, targetBox.left - currentBox.right))
                    val verticalDist = maxOf(0, maxOf(currentBox.top - targetBox.bottom, targetBox.top - currentBox.bottom))

                    // Determine max proximity threshold based on size of the blocks (roughly 1.5x character height/width)
                    val currentHeight = currentBox.height()
                    val targetHeight = targetBox.height()
                    val threshold = (maxOf(currentHeight, targetHeight) * 1.5).toInt().coerceIn(30, 150)

                    if (horizontalDist <= threshold && verticalDist <= threshold) {
                        visited[j] = true
                        cluster.add(j)
                        // Merge bounding boxes by creating a union
                        currentBox.set(
                            minOf(currentBox.left, targetBox.left),
                            minOf(currentBox.top, targetBox.top),
                            maxOf(currentBox.right, targetBox.right),
                            maxOf(currentBox.bottom, targetBox.bottom)
                        )
                        expanded = true
                    }
                }
            }

            // Order cluster items logically based on position (vertical vertical-reading or horizontal horizontal-reading)
            val sortedClusterIndices = cluster.sortedWith { idx1, idx2 ->
                val box1 = uniqueBlocks[idx1].boundingBox ?: android.graphics.Rect()
                val box2 = uniqueBlocks[idx2].boundingBox ?: android.graphics.Rect()
                // If it looks like vertical manga text layout (lines are side-by-side)
                if (currentBox.height() > currentBox.width()) {
                    // Right-to-left line order, then top-to-bottom
                    val xCompare = box2.left.compareTo(box1.left)
                    if (xCompare != 0) xCompare else box1.top.compareTo(box2.top)
                } else {
                    // Left-to-right line order, then top-to-bottom
                    val yCompare = box1.top.compareTo(box2.top)
                    if (yCompare != 0) yCompare else box1.left.compareTo(box2.left)
                }
            }

            val mergedText = sortedClusterIndices.map { uniqueBlocks[it].text.replace("\n", " ").trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")

            mergedResults.add(
                TextRecognitionResult(
                    text = mergedText,
                    boundingBox = currentBox
                )
            )
        }

        mergedResults
    }

    data class TextRecognitionResult(
        val text: String,
        val boundingBox: android.graphics.Rect?,
    )
}
