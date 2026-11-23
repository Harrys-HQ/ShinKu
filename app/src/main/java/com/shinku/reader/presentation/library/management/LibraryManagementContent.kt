package com.shinku.reader.presentation.library.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.LoadingScreen
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.ui.library.DuplicateMangaGroup
import com.shinku.reader.ui.library.LibraryManagementScreenModel
import kotlinx.collections.immutable.persistentListOf

enum class LibraryManagementTab {
    DUPLICATES,
    GROUPS,
    MISSING,
}

@Composable
fun LibraryManagementContent(
    state: LibraryManagementScreenModel.State,
    selectedTab: LibraryManagementTab,
    onSelectTab: (LibraryManagementTab) -> Unit,
    onNavigateUp: () -> Unit,
    getSourceName: (Long) -> String,
    onOpenManga: (LibraryManga) -> Unit,
    onGroupAll: (DuplicateMangaGroup) -> Unit,
    onIgnoreDuplicateGroup: (DuplicateMangaGroup) -> Unit,
    onRequestRemoveMember: (LibraryManga) -> Unit,
    onRequestResetIgnored: () -> Unit,
    onMigrateManga: (LibraryManga) -> Unit,
    onMigrateAll: (List<LibraryManga>) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.library_management_title),
                navigateUp = onNavigateUp,
                actions = {
                    if (state.ignoredDuplicateCount > 0) {
                        AppBarActions(
                            persistentListOf(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.library_management_action_restore_ignored),
                                    onClick = onRequestResetIgnored,
                                ),
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            LoadingScreen(Modifier.padding(paddingValues))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LibraryManagementTabRow(
                selectedTab = selectedTab,
                duplicateCount = state.duplicateGroups.size,
                groupCount = state.activeMergeGroups.size,
                missingCount = state.unavailableSourceEntries.size,
                onSelectTab = onSelectTab,
            )

            when (selectedTab) {
                LibraryManagementTab.DUPLICATES -> DuplicatesSection(
                    groups = state.duplicateGroups,
                    ignoredDuplicateCount = state.ignoredDuplicateCount,
                    getSourceName = getSourceName,
                    onOpenManga = onOpenManga,
                    onGroupAll = onGroupAll,
                    onIgnore = onIgnoreDuplicateGroup,
                    onRequestResetIgnored = onRequestResetIgnored,
                    modifier = Modifier.weight(1f),
                )
                LibraryManagementTab.GROUPS -> ActiveMergeGroupsSection(
                    groups = state.activeMergeGroups,
                    getSourceName = getSourceName,
                    onOpenManga = onOpenManga,
                    onRequestRemoveMember = onRequestRemoveMember,
                    modifier = Modifier.weight(1f),
                )
                LibraryManagementTab.MISSING -> UnavailableSourcesSection(
                    entries = state.unavailableSourceEntries,
                    getSourceName = getSourceName,
                    onOpenManga = onOpenManga,
                    onMigrateManga = onMigrateManga,
                    onMigrateAll = onMigrateAll,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LibraryManagementTabRow(
    selectedTab: LibraryManagementTab,
    duplicateCount: Int,
    groupCount: Int,
    missingCount: Int,
    onSelectTab: (LibraryManagementTab) -> Unit,
) {
    val tabs = listOf(
        Triple(LibraryManagementTab.DUPLICATES, stringResource(MR.strings.library_management_tab_duplicates), duplicateCount),
        Triple(LibraryManagementTab.GROUPS, stringResource(MR.strings.library_management_tab_groups), groupCount),
        Triple(LibraryManagementTab.MISSING, stringResource(MR.strings.library_management_tab_missing), missingCount),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, label, count) ->
            val isSelected = selectedTab == tab
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Surface(
                shape = SanctuaryShapes.medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
                },
                border = if (isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    SanctuaryBrushes.subtleBorder()
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(SanctuaryShapes.medium)
                    .semantics {
                        this.selected = isSelected
                        this.role = Role.Tab
                    }
                    .clickable { onSelectTab(tab) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = contentColor,
                        maxLines = 1,
                    )
                    Pill(
                        text = count.toString(),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
