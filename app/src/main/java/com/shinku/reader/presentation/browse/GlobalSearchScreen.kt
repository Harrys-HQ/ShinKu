package com.shinku.reader.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.presentation.browse.components.GlobalSearchCardRow
import com.shinku.reader.presentation.browse.components.GlobalSearchErrorResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchLoadingResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchProgressCard
import com.shinku.reader.presentation.browse.components.GlobalSearchResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchToolbar
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.ui.browse.source.globalsearch.SearchItemResult
import com.shinku.reader.ui.browse.source.globalsearch.SearchScreenModel
import com.shinku.reader.ui.browse.source.globalsearch.SourceFilter
import com.shinku.reader.util.system.LocaleHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun GlobalSearchScreen(
    state: SearchScreenModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    onClickImageSearch: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                onClickImageSearch = onClickImageSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
            searchQuery = state.searchQuery,
            progress = state.progress,
            total = state.total,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: ImmutableMap<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
    searchQuery: String? = null,
    progress: Int = 0,
    total: Int = 0,
) {
    val loadedCount = items.values
        .filterIsInstance<SearchItemResult.Success>()
        .sumOf { it.result.size }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        if (!searchQuery.isNullOrBlank() && items.isNotEmpty()) {
            item(key = "global-search-summary") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Results for \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$loadedCount titles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }

        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = fromSourceId?.let {
                        "▶ ${source.name}".takeIf { source.id == fromSourceId }
                    } ?: source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is SearchItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = result.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is SearchItemResult.Error -> {
                            GlobalSearchErrorResultItem(message = result.throwable.message)
                        }
                    }
                }
            }
        }

        if (progress in 1..<total) {
            item(key = "global-search-progress-footer") {
                GlobalSearchProgressCard(
                    progress = progress,
                    total = total,
                )
            }
        }
    }
}
