package com.shinku.reader.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.repository.SavedSearchRepository

class GetSavedSearchBySourceId(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(sourceId: Long): List<SavedSearch> {
        return savedSearchRepository.getBySourceId(sourceId)
    }

    fun subscribe(sourceId: Long): Flow<List<SavedSearch>> {
        return savedSearchRepository.getBySourceIdAsFlow(sourceId)
    }
}
