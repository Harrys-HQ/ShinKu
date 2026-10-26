package com.shinku.reader.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    // SY -->
    /** Value to check if this page is used to as if it was too wide */
    var shiftedPage: Boolean = false,
    /** Value to check if a page is can be doubled up, but can't because the next page is too wide */
    var isolatedPage: Boolean = false,
    // SY <--
    var stream: (() -> InputStream)? = null,

) : Page(index, url, imageUrl, null), ReaderItem {

    open lateinit var chapter: ReaderChapter

    /** Value to check if a page is too wide to be doubled up */
    var fullPage: Boolean = false
        set(value) {
            field = value
            if (value) shiftedPage = false
        }

    // Translation Overlay Data
    var translationBlocks: List<TranslatedBlock>? = null
    var showTranslation: Boolean = false
}

data class TranslatedBlock(
    val originalText: String,
    val translatedText: String,
    val boundingBox: android.graphics.Rect?,
    val bubbleRect: android.graphics.Rect? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val bgColor: Int,
    val textColor: Int,
)

object TranslationCache {
    private val memCache = android.util.LruCache<String, List<TranslatedBlock>>(128)

    fun get(chapterId: Long, pageIndex: Int, targetLanguage: String): List<TranslatedBlock>? {
        val key = "${chapterId}_${pageIndex}_$targetLanguage"
        memCache.get(key)?.let { return it }

        // Fallback to persistent disk cache
        val diskBlocks = com.shinku.reader.data.cache.TranslationDiskCache.getPage(chapterId, pageIndex, targetLanguage)
        if (diskBlocks != null) {
            memCache.put(key, diskBlocks)
            return diskBlocks
        }
        return null
    }

    fun put(chapterId: Long, pageIndex: Int, targetLanguage: String, blocks: List<TranslatedBlock>) {
        val key = "${chapterId}_${pageIndex}_$targetLanguage"
        memCache.put(key, blocks)
        com.shinku.reader.data.cache.TranslationDiskCache.putPage(chapterId, pageIndex, targetLanguage, blocks)
    }

    fun getChapter(chapterId: Long, targetLanguage: String): Map<Int, List<TranslatedBlock>>? {
        val diskChapter = com.shinku.reader.data.cache.TranslationDiskCache.getChapter(chapterId, targetLanguage)
        diskChapter?.forEach { (index, blocks) ->
            memCache.put("${chapterId}_${index}_$targetLanguage", blocks)
        }
        return diskChapter
    }

    fun saveChapter(chapterId: Long, targetLanguage: String, pages: Map<Int, List<TranslatedBlock>>) {
        pages.forEach { (index, blocks) ->
            memCache.put("${chapterId}_${index}_$targetLanguage", blocks)
        }
        com.shinku.reader.data.cache.TranslationDiskCache.saveChapter(chapterId, targetLanguage, pages)
    }

    fun hasChapter(chapterId: Long, targetLanguage: String): Boolean {
        return com.shinku.reader.data.cache.TranslationDiskCache.hasChapterTranslation(chapterId, targetLanguage)
    }

    fun clearChapter(chapterId: Long) {
        com.shinku.reader.data.cache.TranslationDiskCache.clearChapter(chapterId)
        memCache.evictAll()
    }

    fun clear() {
        memCache.evictAll()
        com.shinku.reader.data.cache.TranslationDiskCache.clearAll()
    }
}
