package com.shinku.reader.domain.history.model

import com.shinku.reader.domain.manga.interactor.GetCustomMangaInfo
import com.shinku.reader.domain.manga.model.MangaCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    // SY -->
    val ogTitle: String,
    // SY <--
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: MangaCover,
) {
    // SY -->
    val title: String = customMangaManager.get(mangaId)?.title ?: ogTitle

    companion object {
        private val customMangaManager: GetCustomMangaInfo by injectLazy()
    }
    // SY <--
}
