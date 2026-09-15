package com.shinku.reader.presentation.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.browse.components.GlobalSearchCardRow
import com.shinku.reader.presentation.browse.components.GlobalSearchErrorResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchLoadingResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.tachiyomi.source.CatalogueSource
import com.shinku.reader.ui.browse.feed.FeedScreenState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.model.SavedSearch
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.material.PullRefresh
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.core.util.shimmer
import kotlin.time.Duration.Companion.seconds

@androidx.compose.runtime.Immutable
data class FeedItemUI(
    val feed: FeedSavedSearch,
    val savedSearch: SavedSearch?,
    val source: CatalogueSource?,
    val title: String,
    val subtitle: String,
    val results: kotlinx.collections.immutable.ImmutableList<Manga>?,
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
                            com.shinku.reader.presentation.browse.components.DiscoverFeaturedCarousel(
                                featuredManga = state.featuredManga,
                                onClickManga = onClickManga,
                            )
                        }
                    }

                    // 2. Discover Genre Navigation Pills
                    item {
                        com.shinku.reader.presentation.browse.components.DiscoverGenreCloud(
                            selectedGenre = state.selectedGenre,
                            onSelectGenre = onClickGenre,
                        )
                    }

                    // 3. Dynamic Selected Genre Section (When user taps any genre chip)
                    if (state.selectedGenre != null) {
                        item {
                            com.shinku.reader.presentation.browse.components.DiscoverSectionHeader(
                                title = "Top ${state.selectedGenre} Titles",
                                onSeeAllClick = {
                                    onSeeAllTitles(
                                        "Top ${state.selectedGenre} Titles",
                                        state.selectedGenreManga.orEmpty(),
                                        "genre",
                                        state.selectedGenre.orEmpty(),
                                    )
                                },
                            )
                            if (state.isLoadingGenre) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            } else if (!state.selectedGenreManga.isNullOrEmpty()) {
                                com.shinku.reader.presentation.browse.components.DiscoverMangaRow(
                                    mangas = state.selectedGenreManga,
                                    onClickManga = onClickManga,
                                )
                            }
                        }
                    }

                    // 4. Titles For You Section (Personalized based on reading behavior & new releases)
                    if (state.forYouManga.isNotEmpty()) {
                        item {
                            com.shinku.reader.presentation.browse.components.DiscoverSectionHeader(
                                title = "Titles For You",
                                onSeeAllClick = {
                                    onSeeAllTitles("Titles For You", state.forYouManga, "for_you", "")
                                },
                            )
                            com.shinku.reader.presentation.browse.components.DiscoverMangaRow(
                                mangas = state.forYouManga,
                                onClickManga = onClickManga,
                            )
                        }
                    }

                    // 5. Preloaded Genre Highlights across Extensions
                    state.genreSections.forEach { (genreTitle, mangas) ->
                        if (mangas.isNotEmpty()) {
                            item(key = "genre_$genreTitle") {
                                com.shinku.reader.presentation.browse.components.DiscoverSectionHeader(
                                    title = "$genreTitle Highlights",
                                    onSeeAllClick = {
                                        onSeeAllTitles("$genreTitle Highlights", mangas, "genre", genreTitle)
                                    },
                                )
                                com.shinku.reader.presentation.browse.components.DiscoverMangaRow(
                                    mangas = mangas,
                                    onClickManga = onClickManga,
                                )
                            }
                        }
                    }

                    // 6. Recommendations Section
                    if (!state.recommendations.isNullOrEmpty()) {
                        item {
                            com.shinku.reader.presentation.browse.components.DiscoverSectionHeader(
                                title = "Recommendations",
                                onSeeAllClick = {
                                    onSeeAllTitles(
                                        "Recommendations",
                                        state.recommendations.orEmpty(),
                                        "recommendations",
                                        "",
                                    )
                                },
                            )
                            com.shinku.reader.presentation.browse.components.DiscoverMangaRow(
                                mangas = state.recommendations,
                                onClickManga = onClickManga,
                            )
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
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
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
                                            .clip(RoundedCornerShape(12.dp))
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

@Composable
fun FeedAddDialog(
    sources: ImmutableList<CatalogueSource>,
    onDismiss: () -> Unit,
    onClickAdd: (CatalogueSource?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(SYMR.strings.feed),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            RadioSelector(options = sources, selected = selected) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(selected?.let { sources[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun FeedAddSearchDialog(
    source: CatalogueSource,
    savedSearches: ImmutableList<SavedSearch?>,
    onDismiss: () -> Unit,
    onClickAdd: (CatalogueSource, SavedSearch?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = source.name,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            val context = LocalContext.current
            val savedSearchStrings = remember {
                savedSearches.map {
                    it?.name ?: context.stringResource(MR.strings.latest)
                }.toImmutableList()
            }
            RadioSelector(
                options = savedSearches,
                optionStrings = savedSearchStrings,
                selected = selected,
            ) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(source, selected?.let { savedSearches[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun <T> RadioSelector(
    options: ImmutableList<T>,
    optionStrings: ImmutableList<String> = remember { options.map { it.toString() }.toImmutableList() },
    selected: Int?,
    onSelectOption: (Int) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        optionStrings.forEachIndexed { index, option ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected == index) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectOption(index) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected == index, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun FeedDeleteConfirmDialog(
    feed: FeedSavedSearch,
    onDismiss: () -> Unit,
    onClickDeleteConfirm: (FeedSavedSearch) -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(SYMR.strings.feed),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(text = stringResource(SYMR.strings.feed_delete))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickDeleteConfirm(feed) }) {
                Text(text = stringResource(MR.strings.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun FeedFilterDialog(
    currentSourceFilter: String,
    currentLanguageFilter: String,
    currentShowSourceFeeds: Boolean,
    customFeeds: List<FeedItemUI>?,
    enabledLanguages: Set<String>,
    onDismiss: () -> Unit,
    onApply: (sourceFilter: String, languageFilter: String, showSourceFeeds: Boolean) -> Unit,
    onAddFeed: () -> Unit,
    onDeleteFeed: (FeedSavedSearch) -> Unit,
) {
    var sourceFilter by remember { mutableStateOf(currentSourceFilter) }
    var languageFilter by remember { mutableStateOf(currentLanguageFilter) }
    var showSourceFeeds by remember { mutableStateOf(currentShowSourceFeeds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Feed Settings & Filter",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Source Feeds Toggle (Original SY Feeds)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Source Feeds & Searches",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Display custom sources and saved searches on the feed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = showSourceFeeds,
                            onCheckedChange = { showSourceFeeds = it },
                        )
                    }
                }

                // If source feeds are enabled, show management section and active feeds
                if (showSourceFeeds) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // + Add Source Feed Action Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddFeed),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add Source Feed",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Add a source or saved search to feed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Active Custom Feeds List
                    if (!customFeeds.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Active Feeds (${customFeeds.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        customFeeds.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
                                    IconButton(
                                        onClick = { onDeleteFeed(item.feed) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(MR.strings.action_delete),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Source Scope
                Text(
                    text = "Source Scope",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = sourceFilter == "all",
                        onClick = { sourceFilter = "all" },
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("All Sources") },
                    )
                    FilterChip(
                        selected = sourceFilter == "pinned",
                        onClick = { sourceFilter = "pinned" },
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("Pinned Only") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Language Filter
                Text(
                    text = "Language Filter",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Restrict suggestions and highlights to selected language:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = languageFilter == "en",
                        onClick = { languageFilter = "en" },
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("English (en)") },
                    )
                    FilterChip(
                        selected = languageFilter == "all",
                        onClick = { languageFilter = "all" },
                        shape = RoundedCornerShape(10.dp),
                        label = { Text("All Languages") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(sourceFilter, languageFilter, showSourceFeeds) },
            ) {
                Text(text = "Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun DiscoverFeedSkeleton(
    contentPadding: PaddingValues,
    selectedGenre: String? = null,
    onClickGenre: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true,
    ) {
        // 1. Shimmer Featured Hero Carousel Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmer(),
            )
        }

        // 2. Interactive Discover Genre Cloud (Fully interactive so user can tap genres immediately!)
        item {
            com.shinku.reader.presentation.browse.components.DiscoverGenreCloud(
                selectedGenre = selectedGenre,
                onSelectGenre = onClickGenre,
            )
        }

        // 3. Section 1 Shimmer: "Titles For You"
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(140.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(4) {
                    Column(modifier = Modifier.width(116.dp)) {
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .height(162.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shimmer(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                    }
                }
            }
        }

        // 4. Section 2 Shimmer: Highlights
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .width(160.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(4) {
                    Column(modifier = Modifier.width(116.dp)) {
                        Box(
                            modifier = Modifier
                                .width(116.dp)
                                .height(162.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shimmer(),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                    }
                }
            }
        }
    }
}

