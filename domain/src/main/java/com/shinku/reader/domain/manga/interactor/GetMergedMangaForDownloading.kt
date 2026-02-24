package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class GetMergedMangaForDownloading(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(mergeId: Long): List<Manga> {
        return mangaMergeRepository.getMergeMangaForDownloading(mergeId)
    }
}
