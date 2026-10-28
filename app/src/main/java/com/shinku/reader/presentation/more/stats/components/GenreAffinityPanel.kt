package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.more.stats.data.StatsData

@Composable
fun GenreAndAuthorAffinityPanel(
    genres: StatsData.Genres,
    authors: StatsData.Authors,
    modifier: Modifier = Modifier,
) {
    if (genres.topGenres.isEmpty() && authors.topAuthors.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SanctuarySectionHeader(
                title = stringResource(SYMR.strings.label_top_genres),
                overline = "TASTE SPECTRUM & CREATORS",
            )

            genres.topGenres.take(5).forEachIndexed { index, genre ->
                val progress = (5 - index).toFloat() / 5f
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Rank #${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    SanctuaryProgressBar(
                        progress = progress,
                        height = 7.dp,
                    )
                }
            }

            if (authors.topAuthors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(SYMR.strings.label_top_authors),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val medals = listOf("🥇", "🥈", "🥉")
                    authors.topAuthors.take(3).forEachIndexed { idx, author ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            border = SanctuaryBrushes.subtleBorder(),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(text = medals.getOrElse(idx) { "#${idx + 1}" })
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
