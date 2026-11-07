package com.shinku.reader.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.shinku.reader.R
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import com.shinku.reader.presentation.components.DownloadDropdownMenu
import com.shinku.reader.presentation.components.DropdownMenu
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.manga.DownloadAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun MangaBottomActionMenu(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onBookmarkClicked: (() -> Unit)? = null,
    onRemoveBookmarkClicked: (() -> Unit)? = null,
    onMarkAsReadClicked: (() -> Unit)? = null,
    onMarkAsUnreadClicked: (() -> Unit)? = null,
    onMarkPreviousAsReadClicked: (() -> Unit)? = null,
    onDownloadClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        val scope = rememberCoroutineScope()
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val haptic = LocalHapticFeedback.current
            val confirm = remember { mutableStateListOf(false, false, false, false, false, false, false) }
            var resetJob: Job? = remember { null }
            val onLongClickItem: (Int) -> Unit = { toConfirmIndex ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                (0..<7).forEach { i -> confirm[i] = i == toConfirmIndex }
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1.seconds)
                    if (isActive) confirm[toConfirmIndex] = false
                }
            }
            Row(
                modifier = Modifier
                    .padding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                if (onBookmarkClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_bookmark),
                        icon = Icons.Outlined.BookmarkAdd,
                        toConfirm = confirm[0],
                        onLongClick = { onLongClickItem(0) },
                        onClick = onBookmarkClicked,
                    )
                }
                if (onRemoveBookmarkClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_remove_bookmark),
                        icon = Icons.Outlined.BookmarkRemove,
                        toConfirm = confirm[1],
                        onLongClick = { onLongClickItem(1) },
                        onClick = onRemoveBookmarkClicked,
                    )
                }
                if (onMarkAsReadClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_mark_as_read),
                        icon = Icons.Outlined.DoneAll,
                        toConfirm = confirm[2],
                        onLongClick = { onLongClickItem(2) },
                        onClick = onMarkAsReadClicked,
                    )
                }
                if (onMarkAsUnreadClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_mark_as_unread),
                        icon = Icons.Outlined.RemoveDone,
                        toConfirm = confirm[3],
                        onLongClick = { onLongClickItem(3) },
                        onClick = onMarkAsUnreadClicked,
                    )
                }
                if (onMarkPreviousAsReadClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_mark_previous_as_read),
                        icon = ImageVector.vectorResource(R.drawable.ic_done_prev_24dp),
                        toConfirm = confirm[4],
                        onLongClick = { onLongClickItem(4) },
                        onClick = onMarkPreviousAsReadClicked,
                    )
                }
                if (onDownloadClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_download),
                        icon = Icons.Outlined.Download,
                        toConfirm = confirm[5],
                        onLongClick = { onLongClickItem(5) },
                        onClick = onDownloadClicked,
                    )
                }
                if (onDeleteClicked != null) {
                    Button(
                        title = stringResource(MR.strings.action_delete),
                        icon = Icons.Outlined.Delete,
                        toConfirm = confirm[6],
                        onLongClick = { onLongClickItem(6) },
                        onClick = onDeleteClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Button(
    title: String,
    icon: ImageVector,
    toConfirm: Boolean,
    showLabel: Boolean = false,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    val animatedWeight by animateFloatAsState(
        targetValue = if (toConfirm && !showLabel) 2f else 1f,
        label = "weight",
    )
    Box(
        modifier = Modifier
            .then(if (showLabel) Modifier.heightIn(min = 64.dp) else Modifier.size(48.dp))
            .weight(animatedWeight)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (showLabel) null else title,
            )
            AnimatedVisibility(
                visible = toConfirm || showLabel,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Visible,
                    maxLines = if (showLabel) Int.MAX_VALUE else 2,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        content?.invoke()
    }
}

@Composable
fun LibraryBottomActionMenu(
    visible: Boolean,
    onChangeCategoryClicked: () -> Unit,
    onMarkAsReadClicked: () -> Unit,
    onMarkAsUnreadClicked: () -> Unit,
    onDownloadClicked: ((DownloadAction) -> Unit)?,
    onDeleteClicked: () -> Unit,
    onMigrateClicked: (() -> Unit)?,
    // SY -->
    onClickCleanTitles: (() -> Unit)?,
    onClickCollectRecommendations: (() -> Unit)?,
    onClickAddToMangaDex: (() -> Unit)?,
    onClickResetInfo: (() -> Unit)?,
    // SY <--
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 300)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        Surface(
            modifier = modifier,
            shape = com.shinku.reader.presentation.core.theme.SanctuaryShapes.hero,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = com.shinku.reader.presentation.core.theme.SanctuaryBrushes.subtleBorder(),
        ) {
            var overflowOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .background(com.shinku.reader.presentation.core.theme.SanctuaryBrushes.compactHorizontalGradient())
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDownloadClicked != null) {
                    var downloadExpanded by remember { mutableStateOf(false) }
                    Button(
                        showLabel = true,
                        title = stringResource(MR.strings.action_download),
                        icon = Icons.Outlined.Download,
                        toConfirm = false,
                        onLongClick = {},
                        onClick = { downloadExpanded = true },
                    ) {
                        DownloadDropdownMenu(
                            expanded = downloadExpanded,
                            onDismissRequest = { downloadExpanded = false },
                            onDownloadClicked = onDownloadClicked,
                            offset = BottomBarMenuDpOffset,
                        )
                    }
                    VerticalDivider(Modifier.height(40.dp))
                }
                Button(
                    showLabel = true,
                    title = stringResource(MR.strings.library_bulk_category),
                    icon = Icons.Outlined.Folder,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onChangeCategoryClicked,
                )
                VerticalDivider(Modifier.height(40.dp))
                Button(
                    showLabel = true,
                    title = stringResource(MR.strings.action_mark_as_read),
                    icon = Icons.Outlined.CheckCircleOutline,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = onMarkAsReadClicked,
                )
                VerticalDivider(Modifier.height(40.dp))
                Button(
                    showLabel = true,
                    title = stringResource(MR.strings.label_more),
                    icon = Icons.Outlined.MoreVert,
                    toConfirm = false,
                    onLongClick = {},
                    onClick = { overflowOpen = true },
                ) {
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.action_mark_as_unread)) },
                            leadingIcon = { Icon(Icons.Outlined.RemoveDone, contentDescription = null) },
                            onClick = {
                                overflowOpen = false
                                onMarkAsUnreadClicked()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.action_delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = {
                                overflowOpen = false
                                onDeleteClicked()
                            },
                        )
                        if (onClickCleanTitles != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(SYMR.strings.action_clean_titles)) },
                                onClick = {
                                    overflowOpen = false
                                    onClickCleanTitles()
                                },
                            )
                        }
                        if (onMigrateClicked != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.migrate)) },
                                onClick = {
                                    overflowOpen = false
                                    onMigrateClicked()
                                },
                            )
                        }
                        if (onClickCollectRecommendations != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(SYMR.strings.rec_search_short)) },
                                onClick = {
                                    overflowOpen = false
                                    onClickCollectRecommendations()
                                },
                            )
                        }
                        if (onClickAddToMangaDex != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(SYMR.strings.mangadex_add_to_follows)) },
                                onClick = {
                                    overflowOpen = false
                                    onClickAddToMangaDex()
                                },
                            )
                        }
                        if (onClickResetInfo != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(SYMR.strings.reset_info)) },
                                onClick = {
                                    overflowOpen = false
                                    onClickResetInfo()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val BottomBarMenuDpOffset = DpOffset(0.dp, 0.dp)
