package com.shinku.reader.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.common.util.lang.launchNonCancellable
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.toDomainManga
import com.shinku.reader.domain.source.interactor.CountFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.DeleteFeedSavedSearchById
import com.shinku.reader.domain.source.interactor.GetFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.GetRemoteManga
import com.shinku.reader.domain.source.interactor.GetSavedSearchBySourceId
import com.shinku.reader.domain.source.interactor.GetSavedSearchGlobalFeed
import com.shinku.reader.domain.source.interactor.InsertFeedSavedSearch
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.presentation.browse.FeedItemUI
import com.shinku.reader.util.system.LocaleHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import com.shinku.reader.domain.manga.model.Manga as DomainManga

/**
 * Presenter of [feedTab]
 */
open class FeedScreenModel(
    val sourceManager: SourceManager = Injekt.get(),
    val sourcePreferences: SourcePreferences = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val countFeedSavedSearchGlobal: CountFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchBySourceId: GetSavedSearchBySourceId = Injekt.get(),
    private val insertFeedSavedSearch: InsertFeedSavedSearch = Injekt.get(),
    private val deleteFeedSavedSearchById: DeleteFeedSavedSearchById = Injekt.get(),
    private val getReadingStats: com.shinku.reader.domain.history.interactor.GetReadingStats = Injekt.get(),
    private val getHistory: com.shinku.reader.domain.history.interactor.GetHistory = Injekt.get(),
    private val geminiVibeSearch: com.shinku.reader.domain.source.interactor.GeminiVibeSearch = Injekt.get(),
    val shinkuPreferences: com.shinku.reader.exh.source.ShinKuPreferences = Injekt.get(),
    private val getRemoteManga: GetRemoteManga = Injekt.get(),
    private val getLibraryManga: com.shinku.reader.domain.manga.interactor.GetLibraryManga = Injekt.get(),
    private val sourceHealthRepository: SourceHealthRepository = Injekt.get(),
) : StateScreenModel<FeedScreenState>(FeedScreenState()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    // Bounded parallelism prevents Cloudflare 429 rate-limiting across extensions
    private val coroutineDispatcher = Dispatchers.IO.limitedParallelism(4)
    var pushed: Boolean = false
    var isInitialized: Boolean = false
        private set

    init {
        mutableState.update {
            it.copy(showSourceFeeds = shinkuPreferences.feedShowSourceFeeds().get())
        }

        shinkuPreferences.feedShowSourceFeeds().changes()
            .onEach { show ->
                mutableState.update { it.copy(showSourceFeeds = show) }
            }
            .launchIn(screenModelScope)

        getFeedSavedSearchGlobal.subscribe()
            .distinctUntilChanged()
            .onEach {
                sourceManager.isInitialized.first { it }
                val items = getSourcesToGetFeed(it).map { (feed, savedSearch) ->
                    createCatalogueSearchItem(
                        feed = feed,
                        savedSearch = savedSearch,
                        source = sourceManager.get(feed.source) as? CatalogueSource,
                        results = null,
                    )
                }
                mutableState.update { state ->
                    state.copy(
                        items = items.toImmutableList(),
                    )
                }
                getFeed(items)
            }
            .catch {
                _events.send(Event.FailedFetchingSources)
                mutableState.update { state ->
                    state.copy(items = state.items ?: persistentListOf(), isInitialLoadDone = true)
                }
            }
            .launchIn(screenModelScope)

        init(force = true)
    }

    private suspend fun fetchAiRecommendations(
        bestGenres: List<String>,
        healthySources: List<CatalogueSource>,
        libraryIds: Set<Long>,
    ) {
        if (!shinkuPreferences.isAiConfigured() || bestGenres.isEmpty() || healthySources.isEmpty()) return

        try {
            val apiKey = shinkuPreferences.geminiApiKey().get()
            val model = shinkuPreferences.geminiModel().get()
            val recentHistory = getHistory.subscribeRecent(10).first()
            val recentTitles = recentHistory.map<HistoryWithRelations, String> { it.title }.distinct()

            val titles = geminiVibeSearch.getForYouRecommendations(
                historyTitles = recentTitles,
                topGenres = bestGenres,
                apiKey = apiKey,
                model = model,
            )

            if (titles.isNotEmpty()) {
                val primarySource = healthySources.first()
                val recommendedManga = coroutineScope {
                    titles.take(8).map { title ->
                        async(coroutineDispatcher) {
                            withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                try {
                                    primarySource.getSearchManga(1, title, FilterList())
                                        .mangas
                                        .firstOrNull()
                                        ?.toDomainManga(primarySource.id)
                                } catch (e: Throwable) {
                                    null
                                }
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                if (recommendedManga.isNotEmpty()) {
                    val localManga = networkToLocalManga(recommendedManga)
                        .sortedBy { it.id in libraryIds }
                    mutableState.update { it.copy(recommendations = localManga.toImmutableList()) }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
        }
    }

    private val GENRE_POOL = listOf(
        "Action",
        "Fantasy",
        "Romance",
        "Comedy",
        "Adventure",
        "Sci-Fi",
        "Drama",
        "Supernatural",
        "Mystery",
        "Slice of Life",
        "Psychological",
        "Horror",
        "Isekai",
        "Martial Arts",
        "Historical",
        "Thriller",
        "Sports",
        "School Life",
        "Cultivation",
    )

    suspend fun getFilteredCatalogueSources(): List<CatalogueSource> {
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

        // Prioritize healthy, low-latency sources so broken/Cloudflare-blocked extensions don't stall Discover
        val healthMap = runCatching {
            sourceHealthRepository.subscribeAll().first().associateBy { it.sourceId }
        }.getOrDefault(emptyMap())

        val healthyCandidates = candidates.filter { source ->
            val health = healthMap[source.id]
            health == null || (!health.isDnsDead && !health.isServerDown && health.performanceScore >= 40)
        }

        val pool = healthyCandidates.ifEmpty { candidates }
        return pool.shuffled().sortedByDescending { source ->
            val score = healthMap[source.id]?.performanceScore ?: 85
            // Bucket into tiers (e.g. 80+, 50-79, <50) so sources within the same health tier stay randomized
            score / 25
        }
    }

    private suspend fun loadFeaturedAndForYou(
        bestGenres: List<String>,
        libraryManga: List<DomainManga>,
        sources: List<CatalogueSource>,
    ) {
        val libraryIds = libraryManga.map { it.id }.toSet()
        try {
            // Only seed from local library on initial cold load if feed is completely empty
            if (state.value.featuredManga.isEmpty() && state.value.forYouManga.isEmpty()) {
                fallbackToLibraryFeatured(libraryManga)
            }

            if (sources.isNotEmpty()) {
                val topUserGenre = bestGenres.firstOrNull()
                val popularPage = (1..2).random()
                val latestPage = (1..2).random()

                coroutineScope {
                    // 1. Popular manga for Featured Hero Carousel (max 2 healthy sources)
                    val popularDeferred = sources.take(2).map { source ->
                        async(coroutineDispatcher) {
                            withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                try {
                                    source.getPopularManga(popularPage).mangas.take(8).map { it.toDomainManga(source.id) }
                                } catch (e: Throwable) {
                                    emptyList()
                                }
                            }.orEmpty()
                        }
                    }

                    // 2. Latest releases across up to 2 sources supporting latest
                    val latestSources = sources.filter { it.supportsLatest }.take(2).ifEmpty { sources.take(2) }
                    val latestDeferred = latestSources.map { source ->
                        async(coroutineDispatcher) {
                            withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                try {
                                    source.getLatestUpdates(latestPage).mangas.take(8).map { it.toDomainManga(source.id) }
                                } catch (e: Throwable) {
                                    emptyList()
                                }
                            }.orEmpty()
                        }
                    }

                    // 3. Personalized top-genre suggestions across up to 2 sources
                    val behaviorDeferred = if (!topUserGenre.isNullOrBlank()) {
                        sources.take(2).map { source ->
                            async(coroutineDispatcher) {
                                withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                    try {
                                        val filterList = GenreFilterHelper.buildGenreFilterList(source, topUserGenre)
                                        if (filterList != null) {
                                            source.getSearchManga(1, "", filterList).mangas.take(8).map { it.toDomainManga(source.id) }
                                        } else {
                                            emptyList()
                                        }
                                    } catch (e: Throwable) {
                                        emptyList()
                                    }
                                }.orEmpty()
                            }
                        }
                    } else {
                        emptyList()
                    }

                    val popularResults = popularDeferred.awaitAll().flatten()
                    val latestResults = latestDeferred.awaitAll().flatten()
                    val behaviorResults = behaviorDeferred.awaitAll().flatten()

                    val localPopular = if (popularResults.isNotEmpty()) {
                        networkToLocalManga(popularResults).shuffled()
                    } else {
                        emptyList()
                    }

                    val localLatest = if (latestResults.isNotEmpty()) {
                        networkToLocalManga(latestResults)
                    } else {
                        emptyList()
                    }

                    val localBehavior = if (behaviorResults.isNotEmpty()) {
                        val mangas = networkToLocalManga(behaviorResults)
                        if (!topUserGenre.isNullOrBlank()) {
                            mangas.filter { GenreFilterHelper.matchesGenre(it, topUserGenre) }.shuffled()
                        } else {
                            mangas.shuffled()
                        }
                    } else {
                        emptyList()
                    }

                    if (localPopular.isNotEmpty() || localLatest.isNotEmpty() || localBehavior.isNotEmpty()) {
                        val featured = localPopular.ifEmpty { localLatest }.take(5)
                        val featuredIds = featured.map { it.id }.toSet()

                        val forYouPool = mutableListOf<DomainManga>()
                        val maxBlend = maxOf(localBehavior.size, localLatest.size, localPopular.size)
                        for (i in 0 until maxBlend) {
                            if (i < localBehavior.size) forYouPool.add(localBehavior[i])
                            if (i < localLatest.size) forYouPool.add(localLatest[i])
                            if (i < localPopular.size) forYouPool.add(localPopular[i])
                        }

                        // Prioritize fresh discoveries not already in the user's Library
                        val forYou = forYouPool
                            .filterNot { it.id in featuredIds }
                            .distinctBy { it.id }
                            .sortedBy { it.id in libraryIds }
                            .take(15)

                        mutableState.update {
                            it.copy(
                                featuredManga = featured.toImmutableList(),
                                forYouManga = forYou.toImmutableList(),
                            )
                        }
                    } else if (state.value.featuredManga.isEmpty()) {
                        fallbackToLibraryFeatured(libraryManga)
                    }
                }
            } else if (state.value.featuredManga.isEmpty()) {
                fallbackToLibraryFeatured(libraryManga)
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
            if (state.value.featuredManga.isEmpty()) {
                fallbackToLibraryFeatured(libraryManga)
            }
        } finally {
            mutableState.update { it.copy(isInitialLoadDone = true) }
        }
    }

    private fun fallbackToLibraryFeatured(libraryManga: List<DomainManga>) {
        if (libraryManga.isEmpty()) return
        val featured = libraryManga.shuffled().take(5)
        val forYou = if (libraryManga.size > 5) libraryManga.filter { it !in featured }.take(10) else libraryManga

        val offlineGenreMap = mutableMapOf<String, ImmutableList<DomainManga>>()
        GENRE_POOL.take(4).forEach { poolGenre ->
            val matching = libraryManga.filter { manga ->
                manga.genre.orEmpty().any { it.contains(poolGenre, ignoreCase = true) }
            }
            if (matching.isNotEmpty()) {
                offlineGenreMap[poolGenre] = matching.distinctBy { it.id }.take(10).toImmutableList()
            }
        }

        mutableState.update {
            it.copy(
                featuredManga = featured.toImmutableList(),
                forYouManga = forYou.toImmutableList(),
                genreSections = offlineGenreMap.toImmutableMap(),
            )
        }
    }

    private suspend fun loadGenreSections(
        sources: List<CatalogueSource>,
        userBestGenres: List<String>,
        allLibraryManga: List<DomainManga>,
    ) {
        if (sources.isEmpty()) return

        // Curate 3 genre rows per refresh (2 from user's top genres + 1 discovery genre) to keep network fast
        val personalizedGenres = userBestGenres.filter { genre ->
            GENRE_POOL.any { it.equals(genre, ignoreCase = true) }
        }.shuffled().take(2)

        val remainingPool = GENRE_POOL.filterNot { poolGenre ->
            personalizedGenres.any { it.equals(poolGenre, ignoreCase = true) }
        }.shuffled()

        val selectedGenres = (personalizedGenres + remainingPool).take(3)
        val libraryIds = allLibraryManga.map { it.id }.toSet()

        val results = coroutineScope {
            selectedGenres.map { genre ->
                async(coroutineDispatcher) {
                    try {
                        val supportingSources = sources.shuffled().mapNotNull { source ->
                            val filterList = GenreFilterHelper.buildGenreFilterList(source, genre)
                            if (filterList != null) source to filterList else null
                        }.take(2)

                        val genrePage = (1..2).random()
                        val genreResults = supportingSources.map { (source, filterList) ->
                            async(coroutineDispatcher) {
                                withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                    try {
                                        source.getSearchManga(genrePage, "", filterList).mangas.take(8).map { it.toDomainManga(source.id) }
                                    } catch (e: Throwable) {
                                        emptyList()
                                    }
                                }.orEmpty()
                            }
                        }.awaitAll().flatten()

                        val localGenreManga = if (genreResults.isNotEmpty()) {
                            networkToLocalManga(genreResults)
                        } else {
                            emptyList()
                        }

                        val validNetworkManga = localGenreManga
                            .filter { GenreFilterHelper.matchesGenre(it, genre) }
                            .sortedBy { it.id in libraryIds }
                        val libraryMatches = allLibraryManga.filter { GenreFilterHelper.matchesGenre(it, genre) }

                        val combined = (validNetworkManga + libraryMatches.shuffled().take(2))
                            .distinctBy { it.id }

                        if (combined.isNotEmpty()) {
                            genre to combined.take(10).toImmutableList()
                        } else {
                            null
                        }
                    } catch (e: Throwable) {
                        logcat(LogPriority.ERROR, e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (results.isNotEmpty()) {
            mutableState.update {
                it.copy(genreSections = results.toMap().toImmutableMap())
            }
        }
    }

    fun selectGenre(genre: String) {
        if (genre.isBlank() || state.value.selectedGenre.equals(genre, ignoreCase = true)) {
            mutableState.update { it.copy(selectedGenre = null, selectedGenreManga = null, isLoadingGenre = false) }
            return
        }

        mutableState.update { it.copy(selectedGenre = genre, isLoadingGenre = true) }
        screenModelScope.launchIO {
            try {
                val sources = getFilteredCatalogueSources()
                if (sources.isNotEmpty()) {
                    val supportingSources = sources.mapNotNull { source ->
                        val filterList = GenreFilterHelper.buildGenreFilterList(source, genre)
                        if (filterList != null) source to filterList else null
                    }.take(3)

                    val genreResults = coroutineScope {
                        supportingSources.map { (source, filterList) ->
                            async(coroutineDispatcher) {
                                withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                    try {
                                        source.getSearchManga(1, "", filterList).mangas.take(10).map { it.toDomainManga(source.id) }
                                    } catch (e: Throwable) {
                                        emptyList()
                                    }
                                }.orEmpty()
                            }
                        }.awaitAll().flatten()
                    }

                    val localGenreManga = if (genreResults.isNotEmpty()) {
                        networkToLocalManga(genreResults).filter { GenreFilterHelper.matchesGenre(it, genre) }
                    } else {
                        emptyList()
                    }

                    val finalGenreManga = localGenreManga.ifEmpty {
                        runCatching {
                            getLibraryManga.await().map { it.manga }.filter { manga ->
                                manga.genre.orEmpty().any { it.contains(genre, ignoreCase = true) }
                            }
                        }.getOrDefault(emptyList())
                    }
                    mutableState.update { it.copy(selectedGenreManga = finalGenreManga.toImmutableList(), isLoadingGenre = false) }
                } else {
                    val offlineManga = runCatching {
                        getLibraryManga.await().map { it.manga }.filter { manga ->
                            manga.genre.orEmpty().any { it.contains(genre, ignoreCase = true) }
                        }
                    }.getOrDefault(emptyList())
                    mutableState.update { it.copy(selectedGenreManga = offlineManga.toImmutableList(), isLoadingGenre = false) }
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                mutableState.update { it.copy(selectedGenreManga = persistentListOf(), isLoadingGenre = false) }
            }
        }
    }

    fun init(force: Boolean = false) {
        if (!force && isInitialized) return
        isInitialized = true
        pushed = false
        mutableState.update {
            it.copy(
                isRefreshing = true,
                selectedGenre = null,
                selectedGenreManga = null,
                isLoadingGenre = false,
                showSourceFeeds = shinkuPreferences.feedShowSourceFeeds().get(),
            )
        }
        screenModelScope.launchIO {
            try {
                sourceManager.isInitialized.first { it }
                // Query DB once per refresh cycle and share across all sections
                val libraryManga = runCatching { getLibraryManga.await().map { it.manga } }.getOrDefault(emptyList())
                val stats = runCatching { getReadingStats.await() }.getOrNull()
                val bestGenres = stats?.bestGenres.orEmpty()
                val sources = getFilteredCatalogueSources()
                val libraryIds = libraryManga.map { it.id }.toSet()

                // 1. Load Hero Carousel + Titles For You first so top of screen is ready fast
                loadFeaturedAndForYou(
                    bestGenres = bestGenres,
                    libraryManga = libraryManga,
                    sources = sources,
                )

                // Release pull-to-refresh spinner as soon as Hero + Titles For You are ready
                mutableState.update { it.copy(isRefreshing = false) }

                // 2. Load Genre rows, AI Picks, and Custom Source Feeds in background
                launchIO {
                    loadGenreSections(
                        sources = sources,
                        userBestGenres = bestGenres,
                        allLibraryManga = libraryManga,
                    )
                }
                launchIO {
                    fetchAiRecommendations(
                        bestGenres = bestGenres,
                        healthySources = sources,
                        libraryIds = libraryIds,
                    )
                }

                val newItems = state.value.items?.map { it.copy(results = null) }
                if (newItems != null) {
                    mutableState.update { state ->
                        state.copy(
                            items = newItems.toImmutableList(),
                        )
                    }
                    getFeed(newItems)
                }
            } finally {
                mutableState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun openAddDialog() {
        screenModelScope.launchIO {
            if (hasTooManyFeeds()) {
                _events.send(Event.TooManyFeeds)
                return@launchIO
            }
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.AddFeed(getEnabledSources()),
                )
            }
        }
    }

    fun openAddSearchDialog(source: CatalogueSource) {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.AddFeedSearch(
                        source,
                        (
                            (if (source.supportsLatest) persistentListOf(null) else persistentListOf()) +
                                getSourceSavedSearches(source.id)
                            ).toImmutableList(),
                    ),
                )
            }
        }
    }

    fun openDeleteDialog(feed: FeedSavedSearch) {
        screenModelScope.launchIO {
            mutableState.update { state ->
                state.copy(
                    dialog = Dialog.DeleteFeed(feed),
                )
            }
        }
    }

    private suspend fun hasTooManyFeeds(): Boolean {
        return countFeedSavedSearchGlobal.await() > 10
    }

    fun getEnabledSources(): ImmutableList<CatalogueSource> {
        val languages = sourcePreferences.enabledLanguages().get()
        val pinnedSources = sourcePreferences.pinnedSources().get()
        val disabledSources = sourcePreferences.disabledSources().get()
            .mapNotNull { it.toLongOrNull() }

        val list = sourceManager.getVisibleCatalogueSources()
            .filter { it.lang in languages }
            .filterNot { it.id in disabledSources }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { "(${it.lang}) ${it.name}" })

        return list.sortedBy { it.id.toString() !in pinnedSources }.toImmutableList()
    }

    suspend fun getSourceSavedSearches(sourceId: Long): ImmutableList<SavedSearch> {
        return getSavedSearchBySourceId.await(sourceId).toImmutableList()
    }

    fun createFeed(source: CatalogueSource, savedSearch: SavedSearch?) {
        screenModelScope.launchNonCancellable {
            insertFeedSavedSearch.await(
                FeedSavedSearch(
                    id = -1,
                    source = source.id,
                    savedSearch = savedSearch?.id,
                    global = true,
                ),
            )
        }
    }

    fun deleteFeed(feed: FeedSavedSearch) {
        screenModelScope.launchNonCancellable {
            deleteFeedSavedSearchById.await(feed.id)
        }
    }

    private suspend fun getSourcesToGetFeed(feedSavedSearch: List<FeedSavedSearch>): List<Pair<FeedSavedSearch, SavedSearch?>> {
        val savedSearches = getSavedSearchGlobalFeed.await()
            .associateBy { it.id }
        return feedSavedSearch
            .map { it to savedSearches[it.savedSearch] }
    }

    private fun createCatalogueSearchItem(
        feed: FeedSavedSearch,
        savedSearch: SavedSearch?,
        source: CatalogueSource?,
        results: List<DomainManga>?,
    ): FeedItemUI {
        return FeedItemUI(
            feed,
            savedSearch,
            source,
            savedSearch?.name ?: (source?.name ?: feed.source.toString()),
            if (savedSearch != null) {
                source?.name ?: feed.source.toString()
            } else {
                LocaleHelper.getLocalizedDisplayName(source?.lang)
            },
            results?.toImmutableList(),
        )
    }

    private fun getFeed(feedSavedSearch: List<FeedItemUI>) {
        screenModelScope.launch {
            feedSavedSearch.map { itemUI ->
                async {
                    val page = try {
                        if (itemUI.source != null) {
                            withContext(coroutineDispatcher) {
                                withTimeoutOrNull(SOURCE_CALL_TIMEOUT_MS) {
                                    if (itemUI.savedSearch == null) {
                                        itemUI.source.getLatestUpdates(1)
                                    } else {
                                        itemUI.source.getSearchManga(
                                            1,
                                            itemUI.savedSearch.query.orEmpty(),
                                            getFilterList(itemUI.savedSearch, itemUI.source),
                                        )
                                    }
                                }?.mangas.orEmpty()
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Throwable) {
                        emptyList()
                    }

                    val result = withIOContext {
                        itemUI.copy(
                            results = networkToLocalManga(page.take(20).map { it.toDomainManga(itemUI.source!!.id) }).toImmutableList(),
                        )
                    }

                    mutableState.update { state ->
                        state.copy(
                            items = state.items?.map { if (it.feed.id == result.feed.id) result else it }?.toImmutableList(),
                        )
                    }
                }
            }.awaitAll()
        }
    }

    private val filterSerializer = FilterSerializer()

    private fun getFilterList(savedSearch: SavedSearch, source: CatalogueSource): FilterList {
        val filters = savedSearch.filtersJson ?: return FilterList()
        return runCatching {
            val originalFilters = source.getFilterList()
            filterSerializer.deserialize(
                filters = originalFilters,
                json = Json.decodeFromString(filters),
            )
            originalFilters
        }.getOrElse { FilterList() }
    }

    @Composable
    fun getManga(initialManga: DomainManga): State<DomainManga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .collectLatest { manga ->
                    if (manga == null) return@collectLatest
                    value = manga
                }
        }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun openFilterDialog() {
        mutableState.update { it.copy(dialog = Dialog.FeedFilter) }
    }

    sealed class Dialog {
        data object FeedFilter : Dialog()
        data class AddFeed(val options: ImmutableList<CatalogueSource>) : Dialog()
        data class AddFeedSearch(val source: CatalogueSource, val options: ImmutableList<SavedSearch?>) : Dialog()
        data class DeleteFeed(val feed: FeedSavedSearch) : Dialog()
    }

    sealed class Event {
        data object FailedFetchingSources : Event()
        data object TooManyFeeds : Event()
    }

    companion object {
        private const val SOURCE_CALL_TIMEOUT_MS = 8_000L
    }
}

@androidx.compose.runtime.Immutable
data class FeedScreenState(
    val dialog: FeedScreenModel.Dialog? = null,
    val items: kotlinx.collections.immutable.ImmutableList<FeedItemUI>? = null,
    val recommendations: kotlinx.collections.immutable.ImmutableList<DomainManga>? = null,
    val featuredManga: kotlinx.collections.immutable.ImmutableList<DomainManga> = kotlinx.collections.immutable.persistentListOf(),
    val forYouManga: kotlinx.collections.immutable.ImmutableList<DomainManga> = kotlinx.collections.immutable.persistentListOf(),
    val genreSections: kotlinx.collections.immutable.ImmutableMap<String, kotlinx.collections.immutable.ImmutableList<DomainManga>> = kotlinx.collections.immutable.persistentMapOf(),
    val selectedGenre: String? = null,
    val selectedGenreManga: kotlinx.collections.immutable.ImmutableList<DomainManga>? = null,
    val isLoadingGenre: Boolean = false,
    val isRefreshing: Boolean = false,
    val showSourceFeeds: Boolean = true,
    val isInitialLoadDone: Boolean = false,
) {
    val hasContent: Boolean
        get() = featuredManga.isNotEmpty() ||
            forYouManga.isNotEmpty() ||
            genreSections.isNotEmpty() ||
            selectedGenreManga != null ||
            !recommendations.isNullOrEmpty() ||
            (showSourceFeeds && !items.isNullOrEmpty())

    val isLoading: Boolean
        get() = !isInitialLoadDone && !hasContent

    val isLoadingItems: Boolean
        get() = items?.fastAny { it.results == null } != false
}
