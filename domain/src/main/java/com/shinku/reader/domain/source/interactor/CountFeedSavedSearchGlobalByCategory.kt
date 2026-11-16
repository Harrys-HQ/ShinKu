package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class CountFeedSavedSearchGlobalByCategory(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {
    suspend fun await(categoryId: Long): Long {
        return feedSavedSearchRepository.countGlobalByCategory(categoryId)
    }
}
