package com.shinku.reader.data.source

import com.shinku.reader.domain.source.model.FeedCategory

object FeedCategoryMapper {
    fun map(
        id: Long,
        name: String,
        sort: Long,
        lang: String,
        explore: Boolean,
    ): FeedCategory {
        return FeedCategory(
            id = id,
            name = name,
            sort = sort,
            lang = lang,
            explore = explore,
        )
    }
}
