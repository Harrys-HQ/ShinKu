package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.repository.SavedSearchRepository

class GetSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(savedSearchId: Long): SavedSearch {
        return savedSearchRepository.getById(savedSearchId)!!
    }

    suspend fun awaitOrNull(savedSearchId: Long): SavedSearch? {
        return savedSearchRepository.getById(savedSearchId)
    }
}
