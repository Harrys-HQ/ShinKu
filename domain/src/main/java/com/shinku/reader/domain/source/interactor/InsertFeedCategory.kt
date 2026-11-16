package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.repository.FeedCategoryRepository

class InsertFeedCategory(
    private val repository: FeedCategoryRepository,
) {
    suspend fun await(name: String, lang: String = "all", explore: Boolean = false): Long {
        return repository.insert(name.trim(), lang, explore)
    }
}
