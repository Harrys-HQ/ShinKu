package com.shinku.reader.presentation.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.data.download.model.Download
import com.shinku.reader.domain.updates.model.UpdatesWithRelations
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.relativeDateText
import com.shinku.reader.presentation.core.components.ListGroupHeader
import com.shinku.reader.presentation.core.components.material.DISABLED_ALPHA
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.pluralStringResource
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.manga.components.ChapterDownloadAction
import com.shinku.reader.presentation.manga.components.ChapterDownloadIndicator
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.util.animateItemFastScroll
import com.shinku.reader.presentation.util.relativeTimeSpanString
import com.shinku.reader.ui.updates.UpdatesItem

internal fun LazyListScope.updatesLastUpdatedItem(
    lastUpdated: Long,
    onCalendarClicked: (() -> Unit)? = null,
    onUpdateLibrary: (() -> Unit)? = null,
) {
    item(key = "updates-lastUpdated") {
        Column(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onCalendarClicked != null || onUpdateLibrary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onCalendarClicked != null) {
                        Surface(
                            modifier = Modifier
                                .clip(SanctuaryShapes.medium)
                                .clickable(onClick = onCalendarClicked),
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                            border = SanctuaryBrushes.subtleBorder(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(MR.strings.action_view_upcoming),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                    if (onUpdateLibrary != null) {
                        Surface(
                            modifier = Modifier
                                .clip(SanctuaryShapes.medium)
                                .clickable(onClick = onUpdateLibrary),
                            shape = SanctuaryShapes.medium,
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                            border = SanctuaryBrushes.subtleBorder(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(MR.strings.action_webview_refresh),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

internal fun LazyListScope.updatesUiItems(
    uiModels: List<UpdatesUiModel>,
    expandedGroups: Set<Long>,
    onToggleGroup: (Long) -> Unit,
    selectionMode: Boolean,
    // SY -->
    preserveReadingPosition: Boolean,
    // SY <--
    onUpdateSelected: (UpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    uiModels.forEach { model ->
        when (model) {
            is UpdatesUiModel.Header -> {
                item(key = "updates-header-${model.date}", contentType = "header") {
                    ListGroupHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        text = relativeDateText(model.date),
                    )
                }
            }
            is UpdatesUiModel.Group -> {
                val hasMore = model.chapters.size > 1
                // Selection can include a range or all chapters, so every selected row stays visible.
                val expanded = hasMore && (selectionMode || model.id in expandedGroups)
                val visible = if (expanded) model.chapters else model.chapters.subList(0, 1)
                items(
                    count = visible.size,
                    key = { index -> "updates-chapter-${visible[index].update.chapterId}" },
                    contentType = { index -> if (index == 0) "series" else "chapter" },
                ) { index ->
                    val chapter = visible[index]
                    val isFirst = index == 0
                    val isLast = index == visible.lastIndex
                    val update = chapter.update
                    UpdatesUiItem(
                        modifier = Modifier.animateItemFastScroll(),
                        update = update,
                        isFirst = isFirst,
                        isLast = isLast,
                        selected = chapter.selected,
                        selectionMode = selectionMode,
                        readProgress = update.lastPageRead
                            .takeIf { it > 0L && (!update.read || preserveReadingPosition && chapter.isEhBasedUpdate()) }
                            ?.let { stringResource(MR.strings.chapter_progress, it + 1) },
                        onLongClick = { onUpdateSelected(chapter, !chapter.selected, true, true) },
                        onClick = {
                            if (selectionMode) onUpdateSelected(chapter, !chapter.selected, true, false)
                            else onClickUpdate(chapter)
                        },
                        onClickCover = { onClickCover(chapter) }.takeIf { isFirst && !selectionMode },
                        onDownloadChapter = { action: ChapterDownloadAction ->
                            onDownloadChapter(listOf(chapter), action)
                        }.takeIf { !selectionMode },
                        downloadStateProvider = chapter.downloadStateProvider,
                        downloadProgressProvider = chapter.downloadProgressProvider,
                        groupToggle = if (isFirst && hasMore && !selectionMode) {
                            {
                                UpdatesGroupToggle(model.chapters.size - 1, expanded) { onToggleGroup(model.id) }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatesGroupToggle(additionalChapters: Int, expanded: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = if (expanded) {
                stringResource(MR.strings.updates_hide_chapters)
            } else {
                pluralStringResource(MR.plurals.updates_show_more_chapters, additionalChapters, additionalChapters)
            },
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun UpdatesUiItem(
    update: UpdatesWithRelations,
    isFirst: Boolean,
    isLast: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    readProgress: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadChapter: ((ChapterDownloadAction) -> Unit)?,
    // Download Indicator
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    groupToggle: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = if (update.read) DISABLED_ALPHA else 1f
    val cardShape = SanctuaryShapes.deckCard
    val shape = RoundedCornerShape(
        topStart = if (isFirst) cardShape.topStart else CornerSize(0.dp),
        topEnd = if (isFirst) cardShape.topEnd else CornerSize(0.dp),
        bottomStart = if (isLast) cardShape.bottomStart else CornerSize(0.dp),
        bottomEnd = if (isLast) cardShape.bottomEnd else CornerSize(0.dp),
    )
    val standaloneCard = isFirst && isLast

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium)
            .padding(top = if (isFirst) 4.dp else 0.dp, bottom = if (isLast) 4.dp else 0.dp),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
        },
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else if (standaloneCard) {
            SanctuaryBrushes.subtleBorder()
        } else {
            null
        },
        shadowElevation = if (standaloneCard) SanctuaryElevation.subtle else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .semantics { this.selected = selected }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        onLongClick()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFirst) MangaCover.Book(
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(SanctuaryShapes.medium),
                data = update.coverData,
                contentDescription = update.mangaTitle,
                onClick = onClickCover,
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
            ) {
                if (isFirst) Text(
                    text = update.mangaTitle,
                    maxLines = 3,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var textHeight by remember { mutableIntStateOf(0) }
                    if (update.bookmark) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                            modifier = Modifier
                                .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Surface(
                        shape = SanctuaryShapes.tag,
                        color = if (!update.read) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)
                        },
                        modifier = Modifier.weight(weight = 1f, fill = false),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!update.read) {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = stringResource(MR.strings.unread),
                                    modifier = Modifier
                                        .height(7.dp)
                                        .padding(end = 5.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = update.chapterName,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (!update.read) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = if (!update.read) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    LocalContentColor.current.copy(alpha = textAlpha)
                                },
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textHeight = it.size.height },
                            )
                        }
                    }
                }
                if (readProgress != null) {
                    Text(
                        text = readProgress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (!selectionMode) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            onClick = onClick,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = SanctuaryShapes.medium,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                stringResource(
                                    if (readProgress != null) MR.strings.action_resume else MR.strings.action_read_chapter,
                                ),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        ChapterDownloadIndicator(
                            enabled = onDownloadChapter != null,
                            downloadStateProvider = downloadStateProvider,
                            downloadProgressProvider = downloadProgressProvider,
                            onClick = { onDownloadChapter?.invoke(it) },
                        )
                    }
                }
                groupToggle?.invoke()
            }

            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            }
        }
    }
}
