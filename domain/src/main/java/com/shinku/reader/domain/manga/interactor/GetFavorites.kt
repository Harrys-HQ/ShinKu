package com.shinku.reader.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.repository.MangaRepository

class GetFavorites(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<Manga> {
        return mangaRepository.getFavorites()
    }

    fun subscribe(sourceId: Long): Flow<List<Manga>> {
        return mangaRepository.getFavoritesBySourceId(sourceId)
    }
}
