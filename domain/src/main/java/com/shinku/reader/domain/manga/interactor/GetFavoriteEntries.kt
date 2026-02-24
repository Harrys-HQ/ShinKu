package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.FavoriteEntry
import com.shinku.reader.domain.manga.repository.FavoritesEntryRepository

class GetFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(): List<FavoriteEntry> {
        return favoriteEntryRepository.selectAll()
    }
}
