package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository
import kotlinx.coroutines.flow.Flow

class GetFeedSavedSearchGlobalByCategory(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {
    suspend fun await(categoryId: Long): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getGlobalByCategory(categoryId)
    }

    fun subscribe(categoryId: Long): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getGlobalByCategoryAsFlow(categoryId)
    }
}
