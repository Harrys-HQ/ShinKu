package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.exh.metadata.sql.models.SearchTitle
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class GetSearchTitles(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTitle> {
        return mangaMetadataRepository.getTitlesById(mangaId)
    }
}
