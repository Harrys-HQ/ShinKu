package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.exh.metadata.sql.models.SearchMetadata
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class GetSearchMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): SearchMetadata? {
        return mangaMetadataRepository.getMetadataById(mangaId)
    }

    suspend fun await(): List<SearchMetadata> {
        return mangaMetadataRepository.getSearchMetadata()
    }
}
