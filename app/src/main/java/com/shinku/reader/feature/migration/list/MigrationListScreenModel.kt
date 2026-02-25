package com.shinku.reader.feature.migration.list

import androidx.annotation.FloatRange
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.domain.chapter.interactor.SyncChaptersWithSource
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.toSManga
import com.shinku.reader.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.online.all.EHentai
import com.shinku.reader.exh.util.ThrottleManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import com.shinku.reader.domain.migration.usecases.MigrateMangaUseCase
import com.shinku.reader.feature.migration.list.models.MigratingManga
import com.shinku.reader.feature.migration.list.models.MigratingManga.SearchResult
import com.shinku.reader.feature.migration.list.search.SmartSourceSearchEngine
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.core.common.util.lang.withUIContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.chapter.interactor.GetChaptersByMangaId
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrationListScreenModel(
    mangaIds: Collection<Long>,
    extraSearchQuery: String?,
    private val preferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val migrateManga: MigrateMangaUseCase = Injekt.get(),
    private val getSourceHealth: GetSourceHealth = Injekt.get(),
) : StateScreenModel<MigrationListScreenModel.State>(State()) {

    private val smartSearchEngine = SmartSourceSearchEngine(extraSearchQuery)

    // SY -->
    private val throttleManager = ThrottleManager()
    // SY <--

    val items
        inline get() = state.value.items

    private val hideUnmatched = preferences.migrationHideUnmatched().get()
    private val hideWithoutUpdates = preferences.migrationHideWithoutUpdates().get()

    private val navigateBackChannel = Channel<Unit>()
    val navigateBackEvent = navigateBackChannel.receiveAsFlow()

    private var migrateJob: Job? = null

    init {
        screenModelScope.launchIO {
            val manga = mangaIds
                .map {
                    async {
                        val manga = getManga.await(it) ?: return@async null
                        val chapterInfo = getChapterInfo(it)
                        MigratingManga(
                            manga = manga,
                            chapterCount = chapterInfo.chapterCount,
                            latestChapter = chapterInfo.latestChapter,
                            source = sourceManager.getOrStub(manga.source).getNameForMangaInfo(),
                            parentContext = screenModelScope.coroutineContext,
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()
            mutableState.update { it.copy(items = manga.toImmutableList()) }
            runMigrations(manga)
        }
    }

    private suspend fun getChapterInfo(id: Long) = getChaptersByMangaId.await(id).let { chapters ->
        ChapterInfo(
            latestChapter = chapters.maxOfOrNull { it.chapterNumber },
            chapterCount = chapters.size,
        )
    }

    private suspend fun Manga.toSuccessSearchResult(): SearchResult.Success {
        val chapterInfo = getChapterInfo(id)
        val source = sourceManager.getOrStub(source).getNameForMangaInfo()
        return SearchResult.Success(
            manga = this,
            chapterCount = chapterInfo.chapterCount,
            latestChapter = chapterInfo.latestChapter,
            source = source,
        )
    }

    private suspend fun runMigrations(mangas: List<MigratingManga>) {
        // SY -->
        throttleManager.resetThrottle()
        // SY <--
        val prioritizeByChapters = preferences.migrationPrioritizeByChapters().get()
        val deepSearchMode = preferences.migrationDeepSearchMode().get()

        val sources = preferences.migrationSources().get()
            .mapNotNull { sourceManager.get(it) as? CatalogueSource }
            // Sort sources by health score (Healthiest first)
            .map { source ->
                val health = getSourceHealth.await(source.id)
                source to (health?.healthScore ?: -1)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        // Process up to 3 manga in parallel to speed things up without overwhelming the network
        val globalSemaphore = Semaphore(3)

        coroutineScope {
            mangas.map { manga ->
                async {
                    if (manga.manga.id !in state.value.mangaIds) return@async
                    if (manga.searchResult.value != SearchResult.Searching) return@async
                    if (!manga.migrationScope.isActive) return@async

                    globalSemaphore.withPermit {
                        // Small delay to prevent massive bursts
                        delay(100)
                        
                        val result = try {
                            manga.migrationScope.async {
                                if (prioritizeByChapters) {
                                    // Concurrent search within a single manga
                                    val sourceSemaphore = Semaphore(3)
                                    sources.map { source ->
                                        async innerAsync@{
                                            sourceSemaphore.withPermit {
                                                val result = searchSource(manga.manga, source, deepSearchMode)
                                                if (result == null || result.second.chapterCount == 0) return@innerAsync null
                                                result
                                            }
                                        }
                                    }
                                        .mapNotNull { it.await() }
                                        .maxByOrNull { it.second.latestChapter ?: 0.0 }
                                } else {
                                    // Sequential search within a single manga (healthiest first)
                                    sources.forEach { source ->
                                        ensureActive()
                                        val result = searchSource(manga.manga, source, deepSearchMode)
                                        if (result != null) return@async result
                                    }
                                    null
                                }
                            }.await()
                        } catch (_: CancellationException) {
                            null
                        }

                        if (result != null && result.first.thumbnailUrl == null) {
                            try {
                                val newManga = sourceManager.getOrStub(result.first.source).getMangaDetails(result.first.toSManga())
                                updateManga.awaitUpdateFromSource(result.first, newManga, true)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                            }
                        }

                        manga.searchResult.value = result?.first?.toSuccessSearchResult() ?: SearchResult.NotFound

                        if (result == null && hideUnmatched) {
                            removeManga(manga)
                        }
                        if (result != null &&
                            hideWithoutUpdates &&
                            (result.second.latestChapter ?: 0.0) <= (manga.latestChapter ?: 0.0)
                        ) {
                            removeManga(manga)
                        }

                        updateMigrationProgress()
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun searchSource(
        manga: Manga,
        source: CatalogueSource,
        deepSearchMode: Boolean,
    ): Pair<Manga, ChapterInfo>? {
        return try {
            val searchResult = if (deepSearchMode) {
                smartSearchEngine.deepSearch(source, manga.title)
            } else {
                smartSearchEngine.regularSearch(source, manga.title)
            }

            if (searchResult == null || (searchResult.url == manga.url && source.id == manga.source)) return null

            val localManga = networkToLocalManga(searchResult)
            try {
                // SY -->
                val chapters = if (source is EHentai) {
                    source.getChapterList(localManga.toSManga(), throttleManager::throttle)
                } else {
                    source.getChapterList(localManga.toSManga())
                }
                // SY <--
                syncChaptersWithSource.await(chapters, localManga, source)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
            localManga to getChapterInfo(localManga.id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun updateMigrationProgress() {
        mutableState.update { state ->
            state.copy(
                finishedCount = items.count { it.searchResult.value != SearchResult.Searching },
                migrationComplete = migrationComplete(),
            )
        }
        if (items.isEmpty()) {
            navigateBack()
        }
    }

    private fun migrationComplete() = items.all { it.searchResult.value != SearchResult.Searching } &&
        items.any { it.searchResult.value is SearchResult.Success }

    fun useMangaForMigration(current: Long, target: Long, onMissingChapters: () -> Unit) {
        val migratingManga = items.find { it.manga.id == current } ?: return
        migratingManga.searchResult.value = SearchResult.Searching
        screenModelScope.launchIO {
            val result = migratingManga.migrationScope.async {
                val manga = getManga.await(target) ?: return@async null
                try {
                    val source = sourceManager.get(manga.source)!!
                    // SY -->
                    val chapters = if (source is EHentai) {
                        source.getChapterList(manga.toSManga(), throttleManager::throttle)
                    } else {
                        source.getChapterList(manga.toSManga())
                    }
                    // SY <--
                    syncChaptersWithSource.await(chapters, manga, source)
                } catch (_: Exception) {
                    return@async null
                }
                manga
            }
                .await()

            if (result == null) {
                migratingManga.searchResult.value = SearchResult.NotFound
                withUIContext { onMissingChapters() }
                return@launchIO
            }

            try {
                val newManga = sourceManager.getOrStub(result.source).getMangaDetails(result.toSManga())
                updateManga.awaitUpdateFromSource(result, newManga, true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            migratingManga.searchResult.value = result.toSuccessSearchResult()
            updateMigrationProgress()
        }
    }

    fun migrateMangas() {
        migrateMangas(replace = true)
    }

    fun copyMangas() {
        migrateMangas(replace = false)
    }

    private fun migrateMangas(replace: Boolean) {
        migrateJob = screenModelScope.launchIO {
            mutableState.update { it.copy(dialog = Dialog.Progress(0f)) }
            val items = items
            try {
                items.forEachIndexed { index, manga ->
                    try {
                        ensureActive()
                        val target = manga.searchResult.value.let {
                            if (it is SearchResult.Success) {
                                it.manga
                            } else {
                                null
                            }
                        }
                        if (target != null) {
                            migrateManga(current = manga.manga, target = target, replace = replace)
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logcat(LogPriority.WARN, throwable = e)
                    }
                    mutableState.update {
                        it.copy(dialog = Dialog.Progress((index.toFloat() / items.size).coerceAtMost(1f)))
                    }
                }

                navigateBack()
            } finally {
                mutableState.update { it.copy(dialog = null) }
                migrateJob = null
            }
        }
    }

    fun cancelMigrate() {
        migrateJob?.cancel()
        migrateJob = null
    }

    private suspend fun navigateBack() {
        navigateBackChannel.send(Unit)
    }

    fun migrateNow(mangaId: Long, replace: Boolean) {
        screenModelScope.launchIO {
            val manga = items.find { it.manga.id == mangaId } ?: return@launchIO
            val target = (manga.searchResult.value as? SearchResult.Success)?.manga ?: return@launchIO
            migrateManga(current = manga.manga, target = target, replace = replace)

            removeManga(mangaId)
        }
    }

    fun removeManga(mangaId: Long) {
        screenModelScope.launchIO {
            val item = items.find { it.manga.id == mangaId } ?: return@launchIO
            removeManga(item)
            item.migrationScope.cancel()
            updateMigrationProgress()
        }
    }

    private fun removeManga(item: MigratingManga) {
        mutableState.update { it.copy(items = items.toPersistentList().remove(item)) }
    }

    override fun onDispose() {
        super.onDispose()
        items.forEach {
            it.migrationScope.cancel()
        }
    }

    fun showMigrateDialog(copy: Boolean) {
        mutableState.update { state ->
            state.copy(
                dialog = Dialog.Migrate(
                    copy = copy,
                    totalCount = items.size,
                    skippedCount = items.count { it.searchResult.value == SearchResult.NotFound },
                ),
            )
        }
    }

    fun showExitDialog() {
        mutableState.update {
            it.copy(dialog = Dialog.Exit)
        }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    data class ChapterInfo(
        val latestChapter: Double?,
        val chapterCount: Int,
    )

    sealed interface Dialog {
        data class Migrate(val copy: Boolean, val totalCount: Int, val skippedCount: Int) : Dialog
        data class Progress(@FloatRange(0.0, 1.0) val progress: Float) : Dialog
        data object Exit : Dialog
    }

    data class State(
        val items: ImmutableList<MigratingManga> = persistentListOf(),
        val finishedCount: Int = 0,
        val migrationComplete: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        val mangaIds: List<Long> = items.map { it.manga.id }
    }
}
