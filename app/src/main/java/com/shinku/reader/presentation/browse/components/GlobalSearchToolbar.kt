package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.components.SearchToolbar
import com.shinku.reader.ui.browse.source.globalsearch.SourceFilter
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClickImageSearch: (() -> Unit)? = null,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    SearchToolbar(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        searchQuery = searchQuery,
        onChangeSearchQuery = onChangeSearchQuery,
        onSearch = onSearch,
        onClickCloseSearch = navigateUp,
        onClickImageSearch = onClickImageSearch,
        navigateUp = navigateUp,
        scrollBehavior = scrollBehavior,
        bottomContent = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                if (progress in 1..<total) {
                    LinearProgressIndicator(
                        progress = { progress / total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.padding.small)
                        .padding(bottom = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!hideSourceFilter) {
                        FilterChip(
                            selected = sourceFilter == SourceFilter.PinnedOnly,
                            onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(MR.strings.pinned_sources))
                            },
                        )
                        FilterChip(
                            selected = sourceFilter == SourceFilter.All,
                            onClick = { onChangeSearchFilter(SourceFilter.All) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(MR.strings.all))
                            },
                        )

                        VerticalDivider(modifier = Modifier.size(height = 32.dp, width = 1.dp))
                    }

                    FilterChip(
                        selected = onlyShowHasResults,
                        onClick = { onToggleResults() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(MR.strings.has_results))
                        },
                    )
                }

                HorizontalDivider()
            }
        },
    )
}
