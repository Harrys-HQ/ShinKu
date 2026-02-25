package com.shinku.reader.domain.source.repository

import com.shinku.reader.domain.source.model.SourceHealth
import kotlinx.coroutines.flow.Flow

interface SourceHealthRepository {
    fun subscribeAll(): Flow<List<SourceHealth>>
    suspend fun getBySourceId(sourceId: Long): SourceHealth?
    suspend fun updateStats(sourceId: Long, isSuccess: Boolean, latency: Long, error: String?)
    suspend fun resetStats(sourceId: Long)
}
