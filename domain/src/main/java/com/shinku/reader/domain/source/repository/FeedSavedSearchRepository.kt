package com.shinku.reader.domain.source.repository

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch

interface FeedSavedSearchRepository {

    suspend fun getGlobal(): List<FeedSavedSearch>

    fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>>

    suspend fun getGlobalByCategory(categoryId: Long): List<FeedSavedSearch>

    fun getGlobalByCategoryAsFlow(categoryId: Long): Flow<List<FeedSavedSearch>>

    suspend fun getGlobalFeedSavedSearch(): List<SavedSearch>

    suspend fun getGlobalFeedSavedSearchByCategory(categoryId: Long): List<SavedSearch>

    suspend fun countGlobal(): Long

    suspend fun countGlobalByCategory(categoryId: Long): Long

    suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch>

    fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>>

    suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch>

    suspend fun countBySourceId(sourceId: Long): Long

    suspend fun delete(feedSavedSearchId: Long)

    suspend fun deleteByCategory(categoryId: Long)

    suspend fun insert(feedSavedSearch: FeedSavedSearch): Long?

    suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>)

    suspend fun reorderInCategory(id: Long, categoryId: Long, feedOrder: Long)
}
