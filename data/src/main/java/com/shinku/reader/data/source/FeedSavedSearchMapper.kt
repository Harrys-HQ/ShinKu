package com.shinku.reader.data.source

import com.shinku.reader.domain.source.model.FeedSavedSearch

object FeedSavedSearchMapper {
    fun map(
        id: Long,
        source: Long,
        savedSearch: Long?,
        global: Boolean,
        category: Long,
        feedOrder: Long,
        searchType: Long,
    ): FeedSavedSearch {
        return FeedSavedSearch(
            id = id,
            source = source,
            savedSearch = savedSearch,
            global = global,
            category = category,
            feedOrder = feedOrder,
            searchType = searchType,
        )
    }
}
