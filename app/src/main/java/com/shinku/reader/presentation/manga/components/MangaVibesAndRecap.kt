package com.shinku.reader.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.core.components.SanctuaryGradientCard
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.SanctuarySectionHeader
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun MangaRecapCard(
    isLoadingRecap: Boolean,
    recapText: String?,
    onGenerateRecap: (() -> Unit)?,
    onDismissRecap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    SanctuaryGradientCard(
        modifier = modifier.padding(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.small,
        ),
        shape = SanctuaryShapes.card,
        elevation = SanctuaryElevation.compact,
        brush = SanctuaryBrushes.compactHorizontalGradient(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SanctuaryOverlineBadge(
                text = "AI STORY RECAP",
                compact = true,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "It has been over 2 weeks since you last read this manga. Would you like a quick AI summary of what happened in the last read chapters to get you back up to speed?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            when {
                isLoadingRecap -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(28.dp),
                    )
                }
                recapText != null -> {
                    Surface(
                        shape = SanctuaryShapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = SanctuaryBrushes.subtleBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = recapText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { onDismissRecap?.invoke() }) {
                            Text("Close", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { onDismissRecap?.invoke() }) {
                            Text(
                                text = "Ignore",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { onGenerateRecap?.invoke() },
                            shape = SanctuaryShapes.medium,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.heightIn(min = 40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generate AI Recap",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimilarVibesSection(
    mangaList: List<Manga>,
    onClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mangaList.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.extraSmall),
    ) {
        SanctuarySectionHeader(
            title = "SIMILAR VIBES",
            trailingBadge = "${mangaList.size} MATCHES",
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = mangaList,
                key = { it.id },
            ) { manga ->
                Surface(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(SanctuaryShapes.deckCard)
                        .clickable { onClick(manga) },
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                    shadowElevation = SanctuaryElevation.subtle,
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                    ) {
                        MangaCover.Book(
                            data = manga,
                            shape = SanctuaryShapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.68f),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
