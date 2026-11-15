package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

@Composable
fun ActivityHeatmapCard(
    dailyHistory: Map<Date, Long>,
    modifier: Modifier = Modifier,
) {
    var weeks by rememberSaveable { mutableIntStateOf(12) }
    val daysPerWeek = 7
    val totalDays = weeks * daysPerWeek
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val days = remember(dailyHistory, weeks, today, zone) {
        readingActivityDays(dailyHistory, totalDays, today, zone)
    }
    val maximum = days.maxOf { it.duration }.coerceAtLeast(1L)
    val gridData = days.map { day ->
        if (day.duration == 0L) 0f else (day.duration.toFloat() / maximum).coerceIn(0.22f, 1f)
    }
    val activeDaysInRange = days.count { it.duration > 0L }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val dateRange = "${days.first().date.format(formatter)} – ${today.format(formatter)}"
    val minuteLabel = stringResource(MR.strings.journey_minutes_short)
    val chartDescription = days.joinToString("; ") {
        "${it.date.format(formatter)}: ${it.duration / 60_000} $minuteLabel"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SanctuarySectionHeader(
                    title = stringResource(MR.strings.journey_reading_activity),
                    overline = "CONSISTENCY HEATMAP",
                )
                Surface(
                    shape = SanctuaryShapes.pill,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = stringResource(MR.strings.journey_activity_summary, activeDaysInRange, totalDays),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(dateRange, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 4, 12).forEach { range ->
                    FilterChip(
                        selected = weeks == range,
                        onClick = { weeks = range },
                        label = { Text(stringResource(MR.strings.journey_activity_days, range * 7)) },
                        shape = SanctuaryShapes.pill,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            if (activeDaysInRange == 0) {
                Text(stringResource(MR.strings.journey_activity_empty), style = MaterialTheme.typography.bodySmall)
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .semantics { contentDescription = chartDescription },
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

            Text(
                text = stringResource(MR.strings.journey_activity_legend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
