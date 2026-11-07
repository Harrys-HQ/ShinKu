package com.shinku.reader.feature.migration.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.shinku.reader.R
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.feature.migration.list.models.MigratingManga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.Badge
import com.shinku.reader.presentation.core.components.BadgeGroup
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.util.formatChapterNumber
import com.shinku.reader.presentation.util.rememberResourceBitmapPainter

@Composable
fun MigrationListItem(
    modifier: Modifier,
    manga: Manga,
    source: String,
    chapterCount: Int,
    latestChapter: Double?,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .widthIn(max = 150.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MangaCover.Book.ratio),
        ) {
            MangaCover.Book(
                modifier = Modifier.fillMaxWidth(),
                data = manga,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    )
                    .fillMaxHeight(0.4f)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
            BadgeGroup(modifier = Modifier.padding(4.dp)) {
                Badge(text = "$chapterCount")
            }
        }
        Text(
            text = manga.title.ifBlank { stringResource(MR.strings.unknown) },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            modifier = Modifier
                .padding(MaterialTheme.padding.extraSmall),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = source,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.titleSmall,
            )
            val formattedLatestChapters = remember(latestChapter) {
                latestChapter?.let(::formatChapterNumber)
            }
            Text(
                text = stringResource(
                    MR.strings.migrationListScreen_latestChapterLabel,
                    formattedLatestChapters ?: stringResource(MR.strings.migrationListScreen_unknownLatestChapter),
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun MigrationListItemResult(
    modifier: Modifier,
    result: MigratingManga.SearchResult,
    onItemClick: (Manga) -> Unit,
) {
    Box(modifier.height(IntrinsicSize.Min)) {
        when (result) {
            MigratingManga.SearchResult.Searching -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 150.dp)
                        .fillMaxSize()
                        .aspectRatio(MangaCover.Book.ratio),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            MigratingManga.SearchResult.NotFound -> {
                Column(
                    Modifier
                        .widthIn(max = 150.dp)
                        .fillMaxSize()
                        .padding(4.dp),
                ) {
                    Image(
                        painter = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(MangaCover.Book.ratio)
                            .clip(MaterialTheme.shapes.extraSmall),
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = stringResource(MR.strings.migrationListScreen_noMatchFoundText),
                        modifier = Modifier.padding(MaterialTheme.padding.extraSmall),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            is MigratingManga.SearchResult.Success -> {
                MigrationListItem(
                    modifier = Modifier.fillMaxSize(),
                    manga = result.manga,
                    source = result.source,
                    chapterCount = result.chapterCount,
                    latestChapter = result.latestChapter,
                    onClick = { onItemClick(result.manga) },
                )
            }
        }
    }
}

@Composable
internal fun MigrationListItemAction(
    modifier: Modifier,
    result: MigratingManga.SearchResult,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val closeMenu = { menuExpanded = false }
    Box(modifier) {
        when (result) {
            MigratingManga.SearchResult.Searching -> {
                IconButton(onClick = onSkip) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                    )
                }
            }
            MigratingManga.SearchResult.NotFound, is MigratingManga.SearchResult.Success -> {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = closeMenu,
                    offset = DpOffset(8.dp, (-56).dp),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.migrationListScreen_searchManuallyActionLabel)) },
                        onClick = {
                            closeMenu()
                            onSearchManually()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.migrationListScreen_skipActionLabel)) },
                        onClick = {
                            closeMenu()
                            onSkip()
                        },
                    )
                    if (result is MigratingManga.SearchResult.Success) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.migrationListScreen_migrateNowActionLabel)) },
                            onClick = {
                                closeMenu()
                                onMigrate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.migrationListScreen_copyNowActionLabel)) },
                            onClick = {
                                closeMenu()
                                onCopy()
                            },
                        )
                    }
                }
            }
        }
    }
}
