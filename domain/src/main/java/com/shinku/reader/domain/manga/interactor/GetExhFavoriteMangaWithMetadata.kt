package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaMetadataRepository

class GetExhFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(): List<Manga> {
        return mangaMetadataRepository.getExhFavoriteMangaWithMetadata()
    }
}
