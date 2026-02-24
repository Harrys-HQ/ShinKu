package com.shinku.reader.domain.manga.repository

import com.shinku.reader.domain.manga.model.FavoriteEntry
import com.shinku.reader.domain.manga.model.FavoriteEntryAlternative

interface FavoritesEntryRepository {
    suspend fun deleteAll()

    suspend fun insertAll(favoriteEntries: List<FavoriteEntry>)

    suspend fun selectAll(): List<FavoriteEntry>

    suspend fun addAlternative(favoriteEntryAlternative: FavoriteEntryAlternative)
}
