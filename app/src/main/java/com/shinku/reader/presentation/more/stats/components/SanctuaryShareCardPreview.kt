package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shinku.reader.R
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun SanctuaryShareCardPreview(
    theme: ShareCardTheme,
    avatarUri: String,
    nickname: String,
    activeTitle: String,
    levelInfo: SanctuaryLevelInfo,
    readingPersona: String,
    readDurationText: String,
    chaptersRead: Int,
    streakDays: Int,
    unlockedBadgesCount: Int,
    topGenres: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = theme.surfaceColor,
        border = BorderStroke(1.dp, theme.primaryColor.copy(alpha = 0.45f)),
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            theme.secondaryColor.copy(alpha = 0.85f),
                            theme.surfaceColor,
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header: Avatar + Nickname + Title + Persona
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(SanctuaryShapes.medium)
                            .background(theme.primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_shinku_logo),
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = theme.onSurfaceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Lv.${levelInfo.level} • $activeTitle",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = readingPersona,
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.onSurfaceColor.copy(alpha = 0.65f),
                        )
                    }
                }

                // Level Progress
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${levelInfo.currentLevelXp} / ${levelInfo.nextLevelXp} XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.onSurfaceColor.copy(alpha = 0.7f),
                        )
                        Text(
                            text = "${(levelInfo.levelProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                        )
                    }
                    SanctuaryProgressBar(
                        progress = levelInfo.levelProgress,
                        height = 6.dp,
                        color = theme.primaryColor,
                        trackColor = theme.primaryColor.copy(alpha = 0.15f),
                    )
                }

                // 4 Mini Key Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiniStatColumn(label = "TIME", value = readDurationText, theme = theme)
                    MiniStatColumn(label = "CHAPTERS", value = chaptersRead.toString(), theme = theme)
                    MiniStatColumn(label = "STREAK", value = "${streakDays}d", theme = theme)
                    MiniStatColumn(label = "BADGES", value = unlockedBadgesCount.toString(), theme = theme)
                }

                if (topGenres.isNotEmpty()) {
                    Text(
                        text = "Top: ${topGenres.take(3).joinToString(" • ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.primaryColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatColumn(label: String, value: String, theme: ShareCardTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = theme.primaryColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = theme.onSurfaceColor.copy(alpha = 0.72f),
        )
    }
}
