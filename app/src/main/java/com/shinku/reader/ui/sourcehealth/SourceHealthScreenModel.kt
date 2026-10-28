package com.shinku.reader.ui.sourcehealth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthItem
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthScreenState
import com.shinku.reader.presentation.more.sourcehealth.SourceHealthSort
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.local.LocalSource
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Headers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.system.measureTimeMillis

class SourceHealthScreenModel(
    private val getSourceHealth: GetSourceHealth = Injekt.get(),
    private val updateSourceHealth: UpdateSourceHealth = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
) : StateScreenModel<SourceHealthScreenState>(SourceHealthScreenState.Loading) {

    private val _sortMode = MutableStateFlow(SourceHealthSort.Health)
    private val _testingIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        screenModelScope.launchIO {
            extensionManager.findAvailableExtensions()
        }

        combine(
            sourceManager.catalogueSources,
            extensionManager.availableExtensionsFlow,
            getSourceHealth.subscribeAll(),
            _sortMode,
            _testingIds,
        ) { installedSources, availableExtensions, healthList, sortMode, testingIds ->
            val healthMap = healthList.associateBy { it.sourceId }
            val installedSourceIds = installedSources.map { it.id }.toSet()

            // Filter repo sources for English OR already installed
            val repoSources = availableExtensions.flatMap { ext ->
                ext.sources.filter { it.lang == "en" || it.id in installedSourceIds }
            }

            // Group by Name to deduplicate language variants
            val allSourceGroups = (
                installedSources.map { it.id to it.name } +
                    repoSources.map { it.id to it.name }
                )
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
                    if (it.health == null) Long.MAX_VALUE else it.health.avgLatency
                }.thenBy { it.source.name }
                SourceHealthSort.Name -> compareBy { it.source.name }
            }

            mutableState.update {
                SourceHealthScreenState.Success(
                    installedList = installed.map { it.second }.sortedWith(comparator),
                    repoList = repo.map { it.second }.sortedWith(comparator),
                    sortMode = sortMode,
                    testingSourceIds = testingIds,
                )
            }
        }
            .launchIn(screenModelScope)
    }

    fun setSortMode(mode: SourceHealthSort) {
        _sortMode.value = mode
    }

    fun testSingleSource(sourceId: Long) {
        if (sourceId in _testingIds.value) return
        _testingIds.update { it + sourceId }

        screenModelScope.launchIO {
            try {
                val installedHttpSource = sourceManager.get(sourceId) as? HttpSource
                val repoSource = extensionManager.availableExtensionsFlow.value
                    .asSequence()
                    .flatMap { it.sources.asSequence() }
                    .firstOrNull { it.id == sourceId }

                val targetUrl = installedHttpSource?.baseUrl ?: repoSource?.baseUrl
                if (targetUrl.isNullOrBlank()) return@launchIO

                val headers = installedHttpSource?.headers ?: Headers.Builder()
                    .add("User-Agent", networkHelper.defaultUserAgentProvider())
                    .build()
                val client = installedHttpSource?.client ?: networkHelper.client

                var success = false
                var error: String? = null
                val latency = measureTimeMillis {
                    val completed = withTimeoutOrNull(10_000L) {
                        try {
                            client.newCall(GET(targetUrl, headers)).awaitSuccess().use {
                                success = true
                            }
                        } catch (e: Exception) {
                            error = e.message
                        }
                        true
                    }
                    if (completed == null) {
                        error = "Connection timed out after 10 seconds"
                    }
                }
                updateSourceHealth.await(sourceId, success, if (success) latency else 0L, error)
            } finally {
                _testingIds.update { it - sourceId }
            }
        }
    }
}
