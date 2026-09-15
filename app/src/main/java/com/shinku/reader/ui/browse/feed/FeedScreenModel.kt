package com.shinku.reader.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.util.fastAny
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.presentation.browse.FeedItemUI
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import com.shinku.reader.util.system.LocaleHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
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
import kotlinx.serialization.json.Json
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.manga.model.toDomainManga
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.common.util.lang.launchNonCancellable
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.source.interactor.GetRemoteManga
import com.shinku.reader.domain.source.interactor.CountFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.DeleteFeedSavedSearchById
import com.shinku.reader.domain.source.interactor.GetFeedSavedSearchGlobal
import com.shinku.reader.domain.source.interactor.GetSavedSearchBySourceId
import com.shinku.reader.domain.source.interactor.GetSavedSearchGlobalFeed
import com.shinku.reader.domain.source.interactor.InsertFeedSavedSearch
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer
import java.util.concurrent.Executors
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
) : StateScreenModel<FeedScreenState>(FeedScreenState()) {

    private val _events = Channel<Event>(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    private val coroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    var pushed: Boolean = false

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

        fetchAiRecommendations()
        loadFeaturedAndForYou()
    }

    private fun fetchAiRecommendations() {
        val apiKey = shinkuPreferences.geminiApiKey().get()
        val model = shinkuPreferences.geminiModel().get()
        if (apiKey.isBlank()) return

        screenModelScope.launchIO {
            try {
                val stats = getReadingStats.await()
                if (stats.bestGenres.isEmpty()) return@launchIO

                val recentHistory = getHistory.subscribe("").first()
                val recentTitles = recentHistory.take(10).map<HistoryWithRelations, String> { it.title }.distinct()

                val titles = geminiVibeSearch.getForYouRecommendations(
                    historyTitles = recentTitles,
                    topGenres = stats.bestGenres,
                    apiKey = apiKey,
                    model = model
                )
                
                if (titles.isNotEmpty()) {
                    val sourceId = sourcePreferences.lastUsedSource().get()
                    val source = sourceManager.get(sourceId) as? CatalogueSource ?: return@launchIO
                    
                    val recommendedManga = titles.mapNotNull<String, DomainManga> { title ->
                        try {
                            val searchResult = withContext(coroutineDispatcher) {
                                source.getSearchManga(1, title, FilterList())
                            }.mangas.firstOrNull()
                            searchResult?.toDomainManga(sourceId)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    val localManga = networkToLocalManga(recommendedManga)
                    mutableState.update { it.copy(recommendations = localManga.toImmutableList()) }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
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

    fun getFilteredCatalogueSources(): List<CatalogueSource> {
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

        return filtered.ifEmpty { allSources }
    }

    private fun loadFeaturedAndForYou() {
        screenModelScope.launchIO {
            try {
                // Instantly pre-populate from local library cache so feed is interactive immediately (0ms lag)
                fallbackToLibraryFeatured()

                val sources = getFilteredCatalogueSources().shuffled()
                if (sources.isNotEmpty()) {
                    // Fetch reading stats to personalize suggestions based on reading behaviour
                    val stats = runCatching { getReadingStats.await() }.getOrNull()
                    val bestGenres = stats?.bestGenres.orEmpty()
                    val topUserGenre = bestGenres.firstOrNull()

                    // Randomize page offset (1..2) to ensure fresh content on each refresh
                    val popularPage = (1..2).random()
                    val latestPage = (1..2).random()

                    // 1. Query popular manga for featured carousel across up to 3 randomized sources
                    val popularDeferred = sources.take(3).map { source ->
                        async(Dispatchers.IO) {
                            try {
                                source.getPopularManga(popularPage).mangas.take(8).map { it.toDomainManga(source.id) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    // 2. Query new releases (latest updates) across randomized sources supporting it
                    val latestSources = sources.filter { it.supportsLatest }.take(3).ifEmpty { sources.take(2) }
                    val latestDeferred = latestSources.map { source ->
                        async(Dispatchers.IO) {
                            try {
                                source.getLatestUpdates(latestPage).mangas.take(8).map { it.toDomainManga(source.id) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }

                    // 3. Query suggestions based on user's reading behavior (top genre) with true genre filtering
                    val behaviorDeferred = if (!topUserGenre.isNullOrBlank()) {
                        sources.take(3).map { source ->
                            async(Dispatchers.IO) {
                                try {
                                    val filterList = GenreFilterHelper.buildGenreFilterList(source, topUserGenre)
                                    if (filterList != null) {
                                        source.getSearchManga(1, "", filterList).mangas.take(8).map { it.toDomainManga(source.id) }
                                    } else {
                                        emptyList()
                                    }
                                } catch (e: Exception) {
                                    emptyList()
                                }
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
                        val featured = localPopular.take(5)
                        val featuredIds = featured.map { it.id }.toSet()

                        // Build "Titles For You": blend new releases + reading behaviour suggestions + trending
                        val forYouPool = mutableListOf<DomainManga>()
                        val maxBlend = maxOf(localBehavior.size, localLatest.size, localPopular.size)
                        for (i in 0 until maxBlend) {
                            if (i < localBehavior.size) forYouPool.add(localBehavior[i])
                            if (i < localLatest.size) forYouPool.add(localLatest[i])
                            if (i < localPopular.size) forYouPool.add(localPopular[i])
                        }

                        val forYou = forYouPool
                            .filterNot { it.id in featuredIds }
                            .distinctBy { it.id }
                            .take(15)

                        mutableState.update {
                            it.copy(
                                featuredManga = featured.toImmutableList(),
                                forYouManga = forYou.toImmutableList(),
                            )
                        }
                    } else {
                        fallbackToLibraryFeatured()
                    }

                    // Preload randomized suggestions for 5 dynamic genre highlights
                    loadGenreSections(sources, bestGenres)
                } else {
                    fallbackToLibraryFeatured()
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                fallbackToLibraryFeatured()
            } finally {
                mutableState.update { it.copy(isInitialLoadDone = true) }
            }
        }
    }

    private suspend fun fallbackToLibraryFeatured() {
        try {
            val library = getLibraryManga.await()
            val libraryManga = library.map { it.manga }
            if (libraryManga.isNotEmpty()) {
                val featured = libraryManga.shuffled().take(5)
                val forYou = if (libraryManga.size > 5) libraryManga.filter { it !in featured }.take(10) else libraryManga

                val offlineGenreMap = mutableMapOf<String, ImmutableList<DomainManga>>()
                GENRE_POOL.forEach { poolGenre ->
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
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    private suspend fun loadGenreSections(sources: List<CatalogueSource>, userBestGenres: List<String> = emptyList()) {
        // Clear previous genre sections when refreshing to provide fresh randomized recommendations
        mutableState.update { it.copy(genreSections = persistentMapOf()) }

        // Pick 5 genres dynamically: blend user's favorite genres with randomized pool
        val personalizedGenres = userBestGenres.filter { genre ->
            GENRE_POOL.any { it.equals(genre, ignoreCase = true) }
        }.shuffled().take(2)

        val remainingPool = GENRE_POOL.filterNot { poolGenre ->
            personalizedGenres.any { it.equals(poolGenre, ignoreCase = true) }
        }.shuffled()

        val selectedGenres = (personalizedGenres + remainingPool).take(5)
        val sectionsMap = mutableMapOf<String, ImmutableList<DomainManga>>()

        // Query library manga once to blend local favorites matching each genre
        val allLibraryManga = runCatching { getLibraryManga.await().map { it.manga } }.getOrDefault(emptyList())

        for (genre in selectedGenres) {
            try {
                // Find sources that natively support filtering by this genre
                val supportingSources = sources.shuffled().mapNotNull { source ->
                    val filterList = GenreFilterHelper.buildGenreFilterList(source, genre)
                    if (filterList != null) source to filterList else null
                }.take(2)

                val genrePage = (1..2).random()
                val genreResults = supportingSources.map { (source, filterList) ->
                    screenModelScope.async(Dispatchers.IO) {
                        try {
                            source.getSearchManga(genrePage, "", filterList).mangas.take(8).map { it.toDomainManga(source.id) }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()

                val localGenreManga = if (genreResults.isNotEmpty()) {
                    networkToLocalManga(genreResults)
                } else {
                    emptyList()
                }

                // Blend library titles with network titles, strictly filtering out any mismatched tags
                val libraryMatches = allLibraryManga.filter { GenreFilterHelper.matchesGenre(it, genre) }
                val validNetworkManga = localGenreManga.filter { GenreFilterHelper.matchesGenre(it, genre) }

                val combined = (libraryMatches.shuffled().take(2) + validNetworkManga)
                    .distinctBy { it.id }

                if (combined.isNotEmpty()) {
                    sectionsMap[genre] = combined.take(10).toImmutableList()
                    val currentMap = sectionsMap.toImmutableMap()
                    mutableState.update {
                        it.copy(genreSections = currentMap)
                    }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
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
                val sources = getFilteredCatalogueSources().shuffled()
                if (sources.isNotEmpty()) {
                    val supportingSources = sources.mapNotNull { source ->
                        val filterList = GenreFilterHelper.buildGenreFilterList(source, genre)
                        if (filterList != null) source to filterList else null
                    }.take(3)

                    val genreResults = supportingSources.map { (source, filterList) ->
                        async(Dispatchers.IO) {
                            try {
                                source.getSearchManga(1, "", filterList).mangas.take(10).map { it.toDomainManga(source.id) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()

                    val localGenreManga = if (genreResults.isNotEmpty()) {
                        networkToLocalManga(genreResults).filter { GenreFilterHelper.matchesGenre(it, genre) }
                    } else {
                        emptyList()
                    }

                    val finalGenreManga = if (localGenreManga.isNotEmpty()) {
                        localGenreManga
                    } else {
                        try {
                            val library = getLibraryManga.await()
                            library.map { it.manga }.filter { manga ->
                                manga.genre.orEmpty().any { it.contains(genre, ignoreCase = true) }
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    mutableState.update { it.copy(selectedGenreManga = finalGenreManga.toImmutableList(), isLoadingGenre = false) }
                } else {
                    val offlineManga = try {
                        val library = getLibraryManga.await()
                        library.map { it.manga }.filter { manga ->
                            manga.genre.orEmpty().any { it.contains(genre, ignoreCase = true) }
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    mutableState.update { it.copy(selectedGenreManga = offlineManga.toImmutableList(), isLoadingGenre = false) }
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                val offlineManga = try {
                    val library = getLibraryManga.await()
                    library.map { it.manga }.filter { manga ->
                        manga.genre.orEmpty().any { it.contains(genre, ignoreCase = true) }
                    }
                } catch (ex: Exception) {
                    emptyList()
                }
                mutableState.update { it.copy(selectedGenreManga = offlineManga.toImmutableList(), isLoadingGenre = false) }
            }
        }
    }

    fun init() {
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
                fetchAiRecommendations()
                loadFeaturedAndForYou()
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

    /**
     * Creates a catalogue search item
     */
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

    /**
     * Initiates get manga per feed.
     */
    private fun getFeed(feedSavedSearch: List<FeedItemUI>) {
        screenModelScope.launch {
            feedSavedSearch.map { itemUI ->
                async {
                    val page = try {
                        if (itemUI.source != null) {
                            withContext(coroutineDispatcher) {
                                if (itemUI.savedSearch == null) {
                                    itemUI.source.getLatestUpdates(1)
                                } else {
                                    itemUI.source.getSearchManga(
                                        1,
                                        itemUI.savedSearch.query.orEmpty(),
                                        getFilterList(itemUI.savedSearch, itemUI.source),
                                    )
                                }
                            }.mangas
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
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
    override fun onDispose() {
        super.onDispose()
        coroutineDispatcher.close()
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
