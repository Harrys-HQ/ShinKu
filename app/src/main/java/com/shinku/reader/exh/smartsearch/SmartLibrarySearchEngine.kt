package com.shinku.reader.exh.smartsearch

import com.shinku.reader.feature.migration.list.search.BaseSmartSearchEngine
import com.shinku.reader.domain.library.model.LibraryManga

class SmartLibrarySearchEngine(
    extraSearchParams: String? = null,
) : BaseSmartSearchEngine<LibraryManga>(extraSearchParams, 0.7) {

    override fun getTitle(result: LibraryManga) = result.manga.ogTitle

    suspend fun smartSearch(library: List<LibraryManga>, title: String): LibraryManga? =
        deepSearch(
            { query ->
                library.filter { it.manga.ogTitle.contains(query, true) }
            },
            title,
        )
}
