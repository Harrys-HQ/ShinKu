package com.shinku.reader.exh.recs

import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import com.shinku.reader.ui.browse.source.browse.BrowseSourceScreenModel
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.recs.sources.RecommendationPagingSource
import com.shinku.reader.exh.recs.sources.StaticResultPagingSource
import com.shinku.reader.data.source.NoResultsException
import com.shinku.reader.domain.source.repository.SourcePagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRecommendsScreenModel(
    private val args: BrowseRecommendsScreen.Args,
    private val getManga: GetManga = Injekt.get(),
) : BrowseSourceScreenModel(
    sourceId = when (args) {
        is BrowseRecommendsScreen.Args.SingleSourceManga -> args.sourceId
        is BrowseRecommendsScreen.Args.MergedSourceMangas -> args.results.recAssociatedSourceId ?: -1
    },
    listingQuery = null,
) {
    suspend fun resolveRecommendationSource(): RecommendationPagingSource? {
        return when (args) {
            is BrowseRecommendsScreen.Args.MergedSourceMangas -> StaticResultPagingSource(args.results)
            is BrowseRecommendsScreen.Args.SingleSourceManga -> {
                val catalogue = source as? CatalogueSource ?: return null
                val manga = getManga.await(args.mangaId) ?: return null
                RecommendationPagingSource.createSources(manga, catalogue).firstOrNull {
                    it::class.qualifiedName == args.recommendationSourceName
                }
            }
        }
    }

    override fun createSourcePagingSource(query: String, filters: FilterList): SourcePagingSource =
        object : SourcePagingSource() {
            @Volatile
            private var delegate: RecommendationPagingSource? = null

            override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Pair<Manga, RaisedSearchMetadata?>> {
                return try {
                    val pagingSource = delegate ?: resolveRecommendationSource()?.also { delegate = it }
                        ?: return LoadResult.Error(NoResultsException())
                    pagingSource.load(params)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LoadResult.Error(e)
                }
            }

            override fun getRefreshKey(state: PagingState<Long, Pair<Manga, RaisedSearchMetadata?>>): Long? =
                delegate?.getRefreshKey(state)
        }

    override fun Flow<Manga>.combineMetadata(metadata: RaisedSearchMetadata?): Flow<Pair<Manga, RaisedSearchMetadata?>> {
        // Overridden to prevent our custom metadata from being replaced from a cache
        return flatMapLatest { manga -> flowOf(manga to metadata) }
    }

    init {
        mutableState.update { it.copy(filterable = false) }
    }
}
