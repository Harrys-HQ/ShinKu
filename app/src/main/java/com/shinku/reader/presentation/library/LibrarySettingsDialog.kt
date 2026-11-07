package com.shinku.reader.presentation.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.shinku.reader.R
import com.shinku.reader.core.common.preference.TriState
import com.shinku.reader.domain.category.model.Category
import com.shinku.reader.domain.library.model.LibraryDisplayMode
import com.shinku.reader.domain.library.model.LibraryGroup
import com.shinku.reader.domain.library.model.LibrarySort
import com.shinku.reader.domain.library.model.sort
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.components.TabbedDialog
import com.shinku.reader.presentation.components.TabbedDialogPaddings
import com.shinku.reader.presentation.core.components.BaseSortItem
import com.shinku.reader.presentation.core.components.CheckboxItem
import com.shinku.reader.presentation.core.components.HeadingItem
import com.shinku.reader.presentation.core.components.IconItem
import com.shinku.reader.presentation.core.components.SettingsChipRow
import com.shinku.reader.presentation.core.components.SliderItem
import com.shinku.reader.presentation.core.components.SortItem
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.library.components.LibraryFilterItem
import com.shinku.reader.ui.library.LibrarySettingsScreenModel
import com.shinku.reader.util.system.isDevFlavor
import com.shinku.reader.util.system.isPreviewBuildType
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

@Composable
fun LibrarySettingsDialog(
    onDismissRequest: () -> Unit,
    screenModel: LibrarySettingsScreenModel,
    category: Category?,
    // SY -->
    hasCategories: Boolean,
    // SY <--
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { 4 }
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()
    val trackers by screenModel.trackersFlow.collectAsState()
    val downloaded by screenModel.libraryPreferences.filterDownloaded().collectAsState()
    val otherFilters = with(screenModel.libraryPreferences) {
        listOf(filterUnread(), filterStarted(), filterBookmarked(), filterCompleted(), filterIntervalCustom(), filterLewd()) +
            trackers.map { filterTracking(it.id.toInt()) }
    }
    val activeFilters = otherFilters.count { it.collectAsState().value != TriState.DISABLED } +
        if (downloadedOnly || downloaded != TriState.DISABLED) 1 else 0
    TabbedDialog(
        pagerState = pagerState,
        onDismissRequest = onDismissRequest,
        header = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(MR.strings.library_filters),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Surface(
                    shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.pill,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        stringResource(MR.strings.library_filters_active, activeFilters),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        footer = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                if (pagerState.currentPage == 0) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = screenModel::resetFilters,
                        modifier = Modifier.weight(1f),
                        shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.medium,
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                        Text(stringResource(MR.strings.action_reset), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                androidx.compose.material3.Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.medium,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Text(stringResource(MR.strings.action_done), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        tabTitles = persistentListOf(
            stringResource(MR.strings.action_filter),
            stringResource(MR.strings.action_sort),
            stringResource(MR.strings.action_display),
            // SY -->
            stringResource(SYMR.strings.group),
            // SY <--
        ),
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> FilterPage(
                    screenModel = screenModel,
                )
                1 -> SortPage(
                    category = category,
                    screenModel = screenModel,
                )
                2 -> DisplayPage(
                    screenModel = screenModel,
                )
                // SY -->
                3 -> GroupPage(
                    screenModel = screenModel,
                    hasCategories = hasCategories,
                )
                // SY <--
            }
        }
    }
}

@Composable
private fun ColumnScope.FilterPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloaded().collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly().collectAsState()

    LibraryFilterItem(
        label = stringResource(MR.strings.label_downloaded),
        icon = Icons.Outlined.Download,
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.libraryPreferences.filterDownloaded().set(it) },
    )
    val filterUnread by screenModel.libraryPreferences.filterUnread().collectAsState()
    LibraryFilterItem(
        label = stringResource(MR.strings.action_filter_unread),
        icon = Icons.Outlined.MenuBook,
        state = filterUnread,
        onClick = { screenModel.libraryPreferences.filterUnread().set(it) },
    )
    val filterCompleted by screenModel.libraryPreferences.filterCompleted().collectAsState()
    LibraryFilterItem(
        label = stringResource(MR.strings.completed),
        icon = Icons.Outlined.CheckCircleOutline,
        state = filterCompleted,
        onClick = { screenModel.libraryPreferences.filterCompleted().set(it) },
    )
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.TextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Text(stringResource(MR.strings.library_more_filters), modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
    }
    if (expanded) AdditionalFilters(screenModel)
}

@Composable
private fun ColumnScope.AdditionalFilters(screenModel: LibrarySettingsScreenModel) {
    val autoUpdateMangaRestrictions by screenModel.libraryPreferences.autoUpdateMangaRestrictions().collectAsState()
    val filterStarted by screenModel.libraryPreferences.filterStarted().collectAsState()
    LibraryFilterItem(
        label = stringResource(MR.strings.label_started),
        state = filterStarted,
        onClick = { screenModel.libraryPreferences.filterStarted().set(it) },
    )
    val filterBookmarked by screenModel.libraryPreferences.filterBookmarked().collectAsState()
    LibraryFilterItem(
        label = stringResource(MR.strings.action_filter_bookmarked),
        icon = Icons.Outlined.BookmarkBorder,
        state = filterBookmarked,
        onClick = { screenModel.libraryPreferences.filterBookmarked().set(it) },
    )
    // TODO: re-enable when custom intervals are ready for stable
    if (
        (isDevFlavor || isPreviewBuildType) &&
        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in autoUpdateMangaRestrictions
    ) {
        val filterIntervalCustom by screenModel.libraryPreferences.filterIntervalCustom().collectAsState()
        LibraryFilterItem(
            label = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onClick = { screenModel.libraryPreferences.filterIntervalCustom().set(it) },
        )
    }
    // SY -->
    val filterLewd by screenModel.libraryPreferences.filterLewd().collectAsState()
    LibraryFilterItem(
        label = stringResource(SYMR.strings.lewd),
        state = filterLewd,
        onClick = { screenModel.libraryPreferences.filterLewd().set(it) },
    )
    // SY <--

    val trackers by screenModel.trackersFlow.collectAsState()
    when (trackers.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackers[0]
            val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
            LibraryFilterItem(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.libraryPreferences.filterTracking(service.id.toInt()).set(it) },
            )
        }
        else -> {
            HeadingItem(MR.strings.action_filter_tracked)
            trackers.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
                LibraryFilterItem(
                    label = service.name,
                    state = filterTracker,
                    onClick = { screenModel.libraryPreferences.filterTracking(service.id.toInt()).set(it) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SortPage(
    category: Category?,
    screenModel: LibrarySettingsScreenModel,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    // SY -->
    val globalSortMode by screenModel.libraryPreferences.sortingMode().collectAsState()
    val sortingMode = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        category.sort.type
    } else {
        globalSortMode.type
    }
    val sortDescending = if (screenModel.grouping == LibraryGroup.BY_DEFAULT) {
        !category.sort.isAscending
    } else {
        !globalSortMode.isAscending
    }
    val hasSortTags by remember {
        screenModel.libraryPreferences.sortTagsForLibrary().changes()
            .map { it.isNotEmpty() }
    }.collectAsState(initial = screenModel.libraryPreferences.sortTagsForLibrary().get().isNotEmpty())
    // SY <--

    val options = remember(trackers.isEmpty()/* SY --> */, hasSortTags/* SY <-- */) {
        val trackerMeanPair = if (trackers.isNotEmpty()) {
            MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean
        } else {
            null
        }
        // SY -->
        val tagSortPair = if (hasSortTags) {
            SYMR.strings.tag_sorting to LibrarySort.Type.TagList
        } else {
            null
        }
        // SY <--
        listOfNotNull(
            MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
            MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
            MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
            MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
            MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
            MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
            MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
            MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
            trackerMeanPair,
            // SY -->
            tagSortPair,
            // SY <--
            MR.strings.action_sort_random to LibrarySort.Type.Random,
        )
    }

    options.map { (titleRes, mode) ->
        if (mode == LibrarySort.Type.Random) {
            BaseSortItem(
                label = stringResource(titleRes),
                icon = Icons.Default.Refresh
                    .takeIf { sortingMode == LibrarySort.Type.Random },
                onClick = {
                    screenModel.setSort(category, mode, LibrarySort.Direction.Ascending)
                },
            )
            return@map
        }
        SortItem(
            label = stringResource(titleRes),
            sortDescending = sortDescending.takeIf { sortingMode == mode },
            onClick = {
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    else -> if (sortDescending) {
                        LibrarySort.Direction.Descending
                    } else {
                        LibrarySort.Direction.Ascending
                    }
                }
                screenModel.setSort(category, mode, direction)
            },
        )
    }
}

private val displayModes = listOf(
    MR.strings.action_display_grid to LibraryDisplayMode.CompactGrid,
    MR.strings.action_display_comfortable_grid to LibraryDisplayMode.ComfortableGrid,
    MR.strings.action_display_cover_only_grid to LibraryDisplayMode.CoverOnlyGrid,
    MR.strings.action_display_list to LibraryDisplayMode.List,
)

@Composable
private fun ColumnScope.DisplayPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val displayMode by screenModel.libraryPreferences.displayMode().collectAsState()
    SettingsChipRow(MR.strings.action_display_mode) {
        displayModes.map { (titleRes, mode) ->
            FilterChip(
                selected = displayMode == mode,
                onClick = { screenModel.setDisplayMode(mode) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (displayMode != LibraryDisplayMode.List) {
        val configuration = LocalConfiguration.current
        val columnPreference = remember {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                screenModel.libraryPreferences.landscapeColumns()
            } else {
                screenModel.libraryPreferences.portraitColumns()
            }
        }

        val columns by columnPreference.collectAsState()
        SliderItem(
            value = columns,
            valueRange = 0..10,
            label = stringResource(MR.strings.pref_library_columns),
            valueString = if (columns > 0) {
                columns.toString()
            } else {
                stringResource(MR.strings.label_auto)
            },
            onChange = columnPreference::set,
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }

    HeadingItem(MR.strings.overlay_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_download_badge),
        pref = screenModel.libraryPreferences.downloadBadge(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_unread_badge),
        pref = screenModel.libraryPreferences.unreadBadge(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_local_badge),
        pref = screenModel.libraryPreferences.localBadge(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_language_badge),
        pref = screenModel.libraryPreferences.languageBadge(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_source_badge),
        pref = screenModel.libraryPreferences.sourceBadge(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_continue_reading_button),
        pref = screenModel.libraryPreferences.showContinueReadingButton(),
    )

    HeadingItem(MR.strings.tabs_header)
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_tabs),
        pref = screenModel.libraryPreferences.categoryTabs(),
    )
    CheckboxItem(
        label = stringResource(MR.strings.action_display_show_number_of_items),
        pref = screenModel.libraryPreferences.categoryNumberOfItems(),
    )
}

// SY -->
data class GroupMode(
    val int: Int,
    val nameRes: StringResource,
    val drawableRes: Int,
)

private fun groupTypeDrawableRes(type: Int): Int {
    return when (type) {
        LibraryGroup.BY_STATUS -> R.drawable.ic_progress_clock_24dp
        LibraryGroup.BY_TRACK_STATUS -> R.drawable.ic_sync_24dp
        LibraryGroup.BY_SOURCE -> R.drawable.ic_browse_filled_24dp
        LibraryGroup.UNGROUPED -> R.drawable.ic_ungroup_24dp
        else -> R.drawable.ic_label_24dp
    }
}

@Composable
private fun ColumnScope.GroupPage(
    screenModel: LibrarySettingsScreenModel,
    hasCategories: Boolean,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    val groups = remember(hasCategories, trackers) {
        buildList {
            add(LibraryGroup.BY_DEFAULT)
            add(LibraryGroup.BY_SOURCE)
            add(LibraryGroup.BY_STATUS)
            if (trackers.isNotEmpty()) {
                add(LibraryGroup.BY_TRACK_STATUS)
            }
            if (hasCategories) {
                add(LibraryGroup.UNGROUPED)
            }
        }.map {
            GroupMode(
                it,
                LibraryGroup.groupTypeStringRes(it, hasCategories),
                groupTypeDrawableRes(it),
            )
        }.toImmutableList()
    }

    groups.fastForEach {
        IconItem(
            label = stringResource(it.nameRes),
            icon = painterResource(it.drawableRes),
            selected = it.int == screenModel.grouping,
            onClick = {
                screenModel.setGrouping(it.int)
            },
        )
    }
}
// SY <--
