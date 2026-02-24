package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaRepository

class GetMangaBySource(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(sourceId: Long): List<Manga> {
        return mangaRepository.getMangaBySourceId(sourceId)
    }
}
