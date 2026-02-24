package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.SavedSearchRepository

class DeleteSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    suspend fun await(savedSearchId: Long) {
        return savedSearchRepository.delete(savedSearchId)
    }
}
