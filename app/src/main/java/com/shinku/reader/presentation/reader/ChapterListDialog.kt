package com.shinku.reader.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.data.download.DownloadManager
import com.shinku.reader.data.download.model.Download
import com.shinku.reader.domain.chapter.model.Chapter
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.exh.metadata.MetadataUtil
import com.shinku.reader.exh.source.isEhBasedManga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AdaptiveSheet
import com.shinku.reader.presentation.components.TabbedDialog
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.manga.components.MangaChapterListItem
import com.shinku.reader.ui.reader.chapter.ReaderChapterItem
import com.shinku.reader.ui.reader.setting.ReaderSettingsScreenModel
import com.shinku.reader.util.lang.toRelativeString
import eu.kanade.tachiyomi.source.local.isLocal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Chapter) -> Unit,
    onBookmark: (Chapter) -> Unit,
    dateRelativeTime: Boolean,
    currentPage: Int,
    totalPages: Int,
    onClickPage: (Int) -> Unit,
) {
    val manga by screenModel.mangaFlow.collectAsState()
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val downloadQueueState by downloadManager.queueState.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    val filteredChapters = remember(chapters, query) {
        chapters.filter { it.chapter.name.contains(query, ignoreCase = true) }
    }
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = persistentListOf(stringResource(MR.strings.chapters), stringResource(MR.strings.reader_pages)),
        header = {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Navigate",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Browse chapters or jump directly to a page",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        footer = {
            Button(
                onClick = onDismissRequest,
                shape = SanctuaryShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(MR.strings.action_done))
            }
        },
    ) { tab ->
        if (tab == 1) {
            val pageState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                initialFirstVisibleItemIndex = (currentPage - 1).coerceAtLeast(0),
            )
            androidx.compose.runtime.LaunchedEffect(currentPage, totalPages, pageState.layoutInfo.viewportSize) {
                if (totalPages > 0) pageState.scrollToItem((currentPage - 1).coerceIn(0, totalPages - 1))
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                state = pageState,
                modifier = Modifier.heightIn(max = 420.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            ) {
                items(totalPages.coerceAtLeast(0)) { index ->
                    FilterChip(
                        selected = index + 1 == currentPage,
                        onClick = {
                            onClickPage(index)
                            onDismissRequest()
                        },
                        label = { Text("${index + 1}") },
                        shape = SanctuaryShapes.pill,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        } else {
            Column(Modifier.heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    shape = SanctuaryShapes.medium,
                    label = { Text(stringResource(MR.strings.action_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                LazyColumn(
                    state = state,
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    items(
                        items = filteredChapters,
                        key = { "chapter-${it.chapter.id}" },
                    ) { chapterItem ->
                        val activeDownload = downloadQueueState.find { it.chapter.id == chapterItem.chapter.id }
                        val progress = activeDownload?.let {
                            downloadManager.progressFlow()
                                .filter { it.chapter.id == chapterItem.chapter.id }
                                .map { it.progress }
                                .collectAsState(0).value
                        } ?: 0
                        val downloaded = if (chapterItem.manga.isLocal()) {
                            true
                        } else {
                            downloadManager.isChapterDownloaded(
                                chapterItem.chapter.name,
                                chapterItem.chapter.scanlator,
                                chapterItem.chapter.url,
                                chapterItem.manga.ogTitle,
                                chapterItem.manga.source,
                            )
                        }
                        val downloadState = when {
                            activeDownload != null -> activeDownload.status
                            downloaded -> Download.State.DOWNLOADED
                            else -> Download.State.NOT_DOWNLOADED
                        }
                        if (chapterItem.isCurrent) {
                            Surface(
                                shape = SanctuaryShapes.tag,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    stringResource(MR.strings.reader_current_chapter),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        MangaChapterListItem(
                            title = chapterItem.chapter.name,
                            date = chapterItem.chapter.dateUpload
                                .takeIf { it > 0L }
                                ?.let {
                                    // SY -->
                                    if (manga?.isEhBasedManga() == true) {
                                        MetadataUtil.EX_DATE_FORMAT
                                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                                    } else {
                                        LocalDate.ofInstant(
                                            Instant.ofEpochMilli(it),
                                            ZoneId.systemDefault(),
                                        ).toRelativeString(context, dateRelativeTime, chapterItem.dateFormat)
                                    }
                                    // SY <--
                                },
                            readProgress = if (chapterItem.isCurrent && totalPages > 0) "$currentPage / $totalPages" else null,
                            scanlator = chapterItem.chapter.scanlator,
                            sourceName = null,
                            read = chapterItem.chapter.read,
                            bookmark = chapterItem.chapter.bookmark,
                            selected = false,
                            downloadIndicatorEnabled = false,
                            downloadStateProvider = { downloadState },
                            downloadProgressProvider = { progress },
                            chapterSwipeStartAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                            chapterSwipeEndAction = LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
                            onLongClick = { onBookmark(chapterItem.chapter) },
                            onClick = { onClickChapter(chapterItem.chapter) },
                            onDownloadClick = null,
                            onChapterSwipe = {
                                onBookmark(chapterItem.chapter)
                            },
                        )
                    }
                }
            }
        }
    }
}
