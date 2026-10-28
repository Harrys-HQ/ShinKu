package com.shinku.reader.domain.source.interactor

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Collections

class TextRecognitionInteractor {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    private val recognitionCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<TextRecognitionResult>>(40, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<TextRecognitionResult>>?): Boolean = size > 40
        },
    )

    suspend fun recognizeText(
        bitmap: Bitmap,
        sourceLanguage: String = "Auto-Detect",
    ): List<TextRecognitionResult> = coroutineScope {
        val baseKey = if (bitmap.generationId != 0) {
            bitmap.generationId
        } else {
            bitmap.width * 31 + bitmap.height + bitmap.byteCount
        }
        val normalizedLang = sourceLanguage.trim().lowercase()
        val cacheKey = "$baseKey:$normalizedLang"
        recognitionCache[cacheKey]?.let { return@coroutineScope it }

        val image = InputImage.fromBitmap(bitmap, 0)

        // 1. Script-Aware OCR Execution: avoids 4x NPU/CPU overhead and cross-script line-art hallucinations
        val rawBlocks: List<Text.TextBlock> = when (normalizedLang) {
            "japanese", "ja" -> {
                runCatching { japaneseRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList())
            }
            "korean", "ko" -> {
                runCatching { koreanRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList())
            }
            "chinese", "zh" -> {
                runCatching { chineseRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList())
            }
            "english", "en", "spanish", "es", "french", "fr", "german", "de", "italian", "it", "portuguese", "pt", "indonesian", "id", "tagalog", "tl" -> {
                runCatching { latinRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList())
            }
            else -> {
                // Auto-Detect: Run CJK recognizers in parallel first
                val jaJob = async { runCatching { japaneseRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList()) }
                val koJob = async { runCatching { koreanRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList()) }
                val zhJob = async { runCatching { chineseRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList()) }

                val cjkBlocks = jaJob.await() + koJob.await() + zhJob.await()
                val hasRealCjk = cjkBlocks.any { containsCjkCharacters(it.text) }

                if (hasRealCjk) {
                    // Keep blocks that contain CJK or at least a 3+ letter Latin word (suppresses speed-line '||' / '11' noise)
                    cjkBlocks.filter { containsCjkCharacters(it.text) || hasValidLatinWord(it.text) }
                } else {
                    val latinBlocks = runCatching { latinRecognizer.process(image).await().textBlocks }.getOrDefault(emptyList())
                    cjkBlocks + latinBlocks
                }
            }
        }

        // 2. Filter out non-speech-bubble noise (screentone lines, tiny specks, pure numbers/punctuation, page borders)
        val filteredBlocks = rawBlocks.filter { block ->
            val box = block.boundingBox ?: return@filter false
            isLikelyValidMangaText(block.text, box, bitmap.width, bitmap.height)
        }

        // 3. Deduplicate overlapping blocks across recognizers, preferring higher CJK/letter density
        val uniqueBlocks = mutableListOf<Text.TextBlock>()
        for (block in filteredBlocks) {
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

                        val minArea = minOf(existingArea, newArea)
                        val overlapRatio = if (minArea > 0) intersectionArea.toFloat() / minArea.toFloat() else 0f
                        if (overlapRatio > 0.35f) {
                            isDuplicate = true
                            if (textQualityScore(block.text) > textQualityScore(existing.text)) {
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

            if (duplicateIndexToReplace != -1) {
                uniqueBlocks[duplicateIndexToReplace] = block
            } else if (!isDuplicate) {
                uniqueBlocks.add(block)
            }
        }

        // 4. Proximity & layout-aware block merging (combines lines belonging to the same speech balloon)
        val mergedResults = mutableListOf<TextRecognitionResult>()
        val visited = BooleanArray(uniqueBlocks.size)

        for (i in uniqueBlocks.indices) {
            if (visited[i]) continue
            visited[i] = true
            val currentBlock = uniqueBlocks[i]
            val currentBox = currentBlock.boundingBox?.let { Rect(it) } ?: Rect(0, 0, 0, 0)
            val cluster = mutableListOf(i)

            var expanded = true
            while (expanded) {
                expanded = false
                for (j in uniqueBlocks.indices) {
                    if (visited[j]) continue
                    val targetBlock = uniqueBlocks[j]
                    val targetBox = targetBlock.boundingBox ?: continue

                    val horizontalDist = maxOf(0, maxOf(currentBox.left - targetBox.right, targetBox.left - currentBox.right))
                    val verticalDist = maxOf(0, maxOf(currentBox.top - targetBox.bottom, targetBox.top - currentBox.bottom))

                    val isVerticalBubble = currentBox.height() > currentBox.width() * 1.15f ||
                        targetBox.height() > targetBox.width() * 1.15f

                    val charScale = if (isVerticalBubble) {
                        maxOf(currentBox.width(), targetBox.width()).coerceAtLeast(18)
                    } else {
                        maxOf(currentBox.height(), targetBox.height()).coerceAtLeast(18)
                    }

                    val maxHorizThreshold = if (isVerticalBubble) {
                        (charScale * 1.75f).toInt().coerceIn(24, 130)
                    } else {
                        (charScale * 1.15f).toInt().coerceIn(20, 90)
                    }
                    val maxVertThreshold = if (isVerticalBubble) {
                        (charScale * 1.0f).toInt().coerceIn(18, 80)
                    } else {
                        (charScale * 1.65f).toInt().coerceIn(24, 120)
                    }

                    if (horizontalDist <= maxHorizThreshold && verticalDist <= maxVertThreshold) {
                        visited[j] = true
                        cluster.add(j)
                        currentBox.set(
                            minOf(currentBox.left, targetBox.left),
                            minOf(currentBox.top, targetBox.top),
                            maxOf(currentBox.right, targetBox.right),
                            maxOf(currentBox.bottom, targetBox.bottom),
                        )
                        expanded = true
                    }
                }
            }

            // Order cluster lines by reading direction (Right-to-Left for vertical Japanese tategaki, Top-to-Bottom for horizontal)
            val isVerticalLayout = currentBox.height() > currentBox.width() * 1.15f &&
                cluster.any { containsCjkCharacters(uniqueBlocks[it].text) }

            val sortedClusterIndices = cluster.sortedWith { idx1, idx2 ->
                val box1 = uniqueBlocks[idx1].boundingBox ?: Rect()
                val box2 = uniqueBlocks[idx2].boundingBox ?: Rect()
                if (isVerticalLayout) {
                    val xDiff = box2.centerX() - box1.centerX()
                    if (kotlin.math.abs(xDiff) > 12) xDiff.compareTo(0) else box1.top.compareTo(box2.top)
                } else {
                    val yDiff = box1.centerY() - box2.centerY()
                    if (kotlin.math.abs(yDiff) > 12) yDiff.compareTo(0) else box1.left.compareTo(box2.left)
                }
            }

            val mergedText = sortedClusterIndices
                .map { cleanOcrBlockText(uniqueBlocks[it].text) }
                .filter { it.isNotEmpty() }
                .joinToString(if (isVerticalLayout) "" else " ")
                .trim()

            if (mergedText.isNotEmpty() && isLikelyValidMangaText(mergedText, currentBox, bitmap.width, bitmap.height)) {
                mergedResults.add(
                    TextRecognitionResult(
                        text = mergedText,
                        boundingBox = currentBox,
                    ),
                )
            }
        }

        recognitionCache[cacheKey] = mergedResults
        mergedResults
    }

    private fun cleanOcrBlockText(raw: String): String {
        return raw
            .replace("\r\n", " ")
            .replace("\n", " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun containsCjkCharacters(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (
                code in 0x3040..0x30FF || // Hiragana & Katakana
                code in 0x4E00..0x9FFF || // CJK Unified Ideographs
                code in 0x3400..0x4DBF || // CJK Extension A
                code in 0xAC00..0xD7AF || // Hangul Syllables
                code in 0x1100..0x11FF || // Hangul Jamo
                code in 0x3130..0x318F    // Hangul Compatibility Jamo
            ) {
                return true
            }
        }
        return false
    }

    private fun hasValidLatinWord(text: String): Boolean {
        return Regex("""[A-Za-zÀ-ÿ]{3,}""").containsMatchIn(text)
    }

    private fun textQualityScore(text: String): Int {
        var score = 0
        for (ch in text) {
            val code = ch.code
            when {
                code in 0x3040..0x30FF || code in 0x4E00..0x9FFF || code in 0xAC00..0xD7AF -> score += 3
                ch.isLetter() -> score += 2
                ch.isDigit() -> score += 1
            }
        }
        return score
    }

    private fun isLikelyValidMangaText(
        text: String,
        box: Rect,
        bitmapWidth: Int,
        bitmapHeight: Int,
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        val w = box.width()
        val h = box.height()
        if (w < 14 || h < 14) return false

        // Reject thin panel border lines or watermarks spanning the entire edge
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            if (w > bitmapWidth * 0.92f && h < 22) return false
            if (h > bitmapHeight * 0.92f && w < 22) return false
        }

        // Reject common speed-line / hatching OCR hallucinations (e.g. "|||", "///", "111", "...")
        if (Regex("""^[|/\\1Il!.\-_~+=*:;,?'"`()\[\]{}\s0-9]+$""").matches(trimmed)) {
            return false
        }

        if (containsCjkCharacters(trimmed)) {
            return true
        }

        // For non-CJK text, require at least 2 letters so stray single-letter artifacts ('I', 'l', 'V') are ignored
        val letterCount = trimmed.count { it.isLetter() }
        return letterCount >= 2
    }

    data class TextRecognitionResult(
        val text: String,
        val boundingBox: Rect?,
    )
}
