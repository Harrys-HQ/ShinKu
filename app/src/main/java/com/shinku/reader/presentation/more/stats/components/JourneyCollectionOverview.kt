package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.more.stats.data.StatsData
import java.text.NumberFormat

@Composable
fun SanctuaryBentoOverview(
    overview: StatsData.Overview,
    titles: StatsData.Titles,
    chapters: StatsData.Chapters,
    trackers: StatsData.Trackers,
    modifier: Modifier = Modifier,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val total = overview.libraryMangaCount.coerceAtLeast(1)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JourneyDetailCard(
            title = stringResource(MR.strings.journey_collection),
            value = stringResource(
                MR.strings.journey_collection_counts,
                numbers.format(overview.completedMangaCount), numbers.format(overview.libraryMangaCount),
            ),
            detail = stringResource(MR.strings.journey_started_count, numbers.format(titles.startedMangaCount)),
            progress = overview.completedMangaCount.toFloat() / total,
        )
        JourneyDetailCard(
            title = stringResource(MR.strings.journey_chapters),
            value = stringResource(
                MR.strings.journey_chapter_counts,
                numbers.format(chapters.readChapterCount), numbers.format(chapters.totalChapterCount),
            ),
            detail = stringResource(MR.strings.journey_download_count, numbers.format(chapters.downloadCount)),
            progress = chapters.readChapterCount.toFloat() / chapters.totalChapterCount.coerceAtLeast(1),
        )
        val score = if (trackers.trackedTitleCount > 0 && trackers.meanScore.isFinite()) {
            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(trackers.meanScore)
        } else {
            stringResource(MR.strings.not_applicable)
        }
        JourneyDetailCard(
            title = stringResource(MR.strings.journey_tracking),
            value = stringResource(MR.strings.journey_mean_score, score),
            detail = stringResource(
                MR.strings.journey_tracker_counts,
                numbers.format(trackers.trackedTitleCount), numbers.format(trackers.trackerCount),
            ),
        )
    }
}

@Composable
private fun JourneyDetailCard(title: String, value: String, detail: String, progress: Float? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress != null) SanctuaryProgressBar(progress = progress)
        }
    }
}
