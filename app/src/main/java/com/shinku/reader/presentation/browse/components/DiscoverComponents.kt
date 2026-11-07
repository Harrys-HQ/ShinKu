package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.library.components.CommonMangaItemDefaults
import com.shinku.reader.presentation.manga.components.MangaCover

@Composable
fun DiscoverGenreCloud(
    selectedGenre: String? = null,
    onSelectGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genres = listOf(
        "Action",
        "Adventure",
        "Comedy",
        "Fantasy",
        "Isekai",
        "Psychological",
        "Romance",
        "Sci-Fi",
        "Seinen",
        "Shonen",
        "Slice of Life",
        "Supernatural",
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.small),
        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            val isAllSelected = selectedGenre.isNullOrEmpty()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAllSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
                },
                border = BorderStroke(
                    1.dp,
                    if (isAllSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    },
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectGenre("") },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = if (isAllSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isAllSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        items(genres) { genre ->
            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    },
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectGenre(genre) },
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun DiscoverSectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onClearClick != null) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (onSeeAllClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(SanctuaryShapes.pill)
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoverMangaRow(
    mangas: List<Manga>,
    onClickManga: (Manga) -> Unit,
    onLongClickManga: ((Manga) -> Unit)? = null,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    if (mangas.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.extraSmall),
        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(mangas, key = { it.id }) { manga ->
            DiscoverMangaCard(
                manga = manga,
                onClick = { onClickManga(manga) },
                onLongClick = onLongClickManga?.let { { it(manga) } },
                getMangaState = getMangaState,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverMangaCard(
    manga: Manga,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    getMangaState: (@Composable (Manga) -> State<Manga>)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentManga = getMangaState?.invoke(manga)?.value ?: manga
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val cardWidth = if (isTablet) 128.dp else 116.dp
    val cardHeight = if (isTablet) 180.dp else 162.dp

    Column(
        modifier = modifier
            .width(cardWidth)
            .clip(SanctuaryShapes.coverLarge)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke() ?: onClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight),
        ) {
            Surface(
                shape = SanctuaryShapes.coverLarge,
                shadowElevation = SanctuaryElevation.compact,
                border = SanctuaryBrushes.subtleBorder(),
                modifier = Modifier.fillMaxSize(),
            ) {
                MangaCover.Book(
                    data = currentManga,
                    contentDescription = currentManga.title,
                    shape = SanctuaryShapes.coverLarge,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (currentManga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f),
                )
            }

            if (currentManga.favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    InLibraryBadge(enabled = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = currentManga.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val genreSummary = currentManga.genre?.take(2)?.joinToString(" · ").orEmpty()
        if (genreSummary.isNotBlank()) {
            Text(
                text = genreSummary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
