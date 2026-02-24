package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.FavoriteEntry
import com.shinku.reader.domain.manga.repository.FavoritesEntryRepository

class InsertFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(entries: List<FavoriteEntry>) {
        return favoriteEntryRepository.insertAll(entries)
    }
}
