package com.shinku.reader.domain.chapter.interactor

import com.shinku.reader.domain.chapter.repository.ChapterRepository

class DeleteChapters(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapters: List<Long>) {
        chapterRepository.removeChaptersWithIds(chapters)
    }
}
