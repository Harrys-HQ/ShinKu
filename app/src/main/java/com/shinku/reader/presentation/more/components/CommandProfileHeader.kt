package com.shinku.reader.presentation.more.components

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
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.domain.history.model.SanctuaryXpEngine
import com.shinku.reader.exh.source.ShinKuPreferences
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import com.shinku.reader.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CommandProfileHeader(
    readChapters: Int,
    readDuration: Long,
    readStreak: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    levelInfoOverride: SanctuaryLevelInfo? = null,
) {
    val durationText = remember(readDuration) {
        formatDuration(readDuration)
    }

    val levelInfo = remember(levelInfoOverride, readChapters, readDuration, readStreak) {
        levelInfoOverride ?: SanctuaryXpEngine.calculate(
            chaptersRead = readChapters,
            totalDurationMs = readDuration,
            currentStreak = readStreak,
            completedMangaCount = 0,
        )
    }

    val numFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val shinkuPreferences = remember { Injekt.get<ShinKuPreferences>() }
    val nickname by shinkuPreferences.readerNickname().collectAsState()
    val equippedTitle by shinkuPreferences.equippedBadgeTitle().collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val activeTitle = remember(equippedTitle, levelInfo.defaultRankTitle) {
        equippedTitle.ifBlank { levelInfo.defaultRankTitle }
    }

    if (showEditDialog) {
        ReaderIdentityDialog(
            onDismiss = { showEditDialog = false },
        )
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            )
            .sanctuaryShadow(
                elevation = SanctuaryElevation.hero,
                shape = SanctuaryShapes.hero,
                spotAlpha = 0.35f,
                ambientAlpha = 0.15f,
            ),
        shape = SanctuaryShapes.hero,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SanctuaryBrushes.heroVerticalGradient())
                .clickable(onClick = onClick)
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
                        onClick = { showEditDialog = true },
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        SanctuaryOverlineBadge(
                            text = activeTitle.uppercase(),
                            compact = true,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(SanctuaryShapes.tag)
                                .clickable { showEditDialog = true }
                                .padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = nickname.ifBlank { "Reader Identity" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Reader Identity",
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

                // Unified Live XP Bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Level ${levelInfo.level} Mastery",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${numFormat.format(levelInfo.currentLevelXp)} / ${numFormat.format(levelInfo.nextLevelXp)} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    SanctuaryProgressBar(
                        progress = levelInfo.levelProgress,
                        height = 6.dp,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricPill(
                        value = numFormat.format(readChapters),
                        label = "Chapters",
                        modifier = Modifier.weight(1f),
                    )
                    MetricPill(
                        value = durationText,
                        label = "Time Read",
                        modifier = Modifier.weight(1f),
                    )
                    MetricPill(
                        value = "🔥 ${readStreak}d",
                        label = "Streak",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = SanctuaryShapes.coverLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0m"

    val seconds = millis / 1000
    val totalMinutes = seconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildString {
        if (hours > 0) append("${hours}h ")
        append("${minutes}m")
    }.trim()
}
