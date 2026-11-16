package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedCategoryRepository

class UpdateFeedCategory(
    private val repository: FeedCategoryRepository,
) {
    suspend fun await(id: Long, name: String, lang: String) {
        repository.update(id, name.trim(), lang)
    }

    suspend fun updateSort(id: Long, sort: Long) {
        repository.updateSort(id, sort)
    }
}
