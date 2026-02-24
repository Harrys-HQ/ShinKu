package com.shinku.reader.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository

class GetFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    suspend fun await(): List<FeedSavedSearch> {
        return feedSavedSearchRepository.getGlobal()
    }

    fun subscribe(): Flow<List<FeedSavedSearch>> {
        return feedSavedSearchRepository.getGlobalAsFlow()
    }
}
