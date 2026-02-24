package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class CountFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(sourceId: Long): Long {
        return feedSavedSearchRepository.countBySourceId(sourceId)
    }
}
