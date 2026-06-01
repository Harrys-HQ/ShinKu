package com.shinku.reader.ui.stats

import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.util.fastCountNot
import com.shinku.reader.presentation.more.stats.StatsScreenState
import com.shinku.reader.presentation.more.stats.data.StatsData
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.shinku.reader.domain.history.interactor.GetTotalReadDuration
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import com.shinku.reader.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.manga.interactor.GetReadMangaNotInLibraryView
import com.shinku.reader.domain.track.interactor.GetTracks
import com.shinku.reader.domain.track.model.Track
import eu.kanade.tachiyomi.source.local.isLocal

import kotlin.time.DurationUnit
import kotlin.time.toDuration

class StatsScreenModel(
    private val downloadManager: DownloadManager,
    private val getLibraryManga: GetLibraryManga,
    private val getTotalReadDuration: GetTotalReadDuration,
    private val getTracks: GetTracks,
    private val preferences: LibraryPreferences,
    private val trackerManager: TrackerManager,
    // SY -->
    private val getReadMangaNotInLibraryView: GetReadMangaNotInLibraryView,
    private val getReadingStats: com.shinku.reader.domain.history.interactor.GetReadingStats,
    // SY <--
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val loggedInTrackers by lazy { trackerManager.loggedInTrackers() }

    // SY -->
    private val _allRead = MutableStateFlow(false)
    val allRead = _allRead.asStateFlow()
    // SY <--

    init {
        // SY -->
        _allRead.onEach { allRead ->
            mutableState.update { StatsScreenState.Loading }
            val libraryManga = getLibraryManga.await() + if (allRead) {
                getReadMangaNotInLibraryView.await()
            } else {
                emptyList()
            }
            // SY <--

            val distinctLibraryManga = libraryManga.fastDistinctBy { it.id }

            val mangaTrackMap = getMangaTrackMap(distinctLibraryManga)
            val scoredMangaTrackerMap = getScoredMangaTrackMap(mangaTrackMap)

            val meanScore = getTrackMeanScore(scoredMangaTrackerMap)

            val overviewStatData = StatsData.Overview(
                libraryMangaCount = distinctLibraryManga.size,
                completedMangaCount = distinctLibraryManga.count {
                    it.manga.status.toInt() == SManga.COMPLETED && it.unreadCount == 0L
                },
                totalReadDuration = getTotalReadDuration.await(),
            )

            val titlesStatData = StatsData.Titles(
                globalUpdateItemCount = getGlobalUpdateItemCount(libraryManga),
                startedMangaCount = distinctLibraryManga.count { it.hasStarted },
                localMangaCount = distinctLibraryManga.count { it.manga.isLocal() },
            )

            val chaptersStatData = StatsData.Chapters(
                totalChapterCount = distinctLibraryManga.sumOf { it.totalChapters }.toInt(),
                readChapterCount = distinctLibraryManga.sumOf { it.readCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = mangaTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedInTrackers.size,
            )

            // SY -->
            val readingStats = getReadingStats.await()
            val streaksStatData = StatsData.Streaks(
                currentStreak = readingStats.currentStreak,
            )
            val genresStatData = StatsData.Genres(
                topGenres = readingStats.bestGenres,
            )
            val authorsStatData = StatsData.Authors(
                topAuthors = readingStats.bestAuthors,
            )
            val timeStatData = StatsData.TimeStats(
                timeOfDayHistory = readingStats.timeOfDayHistory,
            )
            val velocityStatData = StatsData.VelocityStats(
                averageVelocity = readingStats.averageVelocity,
                dailyVelocity = readingStats.dailyVelocity,
            )
            val milestonesStatData = StatsData.Milestones(
                earnedBadges = readingStats.badges,
            )
            // SY <--

            mutableState.update {
                StatsScreenState.Success(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    chapters = chaptersStatData,
                    trackers = trackersStatData,
                    // SY -->
                    streaks = streaksStatData,
                    genres = genresStatData,
                    authors = authorsStatData,
                    timeStats = timeStatData,
                    velocity = velocityStatData,
                    milestones = milestonesStatData,
                    // SY <--
                )
            }
            // SY -->
        }.launchIn(screenModelScope)
        // SY <--
    }

    private fun getGlobalUpdateItemCount(libraryManga: List<LibraryManga>): Int {
        val includedCategories = preferences.updateCategories().get().map { it.toLong() }.toSet()
        val excludedCategories = preferences.updateCategoriesExclude().get().map { it.toLong() }.toSet()
        val updateRestrictions = preferences.autoUpdateMangaRestrictions().get()

        return libraryManga.filter {
            val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
            val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
            included && !excluded
        }
            .fastCountNot {
                (MANGA_NON_COMPLETED in updateRestrictions && it.manga.status.toInt() == SManga.COMPLETED) ||
                    (MANGA_HAS_UNREAD in updateRestrictions && it.unreadCount != 0L) ||
                    (MANGA_NON_READ in updateRestrictions && it.totalChapters > 0 && !it.hasStarted)
            }
    }

    private suspend fun getMangaTrackMap(libraryManga: List<LibraryManga>): Map<Long, List<Track>> {
        val loggedInTrackerIds = loggedInTrackers.map { it.id }.toHashSet()
        return libraryManga.associate { manga ->
            val tracks = getTracks.await(manga.id)
                .fastFilter { it.trackerId in loggedInTrackerIds }

            manga.id to tracks
        }
    }

    private fun getScoredMangaTrackMap(mangaTrackMap: Map<Long, List<Track>>): Map<Long, List<Track>> {
        return mangaTrackMap.mapNotNull { (mangaId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            mangaId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredMangaTrackMap: Map<Long, List<Track>>): Double {
        return scoredMangaTrackMap
            .map { (_, tracks) ->
                tracks.map(::get10PointScore).average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: Track): Double {
        val service = trackerManager.get(track.trackerId)!!
        return service.get10PointScore(track)
    }

    fun toggleReadManga() {
        _allRead.value = !_allRead.value
    }

    fun getWrappedSummary(context: android.content.Context): String {
        val state = state.value as? StatsScreenState.Success ?: return ""
        val none = context.getString(com.shinku.reader.i18n.MR.strings.none.resourceId)
        
        val duration = state.overview.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .let { d ->
                val hours = d.inWholeHours
                val minutes = d.inWholeMinutes % 60
                if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            }

        return buildString {
            append("📖 My ShinKu Reading Journey\n\n")
            append("⏱ Total Time: $duration\n")
            append("⚡ Speed: %.2f pages/min\n".format(java.util.Locale.ENGLISH, state.velocity.averageVelocity))
            append("📚 Titles in Library: ${state.overview.libraryMangaCount}\n")
            append("✅ Completed: ${state.overview.completedMangaCount}\n")
            append("🔥 Current Streak: ${state.streaks.currentStreak} days\n")
            append("🏆 Badges Unlocked: ${state.milestones.earnedBadges.size}\n\n")
            
            if (state.genres.topGenres.isNotEmpty()) {
                append("🎭 Top Genres: ${state.genres.topGenres.take(3).joinToString(", ")}\n")
            }
            if (state.authors.topAuthors.isNotEmpty()) {
                append("✍️ Top Authors: ${state.authors.topAuthors.take(3).joinToString(", ")}\n")
            }
            
            append("\n#ShinKuReader #Manga")
        }
    }
}
