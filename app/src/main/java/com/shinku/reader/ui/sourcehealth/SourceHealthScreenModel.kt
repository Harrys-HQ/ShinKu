package com.shinku.reader.ui.sourcehealth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthItem
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthScreenState
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthSort
import eu.kanade.tachiyomi.source.local.LocalSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceHealthScreenModel(
    private val getSourceHealth: GetSourceHealth = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
) : StateScreenModel<SourceHealthScreenState>(SourceHealthScreenState.Loading) {

    private val _sortMode = MutableStateFlow(SourceHealthSort.Health)

    init {
        // Trigger available extension fetch
        screenModelScope.launchIO {
            extensionManager.findAvailableExtensions()
        }

        combine(
            sourceManager.catalogueSources,
            extensionManager.availableExtensionsFlow,
            getSourceHealth.subscribeAll(),
            _sortMode
        ) { installedSources, availableExtensions, healthList, sortMode ->
            val healthMap = healthList.associateBy { it.sourceId }
            val installedSourceIds = installedSources.map { it.id }.toSet()
            
            // Filter repo sources for English OR already installed
            val repoSources = availableExtensions.flatMap { ext -> 
                ext.sources.filter { it.lang == "en" || it.id in installedSourceIds }
            }
            
            // Group by Name to deduplicate language variants
            val allSourceGroups = (installedSources.map { it.id to it.name } + 
                                repoSources.map { it.id to it.name })
                .filterNot { it.first == LocalSource.ID }
                .groupBy { 
                    it.second.substringBeforeLast(" (")
                             .substringBeforeLast(" [")
                             .trim()
                }

            val allItems = allSourceGroups.map { (name, sources) ->
                val representativeSource = sources.find { healthMap.containsKey(it.first) } ?: sources.first()
                val id = representativeSource.first
                
                val dummySource = object : eu.kanade.tachiyomi.source.Source {
                    override val id: Long = id
                    override val name: String = name
                    override val lang: String = ""
                    override suspend fun getMangaDetails(manga: eu.kanade.tachiyomi.source.model.SManga) = throw Exception()
                    override suspend fun getChapterList(manga: eu.kanade.tachiyomi.source.model.SManga) = throw Exception()
                    override suspend fun getPageList(chapter: eu.kanade.tachiyomi.source.model.SChapter) = throw Exception()
                }
                
                val isInstalled = sources.any { it.first in installedSourceIds }
                isInstalled to SourceHealthItem(dummySource, healthMap[id])
            }

            val (installed, repo) = allItems.partition { it.first }

            val comparator = when (sortMode) {
                SourceHealthSort.Health -> compareByDescending<SourceHealthItem> { it.health?.performanceScore ?: -1 }
                    .thenBy { it.source.name }
                SourceHealthSort.Speed -> compareBy<SourceHealthItem> { 
                    // Put never scanned at the end
                    if (it.health == null) Long.MAX_VALUE else it.health.avgLatency 
                }.thenBy { it.source.name }
                SourceHealthSort.Name -> compareBy { it.source.name }
            }

            mutableState.update { 
                SourceHealthScreenState.Success(
                    installedList = installed.map { it.second }.sortedWith(comparator),
                    repoList = repo.map { it.second }.sortedWith(comparator),
                    sortMode = sortMode
                )
            }
        }
        .launchIn(screenModelScope)
    }

    fun setSortMode(mode: SourceHealthSort) {
        _sortMode.value = mode
    }
}
