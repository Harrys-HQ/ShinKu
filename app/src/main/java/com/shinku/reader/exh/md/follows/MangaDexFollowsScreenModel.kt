package com.shinku.reader.exh.md.follows

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import com.shinku.reader.ui.browse.source.browse.BrowseSourceScreenModel
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.source.getMainSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import com.shinku.reader.data.source.BaseSourcePagingSource
import com.shinku.reader.domain.manga.model.Manga

class MangaDexFollowsScreenModel(sourceId: Long) : BrowseSourceScreenModel(sourceId, null) {

    override fun createSourcePagingSource(query: String, filters: FilterList): BaseSourcePagingSource {
        return MangaDexFollowsPagingSource(source.getMainSource() as MangaDex)
    }

    override fun Flow<Manga>.combineMetadata(metadata: RaisedSearchMetadata?): Flow<Pair<Manga, RaisedSearchMetadata?>> {
        return map { it to metadata }
    }

    init {
        mutableState.update { it.copy(filterable = false) }
    }
}
