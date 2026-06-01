package com.shinku.reader.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import coil3.asDrawable
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shinku.reader.R
import com.shinku.reader.data.download.model.Download
import com.shinku.reader.domain.chapter.service.calculateChapterGap
import com.shinku.reader.domain.chapter.service.getChapterSort
import com.shinku.reader.domain.chapter.service.missingChaptersCount
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.exh.metadata.MetadataUtil
import com.shinku.reader.exh.source.MERGED_SOURCE_ID
import com.shinku.reader.exh.source.getMainSource
import com.shinku.reader.exh.ui.metadata.adapters.EightMusesDescription
import com.shinku.reader.exh.ui.metadata.adapters.HBrowseDescription
import com.shinku.reader.exh.ui.metadata.adapters.LanraragiDescription
import com.shinku.reader.exh.ui.metadata.adapters.MangaDexDescription
import com.shinku.reader.exh.ui.metadata.adapters.NHentaiDescription
import com.shinku.reader.exh.ui.metadata.adapters.PururinDescription
import com.shinku.reader.exh.ui.metadata.adapters.TsuminoDescription
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.DynamicBackdrop
import com.shinku.reader.presentation.components.relativeDateText
import com.shinku.reader.presentation.core.components.TwoPanelBox
import com.shinku.reader.presentation.core.components.VerticalFastScroller
import com.shinku.reader.presentation.core.components.material.PullRefresh
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.shouldExpandFAB
import com.shinku.reader.presentation.manga.components.ChapterDownloadAction
import com.shinku.reader.presentation.manga.components.ChapterHeader
import com.shinku.reader.presentation.manga.components.ExpandableMangaDescription
import com.shinku.reader.presentation.manga.components.MangaActionRow
import com.shinku.reader.presentation.manga.components.MangaBottomActionMenu
import com.shinku.reader.presentation.manga.components.MangaChapterListItem
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.manga.components.MangaInfoBox
import com.shinku.reader.presentation.manga.components.MangaInfoButtons
import com.shinku.reader.presentation.manga.components.MangaToolbar
import com.shinku.reader.presentation.manga.components.MissingChapterCountListItem
import com.shinku.reader.presentation.manga.components.PagePreviewItems
import com.shinku.reader.presentation.manga.components.PagePreviews
import com.shinku.reader.presentation.manga.components.SearchMetadataChips
import com.shinku.reader.presentation.theme.DynamicThemeProvider
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.presentation.util.formatChapterNumber
import com.shinku.reader.ui.manga.ChapterList
import com.shinku.reader.ui.manga.MangaScreenModel
import com.shinku.reader.ui.manga.MergedMangaData
import com.shinku.reader.ui.manga.PagePreviewState
import com.shinku.reader.util.system.copyToClipboard
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.local.isLocal
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.Lanraragi
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun MangaScreen(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (com.shinku.reader.domain.chapter.model.Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onVibeClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onOpenPagePreview: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    previewsRowCount: Int,
    onMultiBookmarkClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (com.shinku.reader.domain.chapter.model.Chapter) -> Unit,
    onMultiDeleteClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>) -> Unit,
    onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onChapterSelected: (ChapterList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onGenerateRecap: (() -> Unit)? = null,
    onDismissRecap: (() -> Unit)? = null,
) {
    if (!isTabletUi) {
        MangaScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMetadataViewerClicked = onMetadataViewerClicked,
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onVibeClicked = onVibeClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            onOpenPagePreview = onOpenPagePreview,
            onMorePreviewsClicked = onMorePreviewsClicked,
            previewsRowCount = previewsRowCount,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            onGenerateRecap = onGenerateRecap,
            onDismissRecap = onDismissRecap,
        )
    } else {
        // Implement Tablet UI if needed, for now reuse small
        MangaScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMetadataViewerClicked = onMetadataViewerClicked,
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onVibeClicked = onVibeClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            onOpenPagePreview = onOpenPagePreview,
            onMorePreviewsClicked = onMorePreviewsClicked,
            previewsRowCount = previewsRowCount,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            onGenerateRecap = onGenerateRecap,
            onDismissRecap = onDismissRecap,
        )
    }
}

@Composable
private fun MangaScreenSmallImpl(
    state: MangaScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (com.shinku.reader.domain.chapter.model.Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onVibeClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onOpenPagePreview: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    previewsRowCount: Int,
    onMultiBookmarkClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (com.shinku.reader.domain.chapter.model.Chapter) -> Unit,
    onMultiDeleteClicked: (List<com.shinku.reader.domain.chapter.model.Chapter>) -> Unit,
    onChapterSwipe: (ChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onChapterSelected: (ChapterList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onGenerateRecap: (() -> Unit)? = null,
    onDismissRecap: (() -> Unit)? = null,
) {
    val chapterListState = rememberLazyListState()

    val (chapters, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedChapters,
            second = state.chapterListItems,
            third = state.isAnySelected,
        )
    }
    var maxWidth by remember { mutableStateOf(Dp.Hairline) }

    BackHandler(enabled = isAnySelected) {
        onAllChapterSelected(false)
    }

    Scaffold(
        topBar = {
            val selectedChapterCount: Int = remember(chapters) {
                chapters.count { it.selected }
            }
            MangaToolbar(
                title = state.manga.title,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                onClickEditNotes = onEditNotesClicked,
                onClickEditInfo = onEditInfoClicked.takeIf { state.manga.favorite },
                onClickRecommend = onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
                onClickMergedSettings = onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
                onClickMerge = onMergeClicked.takeIf { state.showMergeInOverflow },
                actionModeCounter = selectedChapterCount,
                onCancelActionMode = { onAllChapterSelected(false) },
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = { onInvertSelection() },
                titleAlphaProvider = { 1f },
                backgroundAlphaProvider = { 1f },
            )
        },
        bottomBar = {
            val selectedChapters = remember(chapters) {
                chapters.filter { it.selected }
            }
            MangaBottomActionMenu(
                visible = isAnySelected,
                onBookmarkClicked = {
                    onMultiBookmarkClicked(selectedChapters.map { it.chapter }, true)
                },
                onRemoveBookmarkClicked = {
                    onMultiBookmarkClicked(selectedChapters.map { it.chapter }, false)
                },
                onMarkAsReadClicked = {
                    onMultiMarkAsReadClicked(selectedChapters.map { it.chapter }, true)
                },
                onMarkAsUnreadClicked = {
                    onMultiMarkAsReadClicked(selectedChapters.map { it.chapter }, false)
                },
                onMarkPreviousAsReadClicked = {
                    selectedChapters.firstOrNull()?.let { onMarkPreviousAsReadClicked(it.chapter) }
                },
                onDownloadClicked = onDownloadChapter?.let {
                    { onDownloadChapter(selectedChapters, ChapterDownloadAction.START) }
                },
                onDeleteClicked = {
                    onMultiDeleteClicked(selectedChapters.map { it.chapter })
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = chapters.any { !it.chapter.read } && !isAnySelected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(text = stringResource(MR.strings.action_resume))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    onClick = onContinueReading,
                    expanded = chapterListState.shouldExpandFAB(),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            state = chapterListState,
            contentPadding = contentPadding,
        ) {
            item(key = MangaScreenItem.INFO_BOX) {
                MangaInfoBox(
                    isTabletUi = false,
                    appBarPadding = 0.dp,
                    manga = state.manga,
                    sourceName = remember { state.source.getNameForMangaInfo(state.mergedData?.sources) },
                    isStubSource = remember { state.source is StubSource },
                    onCoverClick = onCoverClicked,
                    doSearch = onSearch,
                )
            }

            item(key = MangaScreenItem.ACTION_ROW) {
                MangaActionRow(
                    favorite = state.manga.favorite,
                    trackingCount = state.trackingCount,
                    nextUpdate = nextUpdate,
                    isUserIntervalMode = state.manga.fetchInterval < 0,
                    onAddToLibraryClicked = onAddToLibraryClicked,
                    onWebViewClicked = onWebViewClicked,
                    onWebViewLongClicked = onWebViewLongClicked,
                    onTrackingClicked = onTrackingClicked,
                    onEditIntervalClicked = onEditIntervalClicked,
                    onEditCategory = onEditCategoryClicked,
                    onMergeClicked = onMergeClicked.takeUnless { state.showMergeInOverflow },
                )
            }

            item(key = MangaScreenItem.DESCRIPTION_WITH_TAG) {
                ExpandableMangaDescription(
                    defaultExpandState = state.isFromSource,
                    description = state.manga.description,
                    tagsProvider = { state.manga.genre },
                    notes = state.manga.notes,
                    onTagSearch = onTagSearch,
                    onCopyTagToClipboard = onCopyTagToClipboard,
                    onEditNotes = onEditNotesClicked,
                    doSearch = onSearch,
                    searchMetadataChips = remember(state.meta, state.source.id, state.manga.genre) {
                        SearchMetadataChips(state.meta, state.source.id, state.manga.genre)
                    },
                )
            }

            if (state.showRecapPrompt) {
                item(key = "ai_chapter_recap_prompt") {
                    androidx.compose.material3.ElevatedCard(
                        modifier = Modifier.padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Welcome Back!",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "It has been over 2 weeks since you last read this manga. Would you like a quick AI summary of what happened in the last read chapters to get you back up to speed?",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            if (state.isLoadingRecap) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else if (state.recapText != null) {
                                Text(
                                    text = state.recapText,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    androidx.compose.material3.TextButton(onClick = { onDismissRecap?.invoke() }) {
                                        Text("Close", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    androidx.compose.material3.TextButton(onClick = { onDismissRecap?.invoke() }) {
                                        Text("Ignore", color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                    }
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Button(
                                        onClick = { onGenerateRecap?.invoke() }
                                    ) {
                                        Text("Generate AI Recap")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.similarVibes.isNotEmpty()) {
                item(key = MangaScreenItem.SIMILAR_VIBES) {
                    SimilarVibes(
                        mangaList = state.similarVibes,
                        onClick = { onSearch(it.title, true) }
                    )
                }
            }

            // Chapter list items implementation
            items(
                items = listItem,
                key = { item ->
                    when (item) {
                        is ChapterList.MissingCount -> "missing-count-${item.id}"
                        is ChapterList.Item -> "chapter-${item.id}"
                    }
                },
                contentType = {
                    when (it) {
                        is ChapterList.MissingCount -> MangaScreenItem.CHAPTER
                        is ChapterList.Item -> MangaScreenItem.CHAPTER
                    }
                },
            ) { item ->
                val haptic = LocalHapticFeedback.current

                when (item) {
                    is ChapterList.MissingCount -> {
                        MissingChapterCountListItem(count = item.count)
                    }
                    is ChapterList.Item -> {
                        MangaChapterListItem(
                            title = if (state.manga.displayMode == com.shinku.reader.domain.manga.model.Manga.CHAPTER_DISPLAY_NUMBER) {
                                stringResource(
                                    MR.strings.display_mode_chapter,
                                    formatChapterNumber(item.chapter.chapterNumber),
                                )
                            } else {
                                item.chapter.name
                            },
                            date = item.chapter.dateUpload
                                .takeIf { it > 0L }
                                ?.let {
                                    relativeDateText(item.chapter.dateUpload)
                                },
                            readProgress = item.chapter.lastPageRead
                                .takeIf {
                                    (!item.chapter.read || state.alwaysShowReadingProgress) && it > 0L
                                }
                                ?.let {
                                    stringResource(
                                        MR.strings.chapter_progress,
                                        it + 1,
                                    )
                                },
                            scanlator = item.chapter.scanlator.takeIf {
                                !it.isNullOrBlank() && item.showScanlator
                            },
                            sourceName = item.sourceName,
                            read = item.chapter.read,
                            bookmark = item.chapter.bookmark,
                            selected = item.selected,
                            downloadIndicatorEnabled =
                            !isAnySelected && !state.manga.isLocal(),
                            downloadStateProvider = { item.downloadState },
                            downloadProgressProvider = { item.downloadProgress },
                            chapterSwipeStartAction = chapterSwipeStartAction,
                            chapterSwipeEndAction = chapterSwipeEndAction,
                            onLongClick = {
                                onChapterSelected(item, !item.selected, true, true)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onClick = {
                                onChapterItemClick(
                                    chapterItem = item,
                                    isAnyChapterSelected = isAnySelected,
                                    onToggleSelection = { onChapterSelected(item, !item.selected, true, false) },
                                    onChapterClicked = onChapterClicked,
                                )
                            },
                            onDownloadClick = if (onDownloadChapter != null) {
                                { onDownloadChapter(listOf(item), it) }
                            } else {
                                null
                            },
                            onChapterSwipe = {
                                onChapterSwipe(item, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun onChapterItemClick(
    chapterItem: ChapterList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: (com.shinku.reader.domain.chapter.model.Chapter) -> Unit,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked(chapterItem.chapter)
    }
}

@Composable
private fun SimilarVibes(
    mangaList: List<Manga>,
    onClick: (Manga) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        Text(
            text = "Similar Vibes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            items(
                items = mangaList,
                key = { it.id },
            ) { manga ->
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onClick(manga) },
                ) {
                    MangaCover.Book(
                        data = manga,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.66f),
                    )
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun metadataDescription(source: eu.kanade.tachiyomi.source.Source): MetadataDescriptionComposable? = null

typealias MetadataDescriptionComposable = @Composable (
    state: MangaScreenModel.State.Success,
    openMetadataViewer: () -> Unit,
    search: (String) -> Unit,
) -> Unit
