package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.shinku.reader.R
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.manga.model.MangaCover
import com.shinku.reader.exh.metadata.metadata.MangaDexSearchMetadata
import com.shinku.reader.exh.metadata.metadata.RaisedSearchMetadata
import com.shinku.reader.exh.metadata.metadata.RankedSearchMetadata
import com.shinku.reader.presentation.core.components.Badge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.library.components.CommonMangaItemDefaults
import com.shinku.reader.presentation.library.components.MangaComfortableGridItem
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowseSourceComfortableGrid(
    mangaList: LazyPagingItems<StateFlow</* SY --> */Pair<Manga, RaisedSearchMetadata?>/* SY <-- */>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    source: Source? = null,
) {
    val sourceContextLabel = remember(source?.id, source?.name) {
        source?.name?.takeIf { it.isNotBlank() }
    }
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonMangaItemDefaults.GridHorizontalSpacer),
    ) {
        if (mangaList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = mangaList.itemCount) { index ->
            // SY -->
            val pair by mangaList[index]?.collectAsState() ?: return@items
            val manga = pair.first
            val metadata = pair.second
            // SY <--

            BrowseSourceComfortableGridItem(
                manga = manga,
                // SY -->
                metadata = metadata,
                // SY <--
                sourceContextLabel = sourceContextLabel,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }

        if (mangaList.loadState.refresh is LoadState.Loading || mangaList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
private fun BrowseSourceComfortableGridItem(
    manga: Manga,
    // SY -->
    metadata: RaisedSearchMetadata?,
    // SY <--
    sourceContextLabel: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    Column {
        MangaComfortableGridItem(
            title = manga.title,
            titleMaxLines = 3,
            captionSurface = true,
            coverData = MangaCover(
                mangaId = manga.id,
                sourceId = manga.source,
                isMangaFavorite = manga.favorite,
                ogUrl = manga.thumbnailUrl,
                lastModified = manga.coverLastModified,
            ),
            coverAlpha = if (manga.favorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            coverBadgeStart = {
                InLibraryBadge(enabled = manga.favorite)
            },
            // SY -->
            coverBadgeEnd = {
                if (metadata is MangaDexSearchMetadata) {
                    metadata.followStatus?.let { followStatus ->
                        val text = LocalContext.current
                            .resources
                            .let {
                                remember {
                                    it.getStringArray(R.array.md_follows_options)
                                        .getOrNull(followStatus)
                                }
                            }
                            ?: return@let
                        Badge(
                            text = text,
                            color = MaterialTheme.colorScheme.tertiary,
                            textColor = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                    metadata.relation?.let {
                        Badge(
                            text = stringResource(it.res),
                            color = MaterialTheme.colorScheme.tertiary,
                            textColor = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                } else if (metadata is RankedSearchMetadata) {
                    metadata.rank?.let {
                        Badge(
                            text = "+$it",
                            color = MaterialTheme.colorScheme.tertiary,
                            textColor = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            },
            // SY <--
            onLongClick = onLongClick,
            onClick = onClick,
        )
        val genreSubtitle = manga.genre?.take(2)?.joinToString(" · ")
        val metaLine = listOfNotNull(sourceContextLabel, genreSubtitle?.takeIf { it.isNotBlank() }).joinToString(" · ")
        if (metaLine.isNotBlank()) {
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
