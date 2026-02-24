package com.shinku.reader.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.FilterList
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.domain.source.model.SourceWithCount

typealias SourcePagingSource = PagingSource<Long, /*SY --> */ Pair<Manga, RaisedSearchMetadata?>/*SY <-- */>

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    fun search(sourceId: Long, query: String, filterList: FilterList): SourcePagingSource

    fun getPopular(sourceId: Long): SourcePagingSource

    fun getLatest(sourceId: Long): SourcePagingSource
}
