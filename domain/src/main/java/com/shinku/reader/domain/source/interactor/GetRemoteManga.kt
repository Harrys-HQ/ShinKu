package com.shinku.reader.domain.source.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import com.shinku.reader.domain.source.repository.SourcePagingSource
import com.shinku.reader.domain.source.repository.SourceRepository

class GetRemoteManga(
    private val repository: SourceRepository,
) {

    operator fun invoke(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource {
        return when (query) {
            QUERY_POPULAR -> repository.getPopular(sourceId)
            QUERY_LATEST -> repository.getLatest(sourceId)
            else -> repository.search(sourceId, query, filterList)
        }
    }

    companion object {
        const val QUERY_POPULAR = "com.shinku.reader.domain.source.interactor.POPULAR"
        const val QUERY_LATEST = "com.shinku.reader.domain.source.interactor.LATEST"
    }
}
