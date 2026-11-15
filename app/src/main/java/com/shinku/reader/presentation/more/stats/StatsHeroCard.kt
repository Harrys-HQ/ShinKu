package com.shinku.reader.presentation.more.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import com.shinku.reader.presentation.more.components.SanctuaryAvatarBox
import com.shinku.reader.presentation.more.stats.data.StatsData
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun SanctuaryStatsHeroCard(
    nickname: String,
    activeTitle: String,
    readDurationString: String,
    overview: StatsData.Overview,
    streaks: StatsData.Streaks,
    levelInfo: SanctuaryLevelInfo,
    onEditIdentity: () -> Unit,
) {
    val numFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val headerTitle = remember(nickname) {
        nickname.ifBlank { "Sanctuary Chronicles" }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            )
            .sanctuaryShadow(
                elevation = SanctuaryElevation.hero,
                shape = SanctuaryShapes.hero,
                spotAlpha = 0.38f,
                ambientAlpha = 0.16f,
            ),
        shape = SanctuaryShapes.hero,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SanctuaryBrushes.heroVerticalGradient())
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SanctuaryAvatarBox(
                        size = 56.dp,
                        levelBadge = levelInfo.level,
                        onClick = onEditIdentity,
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SanctuaryShapes.tag)
                            .clickable(onClick = onEditIdentity)
                            .padding(vertical = 2.dp),
                    ) {
                        SanctuaryOverlineBadge(
                            text = activeTitle.uppercase(),
                            compact = true,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Identity",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = SanctuaryShapes.pill,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${levelInfo.streakShields}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Single-Line XP & Streak Row
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "${streaks.currentStreak}d Streak • Lv.${levelInfo.level}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "${numFormat.format(levelInfo.currentLevelXp)} / ${numFormat.format(levelInfo.nextLevelXp)} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 3,
                        )
                    }
                    SanctuaryProgressBar(
                        progress = levelInfo.levelProgress,
                        height = 6.dp,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                com.shinku.reader.presentation.more.stats.components.JourneyOverviewMetrics(
                    libraryCount = numFormat.format(overview.libraryMangaCount),
                    readDuration = readDurationString,
                    completedCount = numFormat.format(overview.completedMangaCount),
                )
            }
        }
    }
}
