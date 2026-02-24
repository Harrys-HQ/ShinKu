package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class GetIdsOfFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(): List<Long> {
        return mangaMetadataRepository.getIdsOfFavoriteMangaWithMetadata()
    }
}
