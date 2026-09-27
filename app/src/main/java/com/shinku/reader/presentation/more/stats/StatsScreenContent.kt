package com.shinku.reader.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.Badge
import com.shinku.reader.domain.history.model.BadgeCategory
import com.shinku.reader.domain.history.model.BadgeTier
import com.shinku.reader.presentation.more.stats.components.StatsItem
import com.shinku.reader.presentation.more.stats.components.StatsOverviewItem
import com.shinku.reader.presentation.more.stats.data.StatsData
import com.shinku.reader.presentation.util.toDurationString
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.SectionCard
import com.shinku.reader.presentation.core.i18n.stringResource
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
            VelocitySection(state.velocity)
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

enum class MilestoneStatusFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    IN_PROGRESS("In Progress"),
}

@Composable
private fun LazyItemScope.MilestoneSection(
    data: StatsData.Milestones,
) {
    if (data.badges.isEmpty()) return

    var statusFilter by remember { mutableStateOf(MilestoneStatusFilter.ALL) }
    var categoryFilter by remember { mutableStateOf(BadgeCategory.ALL) }
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val totalBadges = data.badges.size
    val earnedBadges = data.earnedBadges.size
    val completionPercent = if (totalBadges > 0) (earnedBadges * 100) / totalBadges else 0
    val readerLevel = (earnedBadges / 3) + 1

    val filteredBadges = remember(data.badges, statusFilter, categoryFilter) {
        data.badges.filter { badge ->
            val matchesStatus = when (statusFilter) {
                MilestoneStatusFilter.ALL -> true
                MilestoneStatusFilter.UNLOCKED -> badge.isEarned
                MilestoneStatusFilter.IN_PROGRESS -> !badge.isEarned
            }
            val matchesCategory = categoryFilter == BadgeCategory.ALL || badge.category == categoryFilter
            matchesStatus && matchesCategory
        }.sortedWith(
            compareByDescending<Badge> { it.isEarned }
                .thenByDescending { it.progress }
        )
    }

    SectionCard(SYMR.strings.label_milestones) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Level & Completion Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎖️ Level $readerLevel Reader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "$earnedBadges of $totalBadges Unlocked ($completionPercent%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "Milestone Guide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (earnedBadges.toFloat() / totalBadges.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            )

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MilestoneStatusFilter.entries.forEach { filter ->
                    val isSelected = statusFilter == filter
                    val count = when (filter) {
                        MilestoneStatusFilter.ALL -> totalBadges
                        MilestoneStatusFilter.UNLOCKED -> earnedBadges
                        MilestoneStatusFilter.IN_PROGRESS -> totalBadges - earnedBadges
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { statusFilter = filter },
                        label = { Text("${filter.label} ($count)") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Category Filter Chips (Scrollable)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(BadgeCategory.entries.toTypedArray()) { cat ->
                    val isSelected = categoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { categoryFilter = cat },
                        label = { Text(cat.label) },
                    )
                }
            }

            // Badges Grid
            if (filteredBadges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No milestones found for this filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                filteredBadges.chunked(2).forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            MilestoneCard(
                                badge = badge,
                                onClick = { selectedBadge = badge },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowBadges.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedBadge?.let { badge ->
        val icon = when (badge.iconId) {
            "time" -> Icons.Outlined.Schedule
            "streak" -> Icons.Outlined.History
            "genre" -> Icons.Outlined.CollectionsBookmark
            "chapter", "library" -> Icons.Outlined.LocalLibrary
            "special" -> Icons.Outlined.EmojiEvents
            else -> Icons.Outlined.Star
        }

        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = {
                Icon(
                    imageVector = if (badge.isEarned) icon else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (badge.isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${badge.tier.icon} ${badge.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${badge.tier.label} Tier • ${badge.category.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    // Progress Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (badge.isEarned) "Status: Completed 🎉" else "Status: In Progress ⏳",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (badge.isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = badge.progressText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { badge.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = if (badge.isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    if (badge.hint.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 Completionist Tip",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = badge.hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Help Roadmap Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "🏆 ShinKu Milestones Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gamify your manga journey! As you read, maintain streaks, and curate your collection, you unlock milestone badges and gain Reader Levels.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Achievement Tiers:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "• 🥉 Bronze: Foundational milestones\n• 🥈 Silver: Dedicated reader feats\n• 🥇 Gold: Veteran manga achievements\n• 💎 Platinum: Elite milestones\n• 👑 Mythic: Legendary reader dedication",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Tip: Filter by 'In Progress' to see the badges you are closest to achieving next!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun MilestoneCard(
    badge: Badge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (badge.iconId) {
        "time" -> Icons.Outlined.Schedule
        "streak" -> Icons.Outlined.History
        "genre" -> Icons.Outlined.CollectionsBookmark
        "chapter", "library" -> Icons.Outlined.LocalLibrary
        "special" -> Icons.Outlined.EmojiEvents
        else -> Icons.Outlined.Star
    }

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (badge.isEarned) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        border = if (badge.isEarned) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (badge.isEarned) icon else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (badge.isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = badge.tier.icon,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (badge.isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (badge.isEarned) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp)
            )

            if (badge.isEarned) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unlocked ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    LinearProgressIndicator(
                        progress = { badge.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        text = badge.progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    if (data.topGenres.isEmpty()) return

    SectionCard(SYMR.strings.label_top_genres) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            data.topGenres.take(5).forEachIndexed { index, genre ->
                val progress = (5 - index).toFloat() / 5f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.VelocitySection(
    data: StatsData.VelocityStats,
) {
    SectionCard(SYMR.strings.label_reading_velocity) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "%.2f".format(Locale.ENGLISH, data.averageVelocity),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(SYMR.strings.label_velocity_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val dailyList = remember(data.dailyVelocity) {
                data.dailyVelocity.entries.sortedBy { it.key }.takeLast(7)
            }
            if (dailyList.isNotEmpty()) {
                val maxVelocity = dailyList.maxOf { it.value }.coerceAtLeast(1.0).toFloat()
                val primaryColor = MaterialTheme.colorScheme.primary
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Bottom
                ) {
                    dailyList.forEach { entry ->
                        val velocityVal = entry.value.toFloat()
                        val ratio = velocityVal / maxVelocity
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(ratio.coerceIn(0.05f, 1f))
                                        .clip(MaterialTheme.shapes.small)
                                ) {
                                    drawRect(color = primaryColor)
                                }
                            }
                            Text(
                                text = "%.1f".format(Locale.ENGLISH, velocityVal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
