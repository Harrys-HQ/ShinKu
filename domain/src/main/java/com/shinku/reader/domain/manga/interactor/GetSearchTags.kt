package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.exh.metadata.sql.models.SearchTag
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class GetSearchTags(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTag> {
        return mangaMetadataRepository.getTagsById(mangaId)
    }
}
