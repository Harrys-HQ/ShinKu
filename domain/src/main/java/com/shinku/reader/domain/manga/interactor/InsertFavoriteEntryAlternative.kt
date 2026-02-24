package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.model.FavoriteEntryAlternative
import com.shinku.reader.domain.manga.repository.FavoritesEntryRepository

class InsertFavoriteEntryAlternative(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(entry: FavoriteEntryAlternative) {
        return favoriteEntryRepository.addAlternative(entry)
    }
}
