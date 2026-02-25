package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import kotlinx.coroutines.flow.Flow

class GetSourceHealth(
    private val repository: SourceHealthRepository,
) {
    fun subscribeAll(): Flow<List<SourceHealth>> {
        return repository.subscribeAll()
    }

    suspend fun await(sourceId: Long): SourceHealth? {
        return repository.getBySourceId(sourceId)
    }
}
