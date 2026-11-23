package com.shinku.reader.ui.browse.source.globalsearch

import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.ui.browse.BulkAddFavoritesHelper
import com.shinku.reader.ui.browse.BulkFavoriteCoordinator
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.flow.update

class GlobalSearchScreenModel(
    initialQuery: String = "",
    initialExtensionFilter: String? = null,
    bulkAddHelper: BulkAddFavoritesHelper = BulkAddFavoritesHelper(),
) : SearchScreenModel(State(searchQuery = initialQuery)) {

    private val bulkFavoriteCoordinator = BulkFavoriteCoordinator(bulkAddHelper)
    val bulkFavoriteEvents = bulkFavoriteCoordinator.events

    init {
        extensionFilter = initialExtensionFilter
        if (initialQuery.isNotBlank() || !initialExtensionFilter.isNullOrBlank()) {
            if (extensionFilter != null) {
                // we're going to use custom extension filter instead
                setSourceFilter(SourceFilter.All)
            }
            search()
        }
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        return super.getEnabledSources()
            .filter { state.value.sourceFilter != SourceFilter.PinnedOnly || "${it.id}" in pinnedSources }
    }

    fun enterSelectionMode() {
        mutableState.update { it.copy(selection = it.selection.enterSelectionMode()) }
    }

    fun exitSelectionMode() {
        mutableState.update { it.copy(selection = it.selection.exitSelectionMode()) }
    }

    fun toggleSelection(manga: Manga) {
        mutableState.update { it.copy(selection = it.selection.toggle(manga)) }
    }

    private fun loadedResults(): List<Manga> {
        return state.value.filteredItems.values
            .filterIsInstance<SearchItemResult.Success>()
            .flatMap { it.result }
    }

    fun selectAllLoaded() {
        val loaded = loadedResults()
        mutableState.update { it.copy(selection = it.selection.selectAllLoaded(loaded)) }
    }

    fun invertLoaded() {
        val loaded = loadedResults()
        mutableState.update { it.copy(selection = it.selection.invertLoaded(loaded)) }
    }

    fun requestBulkAddToLibrary() {
        bulkFavoriteCoordinator.requestBulkAdd(
            scope = screenModelScope,
            selected = state.value.selection.selectedManga,
            sourceProvider = ::getSourceOrStub,
            onNeedsCategoryChoice = { mangas, initialSelection ->
                mutableState.update {
                    it.copy(dialog = Dialog.BulkChangeMangaCategory(mangas, initialSelection))
                }
            },
            onFinished = ::exitSelectionMode,
        )
    }

    fun confirmBulkAddToLibrary(mangas: List<Manga>, categoryIds: List<Long>) {
        bulkFavoriteCoordinator.confirmBulkAdd(
            scope = screenModelScope,
            mangas = mangas,
            categoryIds = categoryIds,
            sourceProvider = ::getSourceOrStub,
            onFinished = ::exitSelectionMode,
        )
    }
}
