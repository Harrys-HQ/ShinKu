package com.shinku.reader.domain.chapter.interactor

import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.chapter.repository.ChapterRepository

class GetChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return try {
            chapterRepository.getChapterByMangaId(mangaId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
