package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedCategoryRepository
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class DeleteFeedCategory(
    private val feedCategoryRepository: FeedCategoryRepository,
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {
    suspend fun await(id: Long) {
        if (id == 1L) return
        feedSavedSearchRepository.deleteByCategory(id)
        feedCategoryRepository.delete(id)
    }
}
