package com.shinku.reader.data.cache

import android.content.Context
import android.graphics.Rect
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.ui.reader.model.TranslatedBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

@Serializable
data class SerializedRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun toAndroidRect(): Rect = Rect(left, top, right, bottom)

    companion object {
        fun fromAndroidRect(rect: Rect?): SerializedRect? {
            return rect?.let { SerializedRect(it.left, it.top, it.right, it.bottom) }
        }
    }
}

@Serializable
data class SerializedTranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: SerializedRect? = null,
    val bubbleRect: SerializedRect? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val bgColor: Int,
    val textColor: Int,
) {
    fun toTranslatedBlock(): TranslatedBlock = TranslatedBlock(
        originalText = originalText,
        translatedText = translatedText,
        boundingBox = boundingBox?.toAndroidRect(),
        bubbleRect = bubbleRect?.toAndroidRect(),
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        bgColor = bgColor,
        textColor = textColor,
    )

    companion object {
        fun from(block: TranslatedBlock): SerializedTranslatedBlock = SerializedTranslatedBlock(
            originalText = block.originalText,
            translatedText = block.translatedText,
            boundingBox = SerializedRect.fromAndroidRect(block.boundingBox),
            bubbleRect = SerializedRect.fromAndroidRect(block.bubbleRect),
            sourceWidth = block.sourceWidth,
            sourceHeight = block.sourceHeight,
            bgColor = block.bgColor,
            textColor = block.textColor,
        )
    }
}

@Serializable
data class ChapterTranslationCache(
    val chapterId: Long,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pages: Map<Int, List<SerializedTranslatedBlock>>,
)

object TranslationDiskCache {
    private val context: Context by lazy { Injekt.get() }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val cacheDir: File
        get() = File(context.cacheDir, "chapter_translations").apply {
            if (!exists()) mkdirs()
        }

    private fun getFile(chapterId: Long, targetLanguage: String): File {
        val sanitizedLang = targetLanguage.lowercase().filter { it.isLetterOrDigit() }
        return File(cacheDir, "${chapterId}_$sanitizedLang.json")
    }

    fun hasChapterTranslation(chapterId: Long, targetLanguage: String): Boolean {
        if (chapterId <= 0L) return false
        val file = getFile(chapterId, targetLanguage)
        return file.exists() && file.length() > 0
    }

    fun getChapter(chapterId: Long, targetLanguage: String): Map<Int, List<TranslatedBlock>>? {
        if (chapterId <= 0L) return null
        val file = getFile(chapterId, targetLanguage)
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            val parsed = json.decodeFromString<ChapterTranslationCache>(content)
            parsed.pages.mapValues { (_, blocks) ->
                blocks.map { it.toTranslatedBlock() }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) {
                "Failed to decode chapter translation for $chapterId ($targetLanguage)"
            }
            null
        }
    }

    fun getPage(chapterId: Long, pageIndex: Int, targetLanguage: String): List<TranslatedBlock>? {
        return getChapter(chapterId, targetLanguage)?.get(pageIndex)
    }

    fun saveChapter(chapterId: Long, targetLanguage: String, pages: Map<Int, List<TranslatedBlock>>) {
        if (chapterId <= 0L || pages.isEmpty()) return
        try {
            val file = getFile(chapterId, targetLanguage)
            val serializedPages = pages.mapValues { (_, blocks) ->
                blocks.map { SerializedTranslatedBlock.from(it) }
            }
            val model = ChapterTranslationCache(
                chapterId = chapterId,
                targetLanguage = targetLanguage,
                timestamp = System.currentTimeMillis(),
                pages = serializedPages,
            )
            val content = json.encodeToString(model)
            file.writeText(content)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to write chapter translation for $chapterId ($targetLanguage)"
            }
        }
    }

    fun putPage(chapterId: Long, pageIndex: Int, targetLanguage: String, blocks: List<TranslatedBlock>) {
        if (chapterId <= 0L) return
        try {
            val current = getChapter(chapterId, targetLanguage)?.toMutableMap() ?: mutableMapOf()
            current[pageIndex] = blocks
            saveChapter(chapterId, targetLanguage, current)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to update page $pageIndex translation for $chapterId"
            }
        }
    }

    fun clearChapter(chapterId: Long) {
        if (chapterId <= 0L) return
        try {
            val prefix = "${chapterId}_"
            cacheDir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) {
                "Failed to delete chapter translations for $chapterId"
            }
        }
    }

    fun clearAll() {
        try {
            cacheDir.deleteRecursively()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) {
                "Failed to clear translation cache directory"
            }
        }
    }

    fun getDiskUsageBytes(): Long {
        return try {
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (_: Exception) {
            0L
        }
    }
}
