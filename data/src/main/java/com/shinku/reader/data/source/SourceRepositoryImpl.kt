package com.shinku.reader.data.source

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import com.shinku.reader.exh.source.MERGED_SOURCE_ID
import com.shinku.reader.exh.source.isEhBasedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.source.model.SourceWithCount
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.domain.source.repository.SourcePagingSource
import com.shinku.reader.domain.source.repository.SourceRepository
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.source.model.Source as DomainSource

class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val handler: DatabaseHandler,
) : SourceRepository {

    override fun getSources(): Flow<List<DomainSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineSources(): Flow<List<DomainSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources
                .filterIsInstance<HttpSource>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        return combine(
            handler.subscribeToList { mangasQueries.getSourceIdWithFavoriteCount() },
            sourceManager.catalogueSources,
        ) { sourceIdWithFavoriteCount, _ -> sourceIdWithFavoriteCount }
            .map {
                // SY -->
                it.filterNot { it.source == MERGED_SOURCE_ID }
                    // SY <--
                    .map { (sourceId, count) ->
                        val source = sourceManager.getOrStub(sourceId)
                        val domainSource = mapSourceToDomainSource(source).copy(
                            isStub = source is StubSource,
                        )
                        domainSource to count
                    }
            }
    }

    override fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>> {
        val sourceIdWithNonLibraryManga =
            handler.subscribeToList { mangasQueries.getSourceIdsWithNonLibraryManga() }
        return sourceIdWithNonLibraryManga.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = mapSourceToDomainSource(source).copy(
                    isStub = source is StubSource,
                )
                SourceWithCount(domainSource, count)
            }
        }
    }

    override fun search(
        sourceId: Long,
        query: String,
        filterList: FilterList,
    ): SourcePagingSource {
        val source = sourceManager.get(sourceId) as CatalogueSource
        // SY -->
        if (source.isEhBasedSource()) {
            return EHentaiSearchPagingSource(source, query, filterList)
        }
        // SY <--
        return SourceSearchPagingSource(source, query, filterList)
    }

    override fun getPopular(sourceId: Long): SourcePagingSource {
        val source = sourceManager.get(sourceId) as CatalogueSource
        // SY -->
        if (source.isEhBasedSource()) {
            return EHentaiPopularPagingSource(source)
        }
        // SY <--
        return SourcePopularPagingSource(source)
    }

    override fun getLatest(sourceId: Long): SourcePagingSource {
        val source = sourceManager.get(sourceId) as CatalogueSource
        // SY -->
        if (source.isEhBasedSource()) {
            return EHentaiLatestPagingSource(source)
        }
        // SY <--
        return SourceLatestPagingSource(source)
    }

    private fun mapSourceToDomainSource(source: Source): DomainSource = DomainSource(
        id = source.id,
        lang = source.lang,
        name = source.name,
        supportsLatest = false,
        isStub = false,
    )
}
