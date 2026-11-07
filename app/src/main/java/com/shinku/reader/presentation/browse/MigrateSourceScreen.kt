package com.shinku.reader.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.interactor.SetMigrateSorting
import com.shinku.reader.domain.source.model.Source
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.browse.components.BaseSourceItem
import com.shinku.reader.presentation.browse.components.SourceIcon
import com.shinku.reader.presentation.core.components.Badge
import com.shinku.reader.presentation.core.components.BadgeGroup
import com.shinku.reader.presentation.core.components.ScrollbarLazyColumn
import com.shinku.reader.presentation.core.components.Scroller.STICKY_HEADER_KEY_PREFIX
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.header
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.core.util.secondaryItemAlpha
import com.shinku.reader.ui.browse.migration.sources.MigrateSourceScreenModel
import com.shinku.reader.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun MigrateSourceScreen(
    state: MigrateSourceScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onFailedUpdates: () -> Unit,
    onToggleSortingDirection: () -> Unit,
    onToggleSortingMode: () -> Unit,
    // SY -->
    onClickAll: (Source) -> Unit,
    // SY <--
) {
    val context = LocalContext.current
    var onlyMissing by rememberSaveable { mutableStateOf(false) }
    val items = if (onlyMissing) state.items.filter { it.first.isStub }.toImmutableList() else state.items
    val direction = LocalLayoutDirection.current
    val listPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(direction),
        end = contentPadding.calculateEndPadding(direction),
        bottom = contentPadding.calculateBottomPadding(),
    )
    Column(Modifier.padding(top = contentPadding.calculateTopPadding())) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = 6.dp),
            shape = SanctuaryShapes.deckCard,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
            border = SanctuaryBrushes.subtleBorder(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = SanctuaryShapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = "Some titles can't be updated from their current sources. Review the list below and choose new sources to keep them updated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MigrateFilterPill(
                label = stringResource(MR.strings.migration_all_sources),
                selected = !onlyMissing,
                onClick = { onlyMissing = false },
            )
            MigrateFilterPill(
                label = "${stringResource(MR.strings.migration_missing_sources)} (${state.items.count { it.first.isStub }})",
                selected = onlyMissing,
                onClick = { onlyMissing = true },
            )
            MigrateFilterPill(
                label = stringResource(MR.strings.migration_failed_updates),
                selected = false,
                onClick = onFailedUpdates,
            )
        }
        Box(Modifier.weight(1f)) {
            when {
                state.isLoading -> LoadingScreen(Modifier.padding(listPadding))
                items.isEmpty() -> EmptyScreen(
                    stringRes = if (onlyMissing) MR.strings.migration_no_missing_sources else MR.strings.information_empty_library,
                    modifier = Modifier.padding(listPadding),
                )
                else ->
                    MigrateSourceList(
                        list = items,
                        contentPadding = listPadding,
                        onClickItem = onClickItem,
                        onLongClickItem = { source ->
                            val sourceId = source.id.toString()
                            context.copyToClipboard(sourceId, sourceId)
                        },
                        sortingMode = state.sortingMode,
                        onToggleSortingMode = onToggleSortingMode,
                        sortingDirection = state.sortingDirection,
                        onToggleSortingDirection = onToggleSortingDirection,
                        // SY -->
                        onClickAll = onClickAll,
                        // SY <--
                    )
            }
        }
    }
}

@Composable
private fun MigrateFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .clip(SanctuaryShapes.medium)
            .clickable(onClick = onClick),
        shape = SanctuaryShapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = if (selected) null else SanctuaryBrushes.subtleBorder(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MigrateSourceList(
    list: ImmutableList<Pair<Source, Long>>,
    contentPadding: PaddingValues,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    sortingMode: SetMigrateSorting.Mode,
    onToggleSortingMode: () -> Unit,
    sortingDirection: SetMigrateSorting.Direction,
    onToggleSortingDirection: () -> Unit,
    // SY -->
    onClickAll: (Source) -> Unit,
    // SY <--
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        stickyHeader(key = STICKY_HEADER_KEY_PREFIX) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.migration_selection_prompt),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.header,
                )

                IconButton(onClick = onToggleSortingMode) {
                    when (sortingMode) {
                        SetMigrateSorting.Mode.ALPHABETICAL -> Icon(
                            Icons.Outlined.SortByAlpha,
                            contentDescription = stringResource(MR.strings.action_sort_alpha),
                        )
                        SetMigrateSorting.Mode.TOTAL -> Icon(
                            Icons.Outlined.Numbers,
                            contentDescription = stringResource(MR.strings.action_sort_count),
                        )
                    }
                }
                IconButton(onClick = onToggleSortingDirection) {
                    when (sortingDirection) {
                        SetMigrateSorting.Direction.ASCENDING -> Icon(
                            Icons.Outlined.ArrowUpward,
                            contentDescription = stringResource(MR.strings.action_asc),
                        )
                        SetMigrateSorting.Direction.DESCENDING -> Icon(
                            Icons.Outlined.ArrowDownward,
                            contentDescription = stringResource(MR.strings.action_desc),
                        )
                    }
                }
            }
        }

        items(
            items = list,
            key = { (source, _) -> "migrate-${source.id}" },
        ) { (source, count) ->
            MigrateSourceItem(
                modifier = Modifier.animateItem(),
                source = source,
                count = count,
                onClickItem = { onClickItem(source) },
                onLongClickItem = { onLongClickItem(source) },
                // SY -->
                onClickAll = { onClickAll(source) },
                // SY <--
            )
        }
    }
}

@Composable
private fun MigrateSourceItem(
    source: Source,
    count: Long,
    onClickItem: () -> Unit,
    onLongClickItem: () -> Unit,
    // SY -->
    onClickAll: () -> Unit,
    // SY <--
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        showLanguageInContent = source.lang != "",
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        icon = { SourceIcon(source = source) },
        action = {
            BadgeGroup {
                Badge(text = "$count")
            }
            // SY -->
            TextButton(onClick = onClickAll) {
                Text(
                    text = stringResource(MR.strings.all),
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            // SY <--
        },
        content = { _, sourceLangString ->
            Column(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .weight(1f),
            ) {
                Text(
                    text = source.name.ifBlank { source.id.toString() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sourceLangString != null) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = sourceLangString,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (source.isStub) {
                        Text(
                            modifier = Modifier.secondaryItemAlpha(),
                            text = stringResource(MR.strings.not_installed),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}
