package com.shinku.reader.presentation.more.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
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
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun ReadingJourneyCard(
    readChapters: Int,
    readDuration: Long,
    readStreak: Int,
    onClick: () -> Unit,
) {
    val durationText = remember(readDuration) {
        formatDuration(readDuration)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(SYMR.strings.reading_journey_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text(
                            text = readChapters.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(SYMR.strings.reading_journey_chapters_read),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    Column {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(SYMR.strings.reading_journey_time_read),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(SYMR.strings.reading_journey_streak_days, readStreak),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(SYMR.strings.reading_journey_streak),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
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
