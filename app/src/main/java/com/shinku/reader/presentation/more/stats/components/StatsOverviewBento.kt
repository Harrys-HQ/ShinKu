package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.DailyQuest
import com.shinku.reader.domain.history.model.SanctuaryLevelInfo
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun DailySanctuaryQuestsCard(
    levelInfo: SanctuaryLevelInfo,
    modifier: Modifier = Modifier,
) {
    val completedCount = levelInfo.dailyQuests.count { it.isCompleted }
    val totalCount = levelInfo.dailyQuests.size

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SanctuarySectionHeader(
                    title = "Daily Sanctuary Quests",
                    overline = "DAILY COMMISSIONS ($completedCount/$totalCount)",
                )
                Surface(
                    shape = SanctuaryShapes.pill,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                            text = "${levelInfo.streakShields} Shields",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            levelInfo.dailyQuests.forEach { quest ->
                QuestRow(quest = quest)
            }
        }
    }
}

@Composable
private fun QuestRow(quest: DailyQuest) {
    Surface(
        shape = SanctuaryShapes.medium,
        color = if (quest.isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        },
        border = SanctuaryBrushes.subtleBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (quest.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (quest.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = quest.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "${quest.description} (${quest.current}/${quest.target})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SanctuaryProgressBar(
                    progress = quest.progress,
                    height = 5.dp,
                )
            }
        }
    }
}
