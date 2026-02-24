package com.shinku.reader.exh.ui.smartsearch

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.CatalogueSource
import com.shinku.reader.ui.browse.source.SourcesScreen
import kotlinx.coroutines.CancellationException
import com.shinku.reader.feature.migration.list.search.SmartSourceSearchEngine
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SmartSearchScreenModel(
    sourceId: Long,
    private val config: SourcesScreen.SmartSearchConfig,
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<SmartSearchScreenModel.SearchResults?>(null) {
    private val smartSearchEngine = SmartSourceSearchEngine(null)

    val source = sourceManager.get(sourceId) as CatalogueSource

    init {
        screenModelScope.launchIO {
            val result = try {
                val resultManga = smartSearchEngine.deepSearch(source, config.origTitle)
                if (resultManga != null) {
                    val localManga = networkToLocalManga(resultManga)
                    SearchResults.Found(localManga)
                } else {
                    SearchResults.NotFound
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                } else {
                    SearchResults.Error
                }
            }

            mutableState.value = result
        }
    }

    sealed class SearchResults {
        data class Found(val manga: Manga) : SearchResults()
        data object NotFound : SearchResults()
        data object Error : SearchResults()
    }
}
