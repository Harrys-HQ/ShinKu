package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.FavoritesEntryRepository

class DeleteFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await() {
        return favoriteEntryRepository.deleteAll()
    }
}
