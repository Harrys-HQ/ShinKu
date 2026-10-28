package com.shinku.reader.ui.browse.feed

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.toDomainManga
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@androidx.compose.runtime.Immutable
data class SuggestedTitlesScreenState(
    val mangas: ImmutableList<Manga> = persistentListOf(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
)

class SuggestedTitlesScreenModel(
    val title: String,
    initialMangas: List<Manga>,
    private val initialMangaIds: List<Long> = emptyList(),
    val mode: String,
    val query: String,
    private val sourceManager: SourceManager = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
    private val sourceHealthRepository: SourceHealthRepository = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<SuggestedTitlesScreenState>(
    SuggestedTitlesScreenState(
        mangas = initialMangas.distinctBy { it.id }.toImmutableList(),
        hasMore = mode == "genre" || mode == "for_you",
    ),
) {
    private val coroutineDispatcher = Dispatchers.IO.limitedParallelism(4)
    private var currentPage = if (initialMangas.isEmpty() && initialMangaIds.isEmpty()) 0 else 1
    private val loadedIds = mutableSetOf<Long>().apply {
        addAll(initialMangas.map { it.id })
    }

    init {
        if (initialMangas.isEmpty()) {
            if (initialMangaIds.isNotEmpty()) {
                screenModelScope.launchIO {
                    val restoredMangas = initialMangaIds.mapNotNull { id ->
                        getManga.await(id)
                    }
                    if (restoredMangas.isNotEmpty()) {
                        currentPage = 1
                        loadedIds.addAll(restoredMangas.map { it.id })
                        mutableState.update { state ->
                            state.copy(mangas = restoredMangas.distinctBy { it.id }.toImmutableList())
                        }
                    } else if (mode == "genre" || mode == "for_you") {
                        currentPage = 0
                        loadNextPage()
                    }
                }
            } else if (mode == "genre" || mode == "for_you") {
                loadNextPage()
            }
        }
    }

    private suspend fun getEligibleSources(): List<CatalogueSource> {
        val sourceFilterMode = shinkuPreferences.feedSourceFilter().get()
        val languageFilterMode = shinkuPreferences.feedLanguageFilter().get()
        val pinnedSources = sourcePreferences.pinnedSources().get()
        val enabledLanguages = sourcePreferences.enabledLanguages().get()
        val disabledSources = sourcePreferences.disabledSources().get()
            .mapNotNull { it.toLongOrNull() }.toSet()

        val allSources = sourceManager.getVisibleCatalogueSources()
            .ifEmpty { sourceManager.getCatalogueSources() }
            .filterNot { it.id in disabledSources }

        var filtered = if (languageFilterMode == "all") {
            allSources.filter { it.lang in enabledLanguages || it.lang == "all" }
        } else {
            allSources.filter { it.lang.equals(languageFilterMode, ignoreCase = true) || it.lang == "all" }
        }

        if (sourceFilterMode == "pinned" && pinnedSources.isNotEmpty()) {
            val pinned = filtered.filter { it.id.toString() in pinnedSources }
            if (pinned.isNotEmpty()) {
                filtered = pinned
            }
        }

        val candidates = filtered.ifEmpty { allSources }
        if (candidates.size <= 1) return candidates

        val healthMap = runCatching {
            sourceHealthRepository.subscribeAll().first().associateBy { it.sourceId }
        }.getOrDefault(emptyMap())

        val healthyCandidates = candidates.filter { source ->
            val health = healthMap[source.id]
            health == null || (!health.isDnsDead && !health.isServerDown && health.performanceScore >= 40)
        }

        val pool = healthyCandidates.ifEmpty { candidates }
        return pool.sortedByDescending { source ->
            healthMap[source.id]?.performanceScore ?: 85
        }
    }

    fun loadNextPage() {
        if (state.value.isLoadingMore || !state.value.hasMore) return

        mutableState.update { it.copy(isLoadingMore = true) }
        val nextPage = currentPage + 1

        screenModelScope.launchIO {
            try {
                val sources = getEligibleSources()
                if (sources.isEmpty()) {
                    mutableState.update { it.copy(isLoadingMore = false, hasMore = false) }
                    return@launchIO
                }

                val newResults: List<Manga> = when (mode) {
                    "genre" -> {
                        if (query.isBlank()) {
                            emptyList()
                        } else {
                            val supportingSources = sources.mapNotNull { source ->
                                val filterList = GenreFilterHelper.buildGenreFilterList(source, query)
                                if (filterList != null) source to filterList else null
                            }.take(3)

                            supportingSources.map { (source, filterList) ->
                                async(coroutineDispatcher) {
                                    withTimeoutOrNull(8_000L) {
                                        try {
                                            source.getSearchManga(nextPage, "", filterList).mangas.map {
                                                it.toDomainManga(source.id)
                                            }
                                        } catch (e: Throwable) {
                                            emptyList()
                                        }
                                    } ?: emptyList()
                                }
                            }.awaitAll().flatten()
                        }
                    }
                    "for_you" -> {
                        val latestSources = sources.filter { it.supportsLatest }.take(2).ifEmpty { sources.take(2) }
                        latestSources.map { source ->
                            async(coroutineDispatcher) {
                                withTimeoutOrNull(8_000L) {
                                    try {
                                        source.getLatestUpdates(nextPage).mangas.map {
                                            it.toDomainManga(source.id)
                                        }
                                    } catch (e: Throwable) {
                                        emptyList()
                                    }
                                } ?: emptyList()
                            }
                        }.awaitAll().flatten()
                    }
                    else -> emptyList()
                }

                val localMangas = if (newResults.isNotEmpty()) {
                    val converted = networkToLocalManga(newResults)
                    if (mode == "genre" && query.isNotBlank()) {
                        converted.filter { GenreFilterHelper.matchesGenre(it, query) }
                    } else {
                        converted
                    }
                } else {
                    emptyList()
                }

                val uniqueNewMangas = localMangas.filter { it.id !in loadedIds }

                if (uniqueNewMangas.isNotEmpty()) {
                    currentPage = nextPage
                    loadedIds.addAll(uniqueNewMangas.map { it.id })
                    mutableState.update {
                        it.copy(
                            mangas = (it.mangas + uniqueNewMangas).distinctBy { manga -> manga.id }.toImmutableList(),
                            isLoadingMore = false,
                            hasMore = true,
                        )
                    }
                } else {
                    mutableState.update {
                        it.copy(
                            isLoadingMore = false,
                            hasMore = false,
                        )
                    }
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                mutableState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}
