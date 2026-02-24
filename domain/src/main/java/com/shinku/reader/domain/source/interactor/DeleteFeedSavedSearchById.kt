package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class DeleteFeedSavedSearchById(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(feedSavedSearchId: Long) {
        return feedSavedSearchRepository.delete(feedSavedSearchId)
    }
}
