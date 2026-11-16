package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.FeedCategory
import com.shinku.reader.domain.source.repository.FeedCategoryRepository
import kotlinx.coroutines.flow.Flow

class GetFeedCategories(
    private val repository: FeedCategoryRepository,
) {
    suspend fun await(): List<FeedCategory> {
        return repository.getAll()
    }

    fun subscribe(): Flow<List<FeedCategory>> {
        return repository.getAllAsFlow()
    }
}
