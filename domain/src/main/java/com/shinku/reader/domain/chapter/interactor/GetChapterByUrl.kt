package com.shinku.reader.domain.chapter.interactor

import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.chapter.repository.ChapterRepository

class GetChapterByUrl(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String): List<Chapter> {
        return try {
            chapterRepository.getChapterByUrl(url)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
