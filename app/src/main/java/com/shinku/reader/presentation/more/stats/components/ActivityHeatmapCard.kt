package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun ActivityHeatmapCard(
    dailyHistory: Map<Date, Long>,
    modifier: Modifier = Modifier,
) {
    val weeks = 12
    val daysPerWeek = 7
    val totalDays = weeks * daysPerWeek

    val gridData = remember(dailyHistory) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMs = cal.timeInMillis
        val dayMillis = TimeUnit.DAYS.toMillis(1)
        val maxDuration = dailyHistory.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

        (totalDays - 1 downTo 0).map { daysAgo ->
            val targetDate = Date(todayMs - daysAgo * dayMillis)
            val duration = dailyHistory[targetDate] ?: 0L
            val intensity = if (duration <= 0L) {
                0f
            } else {
                (duration.toFloat() / maxDuration.toFloat()).coerceIn(0.22f, 1f)
            }
            intensity
        }
    }

    val activeDaysLast12Weeks = remember(gridData) {
        gridData.count { it > 0f }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SanctuarySectionHeader(
                    title = "12-Week Activity Matrix",
                    overline = "CONSISTENCY HEATMAP",
                )
                Surface(
                    shape = SanctuaryShapes.pill,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = "$activeDaysLast12Weeks / $totalDays Active Days",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
            ) {
                val gap = 4.dp.toPx()
                val cellWidth = (size.width - gap * (weeks - 1)) / weeks
                val cellHeight = (size.height - gap * (daysPerWeek - 1)) / daysPerWeek
                val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                for (w in 0 until weeks) {
                    for (d in 0 until daysPerWeek) {
                        val idx = w * daysPerWeek + d
                        val intensity = gridData.getOrElse(idx) { 0f }
                        val cellColor = if (intensity <= 0f) {
                            emptyColor
                        } else {
                            primaryColor.copy(alpha = intensity)
                        }
                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(w * (cellWidth + gap), d * (cellHeight + gap)),
                            size = Size(cellWidth, cellHeight),
                            cornerRadius = corner,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "12 Weeks Ago",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(0.1f, 0.35f, 0.65f, 1f).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(SanctuaryShapes.microIndicator)
                                .background(primaryColor.copy(alpha = alpha)),
                        )
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
