package com.shinku.reader.ui.browse.feed

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.system.logcat
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicBoolean

@androidx.compose.runtime.Immutable
data class SuggestedTitlesScreenState(
    val mangas: ImmutableList<Manga> = persistentListOf(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val hasError: Boolean = false,
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
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StateScreenModel<SuggestedTitlesScreenState>(
    SuggestedTitlesScreenState(
        mangas = initialMangas.distinctBy { it.id }.toImmutableList(),
        hasMore = mode == "genre" || mode == "for_you",
    ),
) {
    private val loading = AtomicBoolean(false)
    private val cursors = mutableMapOf<Long, SourcePageCursor>()

    init {
        if (initialMangas.isEmpty()) {
            loading.set(true)
            screenModelScope.launch(coroutineDispatcher) {
                try {
                    val restored = initialMangaIds.mapNotNull { getManga.await(it) }
                    mutableState.updateIfActive { it.copy(mangas = restored.distinctBy { it.id }.toImmutableList()) }
                } finally {
                    loading.set(false)
                }
                if (state.value.mangas.isEmpty()) loadNextPage()
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

        val healthMap = runCatchingCancellable {
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
        if (!state.value.hasMore || !loading.compareAndSet(false, true)) return
        mutableState.update { it.copy(isLoadingMore = true, hasError = false) }
        screenModelScope.launch(coroutineDispatcher) {
            try {
                val sources = getEligibleSources().let { eligible ->
                    when (mode) {
                        "genre" -> eligible.filter { query.isNotBlank() && GenreFilterHelper.buildGenreFilterList(it, query) != null }.take(3)
                        "for_you" -> eligible.filter { it.supportsLatest }.take(2)
                        else -> emptyList()
                    }
                }
                var failed = false
                // Advance through duplicate-only pages without treating them as end-of-results.
                // Bound automatic work; the footer can continue if three pages contain only duplicates.
                repeat(3) {
                    val pending = sources.filter { cursors.getOrPut(it.id) { SourcePageCursor() }.hasMore }
                    val results = pending.map { source ->
                        async(coroutineDispatcher) {
                            val cursor = cursors.getValue(source.id)
                            val result = runCatchingCancellable {
                                sharedFeedRequests.request(source.id) {
                                    when (mode) {
                                        "genre" -> source.getSearchManga(cursor.nextPage, "", GenreFilterHelper.buildGenreFilterList(source, query)!!)
                                        else -> source.getLatestUpdates(cursor.nextPage)
                                    }
                                } ?: error("Source timed out")
                            }
                            source to result
                        }
                    }.awaitAll()
                    var added = false
                    results.forEach { (source, result) ->
                        val page = result.getOrNull()
                        if (page == null) {
                            failed = true
                        } else {
                            // Only commit a cursor once local conversion has succeeded too.
                            val converted = networkToLocalManga(page.mangas.map { it.toDomainManga(source.id) })
                                .filter { mode != "genre" || GenreFilterHelper.matchesGenre(it, query) }
                            val before = state.value.mangas.size
                            mutableState.updateIfActive { state ->
                                state.copy(mangas = (state.mangas + converted).distinctBy { it.id }.toImmutableList())
                            }
                            cursors.getValue(source.id).accept(page.hasNextPage)
                            added = added || state.value.mangas.size > before
                        }
                    }
                    if (added || failed || pending.isEmpty() || cursors.values.none { it.hasMore }) {
                        mutableState.updateIfActive {
                            it.copy(isLoadingMore = false, hasMore = sources.any { source -> cursors[source.id]?.hasMore != false }, hasError = failed)
                        }
                        return@launch
                    }
                }
                mutableState.updateIfActive { it.copy(isLoadingMore = false, hasMore = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                mutableState.updateIfActive { it.copy(isLoadingMore = false, hasError = true) }
            } finally {
                loading.set(false)
            }
        }
    }
}
