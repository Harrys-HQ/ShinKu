package com.shinku.reader.presentation.library.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.library.model.LibraryManga
import com.shinku.reader.domain.manga.model.asMangaCover
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.screens.EmptyScreen
import com.shinku.reader.presentation.core.screens.EmptyScreenAction
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.ui.library.ActiveMergeGroupUi
import com.shinku.reader.ui.library.DuplicateMangaGroup
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuplicatesSection(
    groups: ImmutableList<DuplicateMangaGroup>,
    ignoredDuplicateCount: Int,
    getSourceName: (Long) -> String,
    onOpenManga: (LibraryManga) -> Unit,
    onGroupAll: (DuplicateMangaGroup) -> Unit,
    onIgnore: (DuplicateMangaGroup) -> Unit,
    onRequestResetIgnored: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        val actions = if (ignoredDuplicateCount > 0) {
            persistentListOf(
                EmptyScreenAction(
                    stringRes = MR.strings.library_management_action_restore_ignored,
                    icon = Icons.Outlined.Refresh,
                    onClick = onRequestResetIgnored,
                ),
            )
        } else {
            null
        }
        EmptyScreen(
            stringRes = MR.strings.library_management_empty_duplicates,
            modifier = modifier.fillMaxSize(),
            actions = actions,
        )
        return
    }

    ManagementSectionList(modifier = modifier) {
        items(groups, key = { it.normalizedTitle }) { group ->
            ManagementSectionCard {
                Text(
                    text = group.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                group.entries.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ManagementEntryRow(
                        entry = entry,
                        title = getSourceName(entry.manga.source),
                        subtitle = stringResource(
                            MR.strings.library_management_chapters_read_summary,
                            entry.totalChapters,
                            entry.readCount,
                        ),
                        titleMaxLines = 1,
                        onClick = { onOpenManga(entry) },
                    ) {
                        TextButton(
                            onClick = { onOpenManga(entry) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(text = stringResource(MR.strings.library_management_action_open))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onIgnore(group) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(MR.strings.library_management_action_ignore))
                    }
                    FilledTonalButton(
                        onClick = { onGroupAll(group) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(MR.strings.library_management_action_group, group.entries.size))
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveMergeGroupsSection(
    groups: ImmutableList<ActiveMergeGroupUi>,
    getSourceName: (Long) -> String,
    onOpenManga: (LibraryManga) -> Unit,
    onRequestRemoveMember: (LibraryManga) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        EmptyScreen(stringRes = MR.strings.library_management_empty_groups, modifier = modifier.fillMaxSize())
        return
    }

    ManagementSectionList(modifier = modifier) {
        items(groups, key = { it.groupId }) { group ->
            ManagementSectionCard {
                Text(
                    text = group.members.firstOrNull()?.manga?.title
                        ?: stringResource(MR.strings.library_management_group_fallback, group.groupId),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                group.members.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ManagementEntryRow(
                        entry = member,
                        title = member.manga.title,
                        subtitle = stringResource(
                            MR.strings.library_management_source_chapters_summary,
                            getSourceName(member.manga.source),
                            member.totalChapters,
                        ),
                        titleMaxLines = 2,
                        onClick = { onOpenManga(member) },
                    ) {
                        IconButton(
                            onClick = { onRequestRemoveMember(member) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(MR.strings.library_management_action_remove_member),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnavailableSourcesSection(
    entries: ImmutableList<LibraryManga>,
    getSourceName: (Long) -> String,
    onOpenManga: (LibraryManga) -> Unit,
    onMigrateManga: (LibraryManga) -> Unit,
    onMigrateAll: (List<LibraryManga>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        EmptyScreen(stringRes = MR.strings.library_management_empty_missing, modifier = modifier.fillMaxSize())
        return
    }

    ManagementSectionList(modifier = modifier) {
        if (entries.size > 1) {
            item(key = "migrate-all-header") {
                FilledTonalButton(
                    onClick = { onMigrateAll(entries) },
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(MR.strings.library_management_action_migrate_all, entries.size))
                }
            }
        }

        items(entries, key = { it.id }) { entry ->
            ManagementSectionCard(onClick = { onOpenManga(entry) }) {
                ManagementEntryRow(
                    entry = entry,
                    title = entry.manga.title,
                    subtitle = stringResource(
                        MR.strings.library_management_source_chapters_summary,
                        getSourceName(entry.manga.source),
                        entry.totalChapters,
                    ),
                    subtitleColor = MaterialTheme.colorScheme.error,
                    titleMaxLines = 2,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalButton(
                        onClick = { onMigrateManga(entry) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(MR.strings.library_management_action_migrate))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementSectionList(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun ManagementSectionCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(SanctuaryShapes.deckCard)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ManagementEntryRow(
    entry: LibraryManga,
    title: String,
    subtitle: String,
    titleMaxLines: Int,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(SanctuaryShapes.tag)
                        .clickable(onClick = onClick)
                        .padding(vertical = 4.dp)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            data = entry.manga.asMangaCover(),
            modifier = Modifier.width(40.dp),
            shape = SanctuaryShapes.tag,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}
