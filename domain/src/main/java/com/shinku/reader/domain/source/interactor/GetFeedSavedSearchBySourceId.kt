package com.shinku.reader.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class GetFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(sourceId: Long): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getBySourceId(sourceId)
    }

    fun subscribe(sourceId: Long): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getBySourceIdAsFlow(sourceId)
    }
}
