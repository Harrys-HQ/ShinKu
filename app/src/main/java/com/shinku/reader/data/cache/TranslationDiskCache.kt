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
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

internal class TranslationPageStore(private val directory: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val cacheDir: File
        get() = directory.apply {
            if (!exists()) mkdirs()
        }

    private fun getFile(chapterId: Long, targetLanguage: String): File {
        val sanitizedLang = targetLanguage.lowercase().filter { it.isLetterOrDigit() }
        return File(cacheDir, "${chapterId}_$sanitizedLang.json")
    }

    fun hasChapterTranslation(chapterId: Long, targetLanguage: String): Boolean {
        if (chapterId <= 0L) return false
        val file = getFile(chapterId, targetLanguage)
        return file.exists() || pageDirectory(chapterId, targetLanguage).listFiles()?.any { it.extension == "json" } == true
    }

    @Synchronized
    fun getChapter(chapterId: Long, targetLanguage: String): Map<Int, List<TranslatedBlock>>? {
        if (chapterId <= 0L) return null
        val file = getFile(chapterId, targetLanguage)
        val pageDir = pageDirectory(chapterId, targetLanguage)
        if (!file.exists() && !pageDir.exists()) return null
        return try {
            migrateLegacy(chapterId, targetLanguage)
            pageDir.listFiles().orEmpty().mapNotNull { page ->
                val index = page.nameWithoutExtension.toIntOrNull()
                if (page.extension != "json" || index == null) {
                    null
                } else {
                    readPage(page)?.let { index to it }
                }
            }.toMap().mapValues { (_, blocks) ->
                blocks.map { it.toTranslatedBlock() }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) {
                "Failed to decode chapter translation for $chapterId ($targetLanguage)"
            }
            null
        }
    }

    @Synchronized
    fun getPage(chapterId: Long, pageIndex: Int, targetLanguage: String): List<TranslatedBlock>? {
        if (chapterId <= 0L) return null
        return try {
            migrateLegacy(chapterId, targetLanguage)
            readPage(File(pageDirectory(chapterId, targetLanguage), "$pageIndex.json"))?.map { it.toTranslatedBlock() }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to read page translation" }
            null
        }
    }

    @Synchronized
    fun saveChapter(chapterId: Long, targetLanguage: String, pages: Map<Int, List<TranslatedBlock>>) {
        if (chapterId <= 0L || pages.isEmpty()) return
        try {
            migrateLegacy(chapterId, targetLanguage)
            pages.forEach { (index, blocks) -> putPage(chapterId, index, targetLanguage, blocks) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to write chapter translation for $chapterId ($targetLanguage)"
            }
        }
    }

    @Synchronized
    fun putPage(chapterId: Long, pageIndex: Int, targetLanguage: String, blocks: List<TranslatedBlock>) {
        if (chapterId <= 0L) return
        try {
            migrateLegacy(chapterId, targetLanguage)
            writePage(File(pageDirectory(chapterId, targetLanguage), "$pageIndex.json"), blocks.map { SerializedTranslatedBlock.from(it) })
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to update page $pageIndex translation for $chapterId"
            }
        }
    }

    @Synchronized
    fun removePage(chapterId: Long, pageIndex: Int, targetLanguage: String) {
        if (chapterId <= 0L) return
        migrateLegacy(chapterId, targetLanguage)
        val page = File(pageDirectory(chapterId, targetLanguage), "$pageIndex.json")
        check(!page.exists() || page.delete()) { "Could not invalidate saved translation" }
    }

    private fun pageDirectory(chapterId: Long, language: String) = File(getFile(chapterId, language).path + ".pages")

    private fun readPage(file: File): List<SerializedTranslatedBlock>? = try {
        if (file.exists()) json.decodeFromString<List<SerializedTranslatedBlock>>(file.readText()) else null
    } catch (e: Exception) {
        logcat(LogPriority.WARN, e) { "Ignoring corrupt translation page" }
        null
    }

    private fun writePage(file: File, blocks: List<SerializedTranslatedBlock>) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temporary = File(file.path + ".tmp")
        try {
            temporary.outputStream().use { output ->
                output.write(json.encodeToString(blocks).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    // Keep the old chapter until every page is durable; interrupted migration can be retried.
    private fun migrateLegacy(chapterId: Long, language: String) {
        val legacy = getFile(chapterId, language)
        if (!legacy.exists()) return
        val parsed = try {
            json.decodeFromString<ChapterTranslationCache>(legacy.readText())
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Ignoring corrupt legacy translation" }
            check(legacy.delete())
            return
        }
        parsed.pages.forEach { (index, blocks) ->
            val page = File(pageDirectory(chapterId, language), "$index.json")
            if (!page.exists()) writePage(page, blocks)
        }
        check(legacy.delete()) { "Could not finish translation cache migration" }
    }

    @Synchronized
    fun clearChapter(chapterId: Long) {
        if (chapterId <= 0L) return
        try {
            val prefix = "${chapterId}_"
            cacheDir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) {
                "Failed to delete chapter translations for $chapterId"
            }
        }
    }

    @Synchronized
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

object TranslationDiskCache {
    private val store by lazy { TranslationPageStore(File(Injekt.get<Context>().cacheDir, "chapter_translations")) }

    fun getPage(chapterId: Long, pageIndex: Int, language: String) = store.getPage(chapterId, pageIndex, language)
    fun getChapter(chapterId: Long, language: String) = store.getChapter(chapterId, language)
    fun putPage(chapterId: Long, pageIndex: Int, language: String, blocks: List<TranslatedBlock>) = store.putPage(chapterId, pageIndex, language, blocks)
    fun saveChapter(chapterId: Long, language: String, pages: Map<Int, List<TranslatedBlock>>) = store.saveChapter(chapterId, language, pages)
    fun removePage(chapterId: Long, pageIndex: Int, language: String) = store.removePage(chapterId, pageIndex, language)
    fun hasChapterTranslation(chapterId: Long, language: String) = store.hasChapterTranslation(chapterId, language)
    fun clearChapter(chapterId: Long) = store.clearChapter(chapterId)
    fun clearAll() = store.clearAll()
    fun getDiskUsageBytes() = store.getDiskUsageBytes()
}
