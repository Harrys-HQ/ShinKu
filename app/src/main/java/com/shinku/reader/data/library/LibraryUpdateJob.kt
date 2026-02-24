package com.shinku.reader.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shinku.reader.domain.chapter.interactor.SyncChaptersWithSource
import com.shinku.reader.domain.manga.interactor.UpdateManga
import com.shinku.reader.domain.manga.model.copyFrom
import com.shinku.reader.domain.manga.model.toSManga
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.domain.track.model.toDbTrack
import com.shinku.reader.domain.track.model.toDomainTrack
import com.shinku.reader.data.cache.CoverCache
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.data.sync.SyncDataJob
import com.shinku.reader.data.track.TrackStatus
import com.shinku.reader.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.all.MergedSource
import com.shinku.reader.util.prepUpdateCover
import com.shinku.reader.util.storage.getUriCompat
import com.shinku.reader.util.system.createFileInCacheDir
import com.shinku.reader.util.system.isConnectedToWifi
import com.shinku.reader.util.system.isRunning
import com.shinku.reader.util.system.setForegroundSafely
import com.shinku.reader.util.system.workManager
import com.shinku.reader.exh.log.xLogE
import com.shinku.reader.exh.md.utils.FollowStatus
import com.shinku.reader.exh.md.utils.MdUtil
import com.shinku.reader.exh.source.LIBRARY_UPDATE_EXCLUDED_SOURCES
import com.shinku.reader.exh.source.MERGED_SOURCE_ID
import com.shinku.reader.exh.source.mangaDexSourceIds
import com.shinku.reader.exh.util.nullIfBlank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import com.shinku.reader.domain.chapter.interactor.FilterChaptersForDownload
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.preference.getAndSet
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.chapter.model.NoChaptersException
import com.shinku.reader.domain.library.model.GroupLibraryMode
import com.shinku.reader.domain.library.model.LibraryGroup
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import com.shinku.reader.domain.manga.interactor.FetchInterval
import com.shinku.reader.domain.manga.interactor.GetFavorites
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.manga.interactor.GetManga
import com.shinku.reader.domain.manga.interactor.GetMergedMangaForDownloading
import com.shinku.reader.domain.manga.interactor.InsertFlatMetadata
import com.shinku.reader.domain.manga.interactor.NetworkToLocalManga
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaUpdate
import com.shinku.reader.domain.manga.model.toMangaUpdate
import com.shinku.reader.domain.source.model.SourceNotInstalledException
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.domain.track.interactor.GetTracks
import com.shinku.reader.domain.track.interactor.InsertTrack
import com.shinku.reader.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class LibraryUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: SourceManager = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val downloadManager: DownloadManager = Injekt.get()
    private val coverCache: CoverCache = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val getManga: GetManga = Injekt.get()
    private val updateManga: UpdateManga = Injekt.get()
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get()
    private val fetchInterval: FetchInterval = Injekt.get()
    private val filterChaptersForDownload: FilterChaptersForDownload = Injekt.get()

    // SY -->
    private val getFavorites: GetFavorites = Injekt.get()
    private val insertFlatMetadata: InsertFlatMetadata = Injekt.get()
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get()
    private val getMergedMangaForDownloading: GetMergedMangaForDownloading = Injekt.get()
    private val getTracks: GetTracks = Injekt.get()
    private val insertTrack: InsertTrack = Injekt.get()
    private val trackerManager: TrackerManager = Injekt.get()
    private val mdList = trackerManager.mdList
    // SY <--

    private val notifier = LibraryUpdateNotifier(context)

    private var mangaToUpdate: List<LibraryManga> = mutableListOf()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val preferences = Injekt.get<LibraryPreferences>()
                val restrictions = preferences.autoUpdateDeviceRestrictions().get()
                if ((DEVICE_ONLY_ON_WIFI in restrictions) && !context.isConnectedToWifi()) {
                    return Result.retry()
                }
            }

            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                return Result.retry()
            }
        }

        setForegroundSafely()

        val target = inputData.getString(KEY_TARGET)?.let { Target.valueOf(it) }
            ?: Target.CHAPTERS

        // If this is a chapter update, set the last update time to now
        if (target == Target.CHAPTERS) {
            libraryPreferences.lastUpdatedTimestamp().set(Instant.now().toEpochMilli())
        }

        val categoryId = inputData.getLong(KEY_CATEGORY, -1L)
        // SY -->
        val group = inputData.getInt(KEY_GROUP, LibraryGroup.BY_DEFAULT)
        val groupExtra = inputData.getString(KEY_GROUP_EXTRA)
        // SY <--

        if (target == Target.CHAPTERS || target == Target.COVERS) {
            addMangaToQueue(categoryId, group, groupExtra, target)
        }

        return withIOContext {
            try {
                when (target) {
                    Target.CHAPTERS -> updateChapterList()
                    Target.COVERS -> updateCovers()
                    // SY -->
                    Target.SYNC_FOLLOWS -> syncFollows()
                    Target.PUSH_FAVORITES -> pushFavorites()
                    // SY <--
                }
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notifier = LibraryUpdateNotifier(context)
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    /**
     * Adds list of manga to be updated.
     *
     * @param categoryId the ID of the category to update, or -1 if no category specified.
     */
    private suspend fun addMangaToQueue(categoryId: Long, group: Int, groupExtra: String?, target: Target) {
        val libraryManga = getLibraryManga.await()
        // SY -->
        val groupLibraryUpdateType = libraryPreferences.groupLibraryUpdateType().get()
        // SY <--

        var listToUpdate = if (categoryId != -1L) {
            libraryManga.filter { categoryId in it.categories }
            // SY -->
        } else if (
            group == LibraryGroup.BY_DEFAULT ||
            groupLibraryUpdateType == GroupLibraryMode.GLOBAL ||
            (groupLibraryUpdateType == GroupLibraryMode.ALL_BUT_UNGROUPED && group == LibraryGroup.UNGROUPED)
        ) {
            // SY <--
            val includedCategories = libraryPreferences.updateCategories().get().map { it.toLong() }.toSet()
            val excludedCategories = libraryPreferences.updateCategoriesExclude().get().map { it.toLong() }.toSet()

            libraryManga.filter {
                val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
                val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
                included && !excluded
            }
            // SY -->
        } else {
            when (group) {
                LibraryGroup.BY_TRACK_STATUS -> {
                    val trackingExtra = groupExtra?.toIntOrNull() ?: -1
                    val tracks = getTracks.await().groupBy { it.mangaId }

                    libraryManga.filter { (manga) ->
                        val status = tracks[manga.id]?.firstNotNullOfOrNull { track ->
                            TrackStatus.parseTrackerStatus(trackerManager, track.trackerId, track.status)
                        } ?: TrackStatus.OTHER
                        status.int == trackingExtra
                    }
                }

                LibraryGroup.BY_SOURCE -> {
                    val sourceExtra = groupExtra?.nullIfBlank()?.toIntOrNull()
                    val source = libraryManga.map { it.manga.source }
                        .distinct()
                        .sorted()
                        .getOrNull(sourceExtra ?: -1)

                    if (source != null) libraryManga.filter { it.manga.source == source } else emptyList()
                }

                LibraryGroup.BY_STATUS -> {
                    val statusExtra = groupExtra?.toLongOrNull() ?: -1
                    libraryManga.filter {
                        it.manga.status == statusExtra
                    }
                }

                LibraryGroup.UNGROUPED -> libraryManga
                else -> libraryManga
            }
            // SY <--
        }

        // SY -->
        if (target == Target.CHAPTERS || target == Target.COVERS) {
            listToUpdate = listToUpdate.filterNot { it.manga.source in LIBRARY_UPDATE_EXCLUDED_SOURCES }
        }
        // SY <--

        val isAuto = tags.contains(WORK_NAME_AUTO)
        val restrictions = if (isAuto) {
            libraryPreferences.autoUpdateMangaRestrictions().get()
        } else {
            emptySet()
        }
        val skippedUpdates = mutableListOf<Pair<Manga, String?>>()
        val (_, fetchWindowUpperBound) = fetchInterval.getWindow(ZonedDateTime.now())

        mangaToUpdate = listToUpdate
            // SY -->
            .distinctBy { it.manga.id }
            // SY <--
            .filter {
                when {
                    it.manga.updateStrategy == UpdateStrategy.ONLY_FETCH_ONCE && it.totalChapters > 0L -> {
                        skippedUpdates.add(
                            it.manga to context.stringResource(MR.strings.skipped_reason_not_always_update),
                        )
                        false
                    }

                    MANGA_NON_COMPLETED in restrictions && it.manga.status.toInt() == SManga.COMPLETED -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_completed))
                        false
                    }

                    MANGA_HAS_UNREAD in restrictions && it.unreadCount != 0L -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_not_caught_up))
                        false
                    }

                    MANGA_NON_READ in restrictions && it.totalChapters > 0L && !it.hasStarted -> {
                        skippedUpdates.add(it.manga to context.stringResource(MR.strings.skipped_reason_not_started))
                        false
                    }

                    MANGA_OUTSIDE_RELEASE_PERIOD in restrictions && it.manga.nextUpdate > fetchWindowUpperBound -> {
                        skippedUpdates.add(
                            it.manga to context.stringResource(MR.strings.skipped_reason_not_in_release_period),
                        )
                        false
                    }

                    else -> true
                }
            }
            .sortedBy { it.manga.title }

        notifier.showQueueSizeWarningNotificationIfNeeded(mangaToUpdate)

        if (skippedUpdates.isNotEmpty()) {
            // TODO: surface skipped reasons to user?
            logcat {
                skippedUpdates
                    .groupBy { it.second }
                    .map { (reason, entries) -> "$reason: [${entries.map { it.first.title }.sorted().joinToString()}]" }
                    .joinToString()
            }
        }
    }

    /**
     * Method that updates manga in [mangaToUpdate]. It's called in a background thread, so it's safe
     * to do heavy operations or network calls here.
     * For each manga it calls [updateManga] and updates the notification showing the current
     * progress.
     *
     * @return an observable delivering the progress of each update.
     */
    private suspend fun updateChapterList() {
        val speed = libraryPreferences.libraryUpdateSpeed().get()
        val (maxConcurrentSources, maxMangaPerSource) = when (speed) {
            1 -> 15 to 3 // Boost (upgraded from 10 to 2)
            2 -> 40 to 10 // Extreme (upgraded from 20 to 5)
            else -> 5 to 1 // Standard (default)
        }

        val semaphore = Semaphore(maxConcurrentSources)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()
        val newUpdates = CopyOnWriteArrayList<Pair<Manga, Array<Chapter>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Manga, String?>>()
        val hasDownloads = AtomicBoolean(false)
        // SY -->
        val mdlistLogged = mdList.isLoggedIn
        // SY <--

        val fetchWindow = fetchInterval.getWindow(ZonedDateTime.now())

        coroutineScope {
            mangaToUpdate.groupBy { it.manga.source }
                .values
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            if (
                                mdlistLogged &&
                                mangaInSource.firstOrNull()
                                    ?.let { it.manga.source in mangaDexSourceIds } == true
                            ) {
                                launch {
                                    mangaInSource.forEach { (manga) ->
                                        try {
                                            val tracks = getTracks.await(manga.id)
                                            if (tracks.isEmpty() ||
                                                tracks.none { it.trackerId == TrackerManager.MDLIST }
                                            ) {
                                                val track = mdList.createInitialTracker(manga)
                                                insertTrack.await(mdList.refresh(track).toDomainTrack(false)!!)
                                            }
                                        } catch (e: Exception) {
                                            if (e is CancellationException) throw e
                                            xLogE("Error adding initial track for ${manga.title}", e)
                                        }
                                    }
                                }
                            }

                            val mangaSemaphore = Semaphore(maxMangaPerSource)
                            coroutineScope {
                                mangaInSource.forEach { libraryManga ->
                                    val manga = libraryManga.manga
                                    ensureActive()

                                    // Don't continue to update if manga is not in library
                                    if (getManga.await(manga.id)?.favorite != true) {
                                        return@forEach
                                    }

                                    launch {
                                        mangaSemaphore.withPermit {
                                            // Source-specific polite delay (only if concurrency is high)
                                            // Spread out requests slightly to avoid burst detection
                                            if (speed > 0) {
                                                kotlinx.coroutines.delay(50)
                                            }

                                            withUpdateNotification(
                                                currentlyUpdatingManga,
                                                progressCount,
                                                manga,
                                            ) {
                                                try {
                                                    val newChapters = updateManga(manga, fetchWindow)
                                                        .sortedByDescending { it.sourceOrder }

                                                    if (newChapters.isNotEmpty()) {
                                                        val chaptersToDownload =
                                                            filterChaptersForDownload.await(manga, newChapters)

                                                        if (chaptersToDownload.isNotEmpty()) {
                                                            downloadChapters(manga, chaptersToDownload)
                                                            hasDownloads.store(true)
                                                        }

                                                        libraryPreferences.newUpdatesCount()
                                                            .getAndSet { it + newChapters.size }

                                                        // Convert to the manga that contains new chapters
                                                        newUpdates.add(manga to newChapters.toTypedArray())
                                                    }
                                                    updateManga.await(MangaUpdate(manga.id, lastUpdateError = false))
                                                } catch (e: Throwable) {
                                                    updateManga.await(MangaUpdate(manga.id, lastUpdateError = true))
                                                    val errorMessage = when (e) {
                                                        is NoChaptersException -> context.stringResource(
                                                            MR.strings.no_chapters_error,
                                                        )
                                                        // failedUpdates will already have the source, don't need to copy it into the message
                                                        is SourceNotInstalledException -> context.stringResource(
                                                            MR.strings.loader_not_implemented_error,
                                                        )

                                                        else -> e.message
                                                    }
                                                    failedUpdates.add(manga to errorMessage)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()

        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
            if (hasDownloads.load()) {
                downloadManager.startDownloads()
            }
        }

        if (failedUpdates.isNotEmpty()) {
            val failedIds = failedUpdates.map { it.first.id.toString() }.toSet()
            libraryPreferences.failedUpdatesMangaIds().set(failedIds)

            val errorFile = writeErrorFile(failedUpdates)
            notifier.showUpdateErrorNotification(
                failedUpdates.size,
                errorFile.getUriCompat(context),
            )
        } else {
            libraryPreferences.failedUpdatesMangaIds().delete()
        }
    }

    private fun downloadChapters(manga: Manga, chapters: List<Chapter>) {
        // We don't want to start downloading while the library is updating, because websites
        // may don't like it and they could ban the user.
        // SY -->
        if (manga.source == MERGED_SOURCE_ID) {
            val downloadingManga = runBlocking { getMergedMangaForDownloading.await(manga.id) }
                .associateBy { it.id }
            chapters.groupBy { it.mangaId }
                .forEach {
                    downloadManager.downloadChapters(
                        downloadingManga[it.key] ?: return@forEach,
                        it.value,
                        false,
                    )
                }

            return
        }
        // SY <--
        downloadManager.downloadChapters(manga, chapters, false)
    }

    /**
     * Updates the chapters for the given manga and adds them to the database.
     *
     * @param manga the manga to update.
     * @return a pair of the inserted and removed chapters.
     */
    private suspend fun updateManga(manga: Manga, fetchWindow: Pair<Long, Long>): List<Chapter> {
        val source = sourceManager.getOrStub(manga.source)

        // Update manga metadata if needed
        if (libraryPreferences.autoUpdateMetadata().get()) {
            val lastUpdate = manga.lastMetadataUpdate
            val now = Instant.now().toEpochMilli()
            // Stagger metadata updates: 7 days + a random offset based on manga ID to prevent spikes
            val staggerOffset = (manga.id % 24).toLong() * 60 * 60 * 1000L
            if (now - lastUpdate > (7 * 24 * 60 * 60 * 1000L) + staggerOffset) {
                try {
                    val networkManga = source.getMangaDetails(manga.toSManga())
                    updateManga.awaitUpdateFromSource(manga, networkManga, manualFetch = false, coverCache)
                } catch (e: Exception) {
                    // Fail silently for metadata to prioritize chapters
                    logcat(LogPriority.DEBUG) { "Failed to update metadata for ${manga.title}" }
                }
            }
        }

        // Get manga from database to account for if it was removed during the update and
        // to get latest data so it doesn't get overwritten later on
        val dbManga = getManga.await(manga.id)?.takeIf { it.favorite } ?: return emptyList()

        if (source is MergedSource) {
            return source.fetchChaptersAndSync(dbManga, false)
        }

        val chapters = source.getChapterList(dbManga.toSManga())

        return syncChaptersWithSource.await(chapters, dbManga, source, false, fetchWindow)
    }

    private suspend fun updateCovers() {
        val speed = libraryPreferences.libraryUpdateSpeed().get()
        val (maxConcurrentSources, maxMangaPerSource) = when (speed) {
            1 -> 10 to 2 // Boost
            2 -> Int.MAX_VALUE to Int.MAX_VALUE // Extreme
            else -> 5 to 1 // Standard (default)
        }

        val semaphore = Semaphore(maxConcurrentSources)
        val progressCount = AtomicInt(0)
        val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()

        coroutineScope {
            mangaToUpdate.groupBy { it.manga.source }
                .values
                .map { mangaInSource ->
                    async {
                        semaphore.withPermit {
                            val mangaSemaphore = Semaphore(maxMangaPerSource)
                            coroutineScope {
                                mangaInSource.forEach { libraryManga ->
                                    val manga = libraryManga.manga
                                    ensureActive()

                                    launch {
                                        mangaSemaphore.withPermit {
                                            withUpdateNotification(
                                                currentlyUpdatingManga,
                                                progressCount,
                                                manga,
                                            ) {
                                                val source =
                                                    sourceManager.get(manga.source) ?: return@withUpdateNotification
                                                try {
                                                    val networkManga = source.getMangaDetails(manga.toSManga())
                                                    val updatedManga = manga.prepUpdateCover(
                                                        coverCache,
                                                        networkManga,
                                                        true,
                                                    )
                                                        .copyFrom(networkManga)
                                                    try {
                                                        updateManga.await(updatedManga.toMangaUpdate())
                                                    } catch (_: Exception) {
                                                        logcat(LogPriority.ERROR) { "Manga doesn't exist anymore" }
                                                    }
                                                } catch (e: Throwable) {
                                                    // Ignore errors and continue
                                                    logcat(LogPriority.ERROR, e)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
    }

    // SY -->

    /**
     * filter all follows from Mangadex and only add reading or rereading manga to library
     */
    private suspend fun syncFollows() = coroutineScope {
        val preferences = Injekt.get<SourcePreferences>()
        var count = 0
        val mangaDex = MdUtil.getEnabledMangaDex(preferences, sourceManager = sourceManager)
            ?: return@coroutineScope
        val syncFollowStatusInts = preferences.mangadexSyncToLibraryIndexes().get().map { it.toInt() }

        val size: Int
        mangaDex.fetchAllFollows()
            .filter { (_, metadata) ->
                syncFollowStatusInts.contains(metadata.followStatus)
            }
            .also { size = it.size }
            .forEach { (networkManga, metadata) ->
                ensureActive()

                count++
                notifier.showProgressNotification(
                    listOf(Manga.create().copy(ogTitle = networkManga.title)),
                    count,
                    size,
                )

                var dbManga = getManga.await(networkManga.url, mangaDex.id)

                if (dbManga == null) {
                    dbManga = networkToLocalManga(
                        Manga.create().copy(
                            url = networkManga.url,
                            ogTitle = networkManga.title,
                            source = mangaDex.id,
                            favorite = true,
                            dateAdded = System.currentTimeMillis(),
                        ),
                    )
                } else if (!dbManga.favorite) {
                    updateManga.awaitUpdateFavorite(dbManga.id, true)
                }

                updateManga.awaitUpdateFromSource(dbManga, networkManga, true)
                metadata.mangaId = dbManga.id
                insertFlatMetadata.await(metadata)
            }

        notifier.cancelProgressNotification()
    }

    /**
     * Method that updates the all mangas which are not tracked as "reading" on mangadex
     */
    private suspend fun pushFavorites() = coroutineScope {
        var count = 0
        val listManga = getFavorites.await().filter { it.source in mangaDexSourceIds }

        // filter all follows from Mangadex and only add reading or rereading manga to library
        if (mdList.isLoggedIn) {
            listManga.forEach { manga ->
                ensureActive()

                count++
                notifier.showProgressNotification(listOf(manga), count, listManga.size)

                // Get this manga's trackers from the database
                val dbTracks = getTracks.await(manga.id)

                // find the mdlist entry if its unfollowed the follow it
                var tracker = dbTracks.firstOrNull { it.trackerId == TrackerManager.MDLIST }
                    ?: mdList.createInitialTracker(manga).toDomainTrack(idRequired = false)

                if (tracker?.status == FollowStatus.UNFOLLOWED.long) {
                    tracker = tracker.copy(
                        status = FollowStatus.READING.long,
                    )
                    val updatedTrack = mdList.update(tracker.toDbTrack())
                    insertTrack.await(updatedTrack.toDomainTrack(false)!!)
                }
            }
        }

        notifier.cancelProgressNotification()
    }
    // SY <--

    private suspend fun withUpdateNotification(
        updatingManga: CopyOnWriteArrayList<Manga>,
        completed: AtomicInt,
        manga: Manga,
        block: suspend () -> Unit,
    ) = coroutineScope {
        ensureActive()

        updatingManga.add(manga)
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )

        block()

        ensureActive()

        updatingManga.remove(manga)
        completed.incrementAndFetch()
        notifier.showProgressNotification(
            updatingManga,
            completed.load(),
            mangaToUpdate.size,
        )
    }

    /**
     * Writes basic file of update errors to cache dir.
     */
    private fun writeErrorFile(errors: List<Pair<Manga, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("shinku_update_errors.txt")
                file.bufferedWriter().use { out ->
                    out.write(context.stringResource(MR.strings.library_errors_help, ERROR_LOG_HELP_URL) + "\n\n")
                    // Error file format:
                    // ! Error
                    //   # Source
                    //     - Manga
                    errors.groupBy({ it.second }, { it.first }).forEach { (error, mangas) ->
                        out.write("\n! ${error}\n")
                        mangas.groupBy { it.source }.forEach { (srcId, mangas) ->
                            val source = sourceManager.getOrStub(srcId)
                            out.write("  # $source\n")
                            mangas.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {
        }
        return File("")
    }

    /**
     * Defines what should be updated within a service execution.
     */
    enum class Target {
        CHAPTERS, // Manga chapters
        COVERS, // Manga covers

        // SY -->
        SYNC_FOLLOWS, // MangaDex specific, pull mangadex manga in reading, rereading

        PUSH_FAVORITES, // MangaDex specific, push mangadex manga to mangadex
        // SY <--
    }

    companion object {
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME_AUTO = "LibraryUpdate-auto"
        private const val WORK_NAME_MANUAL = "LibraryUpdate-manual"

        private const val ERROR_LOG_HELP_URL = "https://shinku.app/docs/guides/troubleshooting/"

        /**
         * Key for category to update.
         */
        private const val KEY_CATEGORY = "category"

        /**
         * Key that defines what should be updated.
         */
        private const val KEY_TARGET = "target"

        // SY -->

        /**
         * Key for group to update.
         */
        const val KEY_GROUP = "group"
        const val KEY_GROUP_EXTRA = "group_extra"
        // SY <--

        fun cancelAllWorks(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val preferences = Injekt.get<LibraryPreferences>()
            val interval = prefInterval ?: preferences.autoUpdateInterval().get()
            if (interval > 0) {
                val restrictions = preferences.autoUpdateDeviceRestrictions().get()
                val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
                val networkRequestBuilder = NetworkRequest.Builder()
                if (DEVICE_ONLY_ON_WIFI in restrictions) {
                    networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                }
                if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                    networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
                val constraints = Constraints.Builder()
                    // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
                    .setRequiredNetworkRequest(networkRequestBuilder.build(), networkType)
                    .setRequiresCharging(DEVICE_CHARGING in restrictions)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<LibraryUpdateJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_AUTO,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }

        fun startNow(
            context: Context,
            category: Category? = null,
            target: Target = Target.CHAPTERS,
            // SY -->
            group: Int = LibraryGroup.BY_DEFAULT,
            groupExtra: String? = null,
            // SY <--
        ): Boolean {
            val wm = context.workManager
            // Check if the LibraryUpdateJob is already running
            if (wm.isRunning(TAG)) {
                // Already running either as a scheduled or manual job
                return false
            }

            val inputData = workDataOf(
                KEY_CATEGORY to category?.id,
                KEY_TARGET to target.name,
                // SY -->
                KEY_GROUP to group,
                KEY_GROUP_EXTRA to groupExtra,
                // SY <--
            )

            val syncPreferences: SyncPreferences = Injekt.get()

            // Always sync the data before library update if syncing is enabled.
            if (syncPreferences.isSyncEnabled()) {
                // Check if SyncDataJob is already running
                if (SyncDataJob.isRunning(context)) {
                    // SyncDataJob is already running
                    return false
                }

                // Define the SyncDataJob
                val syncDataJob = OneTimeWorkRequestBuilder<SyncDataJob>()
                    .addTag(SyncDataJob.TAG_MANUAL)
                    .build()

                // Chain SyncDataJob to run before LibraryUpdateJob
                val libraryUpdateJob = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                    .addTag(TAG)
                    .addTag(WORK_NAME_MANUAL)
                    .setInputData(inputData)
                    .build()

                wm.beginUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, syncDataJob)
                    .then(libraryUpdateJob)
                    .enqueue()
            } else {
                val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
                    .addTag(TAG)
                    .addTag(WORK_NAME_MANUAL)
                    .setInputData(inputData)
                    .build()

                wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)
            }

            return true
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }
    }
}
