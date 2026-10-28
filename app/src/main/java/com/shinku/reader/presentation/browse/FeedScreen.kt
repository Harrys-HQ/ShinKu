package com.shinku.reader.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.browse.components.DiscoverFeaturedCarousel
import com.shinku.reader.presentation.browse.components.DiscoverFeedSkeleton
import com.shinku.reader.presentation.browse.components.DiscoverGenreCloud
import com.shinku.reader.presentation.browse.components.DiscoverMangaRow
import com.shinku.reader.presentation.browse.components.DiscoverSectionHeader
import com.shinku.reader.presentation.browse.components.GlobalSearchCardRow
import com.shinku.reader.presentation.browse.components.GlobalSearchErrorResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchLoadingResultItem
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.PullRefresh
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.ui.browse.feed.FeedScreenState
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class FeedItemUI(
    val feed: FeedSavedSearch,
    val savedSearch: SavedSearch?,
    val source: CatalogueSource?,
    val title: String,
    val subtitle: String,
    val results: ImmutableList<Manga>?,
)

@Composable
fun FeedScreen(
    state: FeedScreenState,
    contentPadding: PaddingValues,
    onClickSavedSearch: (SavedSearch, CatalogueSource) -> Unit,
    onClickSource: (CatalogueSource) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
    onClickGenre: (String) -> Unit = {},
    onSeeAllGenre: (String) -> Unit = {},
    onSeeAllTitles: (String, List<Manga>, String, String) -> Unit = { _, _, _, _ -> },
    onAddFeed: () -> Unit = {},
    onRefresh: () -> Unit,
    getMangaState: @Composable (Manga) -> State<Manga>,
) {
    val hasContent = state.hasContent

    when {
        state.isLoading -> DiscoverFeedSkeleton(
            contentPadding = contentPadding,
            selectedGenre = state.selectedGenre,
            onClickGenre = onClickGenre,
        )
        !hasContent -> EmptyScreen(
            SYMR.strings.feed_tab_empty,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            PullRefresh(
                refreshing = state.isRefreshing,
                onRefresh = onRefresh,
                enabled = true,
                indicatorPadding = contentPadding,
            ) {
                ScrollbarLazyColumn(
                    contentPadding = contentPadding + topSmallPaddingValues,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 1. Featured Manga Carousel Banner
                    if (state.featuredManga.isNotEmpty()) {
                        item {
                            DiscoverFeaturedCarousel(
                                featuredManga = state.featuredManga,
                                onClickManga = onClickManga,
                                getMangaState = getMangaState,
                            )
                        }
                    }

                    // 2. Discover Genre Navigation Pills
                    item {
                        DiscoverGenreCloud(
                            selectedGenre = state.selectedGenre,
                            onSelectGenre = onClickGenre,
                        )
                    }

                    // 3. Dynamic Selected Genre Section (When user taps any genre chip)
                    if (state.selectedGenre != null) {
                        item {
                            val hasGenreManga = !state.selectedGenreManga.isNullOrEmpty()
                            DiscoverSectionHeader(
                                title = "Top ${state.selectedGenre} Titles",
                                onSeeAllClick = if (hasGenreManga) {
                                    {
                                        onSeeAllTitles(
                                            "Top ${state.selectedGenre} Titles",
                                            state.selectedGenreManga.orEmpty(),
                                            "genre",
                                            state.selectedGenre.orEmpty(),
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClearClick = { onClickGenre(state.selectedGenre.orEmpty()) },
                            )
                            if (state.isLoadingGenre) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            } else if (hasGenreManga) {
                                DiscoverMangaRow(
                                    mangas = state.selectedGenreManga!!,
                                    onClickManga = onClickManga,
                                    getMangaState = getMangaState,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No titles found for \"${state.selectedGenre}\" in active sources",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // 4. Titles For You Section (Personalized based on reading behavior & new releases)
                    if (state.forYouManga.isNotEmpty()) {
                        item {
                            DiscoverSectionHeader(
                                title = "Titles For You",
                                onSeeAllClick = {
                                    onSeeAllTitles("Titles For You", state.forYouManga, "for_you", "")
                                },
                            )
                            DiscoverMangaRow(
                                mangas = state.forYouManga,
                                onClickManga = onClickManga,
                                getMangaState = getMangaState,
                            )
                        }
                    }

                    // 5. AI Sanctuary Picks (When an AI provider is configured)
                    if (!state.recommendations.isNullOrEmpty()) {
                        item {
                            DiscoverSectionHeader(
                                title = "AI Sanctuary Picks",
                                onSeeAllClick = {
                                    onSeeAllTitles(
                                        "AI Sanctuary Picks",
                                        state.recommendations.orEmpty(),
                                        "recommendations",
                                        "",
                                    )
                                },
                            )
                            DiscoverMangaRow(
                                mangas = state.recommendations,
                                onClickManga = onClickManga,
                                getMangaState = getMangaState,
                            )
                        }
                    }

                    // 6. Curated Genre Highlights across Healthy Extensions
                    state.genreSections.forEach { (genreTitle, mangas) ->
                        if (mangas.isNotEmpty()) {
                            item(key = "genre_$genreTitle") {
                                DiscoverSectionHeader(
                                    title = "$genreTitle Highlights",
                                    onSeeAllClick = {
                                        onSeeAllTitles("$genreTitle Highlights", mangas, "genre", genreTitle)
                                    },
                                )
                                DiscoverMangaRow(
                                    mangas = mangas,
                                    onClickManga = onClickManga,
                                    getMangaState = getMangaState,
                                )
                            }
                        }
                    }

                    // 7. Custom Feeds & Saved Searches
                    if (state.showSourceFeeds && !state.items.isNullOrEmpty()) {
                        items(
                            state.items.orEmpty(),
                            key = { it.feed.id },
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (item.savedSearch != null && item.source != null) {
                                                onClickSavedSearch(item.savedSearch, item.source)
                                            } else if (item.source != null) {
                                                onClickSource(item.source)
                                            }
                                        },
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onClickDelete(item.feed) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(MR.strings.action_delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (item.savedSearch != null && item.source != null) {
                                                    onClickSavedSearch(item.savedSearch, item.source)
                                                } else if (item.source != null) {
                                                    onClickSource(item.source)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "See",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                            FeedItem(
                                item = item,
                                getMangaState = { getMangaState(it) },
                                onClickManga = onClickManga,
                            )
                        }
                    }

                    // 8. Add Source Feed Card
                    if (state.showSourceFeeds) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = SanctuaryShapes.deckCard,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = SanctuaryBrushes.subtleBorder(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(SanctuaryShapes.deckCard)
                                    .clickable { onAddFeed() },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(SanctuaryShapes.medium)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Add Source Feed",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Pin a source or saved search to your feed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedItem(
    item: FeedItemUI,
    getMangaState: @Composable ((Manga) -> State<Manga>),
    onClickManga: (Manga) -> Unit,
) {
    when {
        item.results == null -> {
            GlobalSearchLoadingResultItem()
        }
        item.results.isEmpty() -> {
            GlobalSearchErrorResultItem(message = stringResource(MR.strings.no_results_found))
        }
        else -> {
            GlobalSearchCardRow(
                titles = item.results,
                getManga = getMangaState,
                onClick = onClickManga,
                onLongClick = onClickManga,
            )
        }
    }
}
