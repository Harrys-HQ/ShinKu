package com.shinku.reader.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.more.stats.components.StatsItem
import com.shinku.reader.presentation.more.stats.components.StatsOverviewItem
import com.shinku.reader.presentation.more.stats.data.StatsData
import com.shinku.reader.presentation.util.toDurationString
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.SectionCard
import com.shinku.reader.presentation.core.i18n.stringResource
import androidx.compose.material.icons.outlined.History
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OverviewSection(state.overview)
        }
        item {
            TitlesStats(state.titles)
        }
        item {
            ChapterStats(state.chapters)
        }
        item {
            TrackerStats(state.trackers)
        }
        // SY -->
        item {
            StreakSection(state.streaks)
        }
        item {
            GenreSection(state.genres)
        }
        item {
            AuthorSection(state.authors)
        }
        item {
            TimeOfDaySection(state.timeStats)
        }
        item {
            MilestoneSection(state.milestones)
        }
        // SY <--
    }
}

@Composable
private fun LazyItemScope.MilestoneSection(
    data: StatsData.Milestones,
) {
    if (data.earnedBadges.isEmpty()) return

    SectionCard(SYMR.strings.label_milestones) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            data.earnedBadges.chunked(2).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowBadges.forEach { badge ->
                        val icon = when (badge.iconId) {
                            "time" -> Icons.Outlined.Schedule
                            "streak" -> Icons.Outlined.History
                            "genre" -> Icons.Outlined.CollectionsBookmark
                            else -> Icons.Outlined.LocalLibrary
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            StatsItem(
                                title = badge.name,
                                subtitle = badge.description,
                                icon = icon,
                            )
                        }
                    }
                    if (rowBadges.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.AuthorSection(
    data: StatsData.Authors,
) {
    SectionCard(SYMR.strings.label_top_authors) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            data.topAuthors.chunked(2).forEach { rowAuthors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowAuthors.forEach { author ->
                        Row(modifier = Modifier.weight(1f)) {
                            StatsItem(
                                title = author,
                                subtitle = "",
                            )
                        }
                    }
                    if (rowAuthors.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.TimeOfDaySection(
    data: StatsData.TimeStats,
) {
    val maxDuration = remember(data.timeOfDayHistory) {
        data.timeOfDayHistory.values.maxOrNull() ?: 1L
    }

    SectionCard(SYMR.strings.label_reading_time_patterns) {
        val primaryColor = MaterialTheme.colorScheme.primary
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                (0..23).forEach { hour ->
                    val duration = data.timeOfDayHistory[hour] ?: 0L
                    val alpha = (duration.toFloat() / maxDuration.toFloat()).coerceIn(0.1f, 1f)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                    ) {
                        drawRect(
                            color = primaryColor.copy(alpha = alpha)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12am", style = MaterialTheme.typography.labelSmall)
                Text("6am", style = MaterialTheme.typography.labelSmall)
                Text("12pm", style = MaterialTheme.typography.labelSmall)
                Text("6pm", style = MaterialTheme.typography.labelSmall)
                Text("11pm", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LazyItemScope.StreakSection(
    data: StatsData.Streaks,
) {
    SectionCard(SYMR.strings.label_streaks) {
        Row {
            StatsItem(
                data.currentStreak.toString(),
                stringResource(SYMR.strings.label_current_streak),
            )
        }
    }
}

@Composable
private fun LazyItemScope.GenreSection(
    data: StatsData.Genres,
) {
    SectionCard(SYMR.strings.label_top_genres) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            data.topGenres.chunked(2).forEach { rowGenres ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowGenres.forEach { genre ->
                        Row(modifier = Modifier.weight(1f)) {
                            StatsItem(
                                title = genre,
                                subtitle = "",
                            )
                        }
                    }
                    if (rowGenres.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.Overview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    SectionCard(MR.strings.label_overview_section) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            StatsOverviewItem(
                title = data.libraryMangaCount.toString(),
                subtitle = stringResource(MR.strings.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
            )
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(MR.strings.label_read_duration),
                icon = Icons.Outlined.Schedule,
            )
            StatsOverviewItem(
                title = data.completedMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
            )
        }
    }
}

@Composable
private fun LazyItemScope.TitlesStats(
    data: StatsData.Titles,
) {
    SectionCard(MR.strings.label_titles_section) {
        Row {
            StatsItem(
                data.globalUpdateItemCount.toString(),
                stringResource(MR.strings.label_titles_in_global_update),
            )
            StatsItem(
                data.startedMangaCount.toString(),
                stringResource(MR.strings.label_started),
            )
            StatsItem(
                data.localMangaCount.toString(),
                stringResource(MR.strings.label_local),
            )
        }
    }
}

@Composable
private fun LazyItemScope.ChapterStats(
    data: StatsData.Chapters,
) {
    SectionCard(MR.strings.chapters) {
        Row {
            StatsItem(
                data.totalChapterCount.toString(),
                stringResource(MR.strings.label_total_chapters),
            )
            StatsItem(
                data.readChapterCount.toString(),
                stringResource(MR.strings.label_read_chapters),
            )
            StatsItem(
                data.downloadCount.toString(),
                stringResource(MR.strings.label_downloaded),
            )
        }
    }
}

@Composable
private fun LazyItemScope.TrackerStats(
    data: StatsData.Trackers,
) {
    val notApplicable = stringResource(MR.strings.not_applicable)
    val meanScoreStr = remember(data.trackedTitleCount, data.meanScore) {
        if (data.trackedTitleCount > 0 && !data.meanScore.isNaN()) {
            // All other numbers are localized in English
            "%.2f ★".format(Locale.ENGLISH, data.meanScore)
        } else {
            notApplicable
        }
    }
    SectionCard(MR.strings.label_tracker_section) {
        Row {
            StatsItem(
                data.trackedTitleCount.toString(),
                stringResource(MR.strings.label_tracked_titles),
            )
            StatsItem(
                meanScoreStr,
                stringResource(MR.strings.label_mean_score),
            )
            StatsItem(
                data.trackerCount.toString(),
                stringResource(MR.strings.label_used),
            )
        }
    }
}
