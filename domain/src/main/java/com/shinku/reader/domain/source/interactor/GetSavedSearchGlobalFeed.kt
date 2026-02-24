package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class GetSavedSearchGlobalFeed(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(): List<SavedSearch> {
        return feedSavedSearchRepository.getGlobalFeedSavedSearch()
    }
}
