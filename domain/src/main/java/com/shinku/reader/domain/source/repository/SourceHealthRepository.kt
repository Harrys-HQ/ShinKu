package com.shinku.reader.domain.source.repository

import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.model.SourceProbeOutcome
import kotlinx.coroutines.flow.Flow

interface SourceHealthRepository {
    fun subscribeAll(): Flow<List<SourceHealth>>
    suspend fun getBySourceId(sourceId: Long): SourceHealth?
    suspend fun updateStats(sourceId: Long, isSuccess: Boolean, latency: Long, error: String?)
    suspend fun updateOutcome(sourceId: Long, outcome: SourceProbeOutcome)
    suspend fun resetStats(sourceId: Long)
    suspend fun resetAllStats()
}
