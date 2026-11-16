package com.shinku.reader.domain.source.repository

import com.shinku.reader.domain.source.model.FeedCategory
import kotlinx.coroutines.flow.Flow

interface FeedCategoryRepository {
    suspend fun getAll(): List<FeedCategory>
    fun getAllAsFlow(): Flow<List<FeedCategory>>
    suspend fun getById(id: Long): FeedCategory?
    suspend fun insert(name: String, lang: String = "all", explore: Boolean = false): Long
    suspend fun update(id: Long, name: String, lang: String)
    suspend fun updateSort(id: Long, sort: Long)
    suspend fun delete(id: Long)
}
