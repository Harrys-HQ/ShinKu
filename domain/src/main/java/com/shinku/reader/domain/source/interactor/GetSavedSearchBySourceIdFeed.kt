package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class GetSavedSearchBySourceIdFeed(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(sourceId: Long): List<SavedSearch> {
        return feedSavedSearchRepository.getBySourceIdFeedSavedSearch(sourceId)
    }
}
