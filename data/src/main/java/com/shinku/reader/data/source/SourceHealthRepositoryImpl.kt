package com.shinku.reader.data.source

import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import kotlinx.coroutines.flow.Flow

class SourceHealthRepositoryImpl(
    private val handler: DatabaseHandler,
) : SourceHealthRepository {

    override fun subscribeAll(): Flow<List<SourceHealth>> {
        return handler.subscribeToList { source_healthQueries.findAll(::mapSourceHealth) }
    }

    override suspend fun getBySourceId(sourceId: Long): SourceHealth? {
        return handler.awaitOneOrNull { source_healthQueries.findOne(sourceId, ::mapSourceHealth) }
    }

    override suspend fun updateStats(sourceId: Long, isSuccess: Boolean, latency: Long, error: String?) {
        handler.await {
            source_healthQueries.updateStats(
                id = sourceId,
                is_success = if (isSuccess) 1L else 0L,
                timestamp = System.currentTimeMillis(),
                latency = latency,
                error = error,
            )
        }
    }

    override suspend fun resetStats(sourceId: Long) {
        handler.await {
            source_healthQueries.resetStats(sourceId)
        }
    }
}
