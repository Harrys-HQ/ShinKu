package com.shinku.reader.presentation.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.browse.components.GlobalSearchCardRow
import com.shinku.reader.presentation.browse.components.GlobalSearchErrorResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchLoadingResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchProgressCard
import com.shinku.reader.presentation.browse.components.GlobalSearchResultItem
import com.shinku.reader.presentation.browse.components.GlobalSearchToolbar
import com.shinku.reader.presentation.browse.components.SearchFeedbackCard
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
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
            failedSourceCount = state.items.values.count { it is SearchItemResult.Error },
            noSourcesSelected = state.noSourcesSelected,
            onShowAllSources = if (state.sourceFilter != SourceFilter.All || state.onlyShowHasResults) {
                {
                    if (state.onlyShowHasResults) onToggleResults()
                    if (state.sourceFilter != SourceFilter.All) onChangeSearchFilter(SourceFilter.All)
                }
            } else {
                null
            },
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
    failedSourceCount: Int = items.values.count { it is SearchItemResult.Error },
    noSourcesSelected: Boolean = false,
    onShowAllSources: (() -> Unit)? = null,
) {
    val loadedCount = items.values
        .filterIsInstance<SearchItemResult.Success>()
        .sumOf { it.result.size }

    LazyColumn(
        contentPadding = contentPadding,
    ) {
        if (!searchQuery.isNullOrBlank() && items.isNotEmpty()) {
            item(key = "global-search-summary") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.search_query_summary, searchQuery),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(MR.strings.search_title_count, loadedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!searchQuery.isNullOrBlank() && noSourcesSelected) {
            item(key = "global-search-no-sources") {
                SearchFeedbackCard(
                    title = stringResource(MR.strings.search_no_sources_heading),
                    message = stringResource(MR.strings.search_no_sources_guidance),
                    onShowAllSources = onShowAllSources,
                )
            }
        } else if (!searchQuery.isNullOrBlank() && failedSourceCount > 0) {
            item(key = "global-search-failures") {
                SearchFeedbackCard(
                    message = stringResource(
                        if (loadedCount > 0) MR.strings.search_sources_unavailable else MR.strings.search_sources_failed_without_results,
                        failedSourceCount,
                    ),
                    onShowAllSources = onShowAllSources,
                )
            }
        } else if (!searchQuery.isNullOrBlank() && total > 0 && progress >= total && loadedCount == 0) {
            item(key = "global-search-empty") {
                SearchFeedbackCard(
                    title = stringResource(MR.strings.search_no_matching_titles),
                    message = stringResource(MR.strings.search_no_matching_guidance),
                    onShowAllSources = onShowAllSources,
                )
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
