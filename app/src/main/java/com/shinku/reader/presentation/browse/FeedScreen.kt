package com.shinku.reader.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import kotlin.time.Duration.Companion.seconds

data class FeedItemUI(
    val feed: FeedSavedSearch,
    val savedSearch: SavedSearch?,
    val source: CatalogueSource?,
    val title: String,
    val subtitle: String,
    val results: List<Manga>?,
)

@Composable
fun FeedScreen(
    state: FeedScreenState,
    contentPadding: PaddingValues,
    onClickSavedSearch: (SavedSearch, CatalogueSource) -> Unit,
    onClickSource: (CatalogueSource) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
    onRefresh: () -> Unit,
    getMangaState: @Composable (Manga) -> State<Manga>,
) {
    when {
        state.isLoading -> LoadingScreen()
        state.isEmpty -> EmptyScreen(
            SYMR.strings.feed_tab_empty,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            var refreshing by remember { mutableStateOf(false) }
            LaunchedEffect(refreshing) {
                if (refreshing) {
                    delay(1.seconds)
                    refreshing = false
                }
            }
            PullRefresh(
                refreshing = refreshing && state.isLoadingItems,
                onRefresh = {
                    refreshing = true
                    onRefresh()
                },
                enabled = !state.isLoadingItems,
            ) {
                ScrollbarLazyColumn(
                    contentPadding = contentPadding + topSmallPaddingValues,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        state.items.orEmpty(),
                        key = { it.feed.id },
                    ) { item ->
                        GlobalSearchResultItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            onLongClick = {
                                onClickDelete(item.feed)
                            },
                            onClick = {
                                if (item.savedSearch != null && item.source != null) {
                                    onClickSavedSearch(item.savedSearch, item.source)
                                } else if (item.source != null) {
                                    onClickSource(item.source)
                                }
                            },
                            modifier = Modifier.animateItem(),
                        ) {
                            FeedItem(
                                item = item,
                                getMangaState = { getMangaState(it) },
                                onClickManga = onClickManga,
                            )
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
        title = {
            Text(text = stringResource(SYMR.strings.feed))
        },
        text = {
            RadioSelector(options = sources, selected = selected) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(selected?.let { sources[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok))
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
        title = {
            Text(text = source.name)
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
                Text(text = stringResource(MR.strings.action_ok))
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onSelectOption(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(4.dp))
                Text(option, maxLines = 1)
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
        title = {
            Text(text = stringResource(SYMR.strings.feed))
        },
        text = {
            Text(text = stringResource(SYMR.strings.feed_delete))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickDeleteConfirm(feed) }) {
                Text(text = stringResource(MR.strings.action_delete))
            }
        },
    )
}
