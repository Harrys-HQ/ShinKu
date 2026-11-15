package com.shinku.reader.presentation.more.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.SanctuaryXpEngine
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.components.ReaderIdentityDialog
import com.shinku.reader.presentation.more.stats.components.ActivityHeatmapCard
import com.shinku.reader.presentation.more.stats.components.CircadianAndVelocityPanel
import com.shinku.reader.presentation.more.stats.components.DailySanctuaryQuestsCard
import com.shinku.reader.presentation.more.stats.components.GenreAndAuthorAffinityPanel
import com.shinku.reader.presentation.more.stats.components.SanctuaryBentoOverview
import com.shinku.reader.presentation.more.stats.components.SanctuaryShareCardDialog
import com.shinku.reader.presentation.util.toDurationString
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private enum class SanctuaryStatsTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.Dashboard),
    INSIGHTS("Insights", Icons.Outlined.AutoGraph),
    MASTERY("Badges", Icons.Outlined.EmojiEvents),
}

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
    showShareDialog: Boolean = false,
    onDismissShareDialog: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showIdentityDialog by remember { mutableStateOf(false) }

    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val nickname by shinkuPreferences.readerNickname().collectAsState()
    val equippedTitle by shinkuPreferences.equippedBadgeTitle().collectAsState()

    val levelInfo = remember(state) {
        state.milestones.levelInfo ?: SanctuaryXpEngine.calculate(
            chaptersRead = state.chapters.readChapterCount,
            totalDurationMs = state.overview.totalReadDuration,
            currentStreak = state.streaks.currentStreak,
            completedMangaCount = state.overview.completedMangaCount,
            earnedBadges = state.milestones.earnedBadges,
        )
    }

    val activeTitle = remember(equippedTitle, levelInfo.defaultRankTitle) {
        equippedTitle.ifBlank { levelInfo.defaultRankTitle }
    }

    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(state.overview.totalReadDuration) {
        state.overview.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }

    if (showIdentityDialog) {
        ReaderIdentityDialog(
            unlockedTitles = state.milestones.earnedBadges.map { it.name },
            onDismiss = { showIdentityDialog = false },
        )
    }

    if (showShareDialog) {
        val peakHour = remember(state.timeStats.timeOfDayHistory) {
            state.timeStats.timeOfDayHistory.maxByOrNull { it.value }?.key ?: 22
        }
        val persona = remember(peakHour, state.timeStats.timeOfDayHistory) {
            if (state.timeStats.timeOfDayHistory.values.none { it > 0L }) {
                "Sanctuary Scholar"
            } else {
                when (peakHour) {
                    in 0..4, in 22..23 -> "Night Owl"
                    in 5..11 -> "Dawn Seeker"
                    in 12..17 -> "Daylight Savant"
                    else -> "Twilight Scholar"
                }
            }
        }
        SanctuaryShareCardDialog(
            nickname = nickname,
            activeTitle = activeTitle,
            levelInfo = levelInfo,
            readDurationText = readDurationString,
            chaptersRead = state.chapters.readChapterCount,
            streakDays = state.streaks.currentStreak,
            topGenres = state.genres.topGenres,
            unlockedBadgesCount = state.milestones.earnedBadges.size,
            readingPersona = persona,
            onDismiss = onDismissShareDialog,
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + MaterialTheme.padding.extraSmall,
            bottom = paddingValues.calculateBottomPadding() + MaterialTheme.padding.large,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SanctuaryStatsHeroCard(
                nickname = nickname,
                activeTitle = activeTitle,
                readDurationString = readDurationString,
                overview = state.overview,
                streaks = state.streaks,
                levelInfo = levelInfo,
                onEditIdentity = { showIdentityDialog = true },
            )
        }

        item {
            SanctuaryStatsTabSelector(
                selectedIndex = selectedTab,
                onSelectTab = { selectedTab = it },
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
        }

        item {
            AnimatedContent(
                targetState = selectedTab,
                label = "stats_tab_content",
            ) { tabIndex ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (SanctuaryStatsTab.entries[tabIndex]) {
                        SanctuaryStatsTab.OVERVIEW -> {
                            DailySanctuaryQuestsCard(levelInfo = levelInfo)
                            SanctuaryBentoOverview(
                                overview = state.overview,
                                titles = state.titles,
                                chapters = state.chapters,
                                trackers = state.trackers,
                            )
                        }
                        SanctuaryStatsTab.INSIGHTS -> {
                            ActivityHeatmapCard(dailyHistory = state.timeStats.dailyHistory)
                            CircadianAndVelocityPanel(
                                timeStats = state.timeStats,
                                velocity = state.velocity,
                            )
                            GenreAndAuthorAffinityPanel(
                                genres = state.genres,
                                authors = state.authors,
                            )
                        }
                        SanctuaryStatsTab.MASTERY -> {
                            MilestoneSection(data = state.milestones)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SanctuaryStatsTabSelector(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SanctuaryStatsTab.entries.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SanctuaryShapes.medium)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelectTab(index) }),
                    shape = SanctuaryShapes.medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                    },
                ) {
                    Column(
                        modifier = Modifier.heightIn(min = 48.dp).padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
