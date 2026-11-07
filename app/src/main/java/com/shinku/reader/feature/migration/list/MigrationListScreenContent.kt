package com.shinku.reader.feature.migration.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.shinku.reader.R
import com.shinku.reader.domain.manga.model.Manga
import com.shinku.reader.feature.migration.list.models.MigratingManga
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.Badge
import com.shinku.reader.presentation.core.components.BadgeGroup
import com.shinku.reader.presentation.core.components.FastScrollLazyColumn
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.components.material.topSmallPaddingValues
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.plus
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.util.animateItemFastScroll
import com.shinku.reader.presentation.util.formatChapterNumber
import com.shinku.reader.presentation.util.rememberResourceBitmapPainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun MigrationListScreenContent(
    items: ImmutableList<MigratingManga>,
    migrationComplete: Boolean,
    finishedCount: Int,
    onItemClick: (Manga) -> Unit,
    onSearchManually: (MigratingManga) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
    openMigrationDialog: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (items.isNotEmpty()) {
                    stringResource(MR.strings.migrationListScreenTitleWithProgress, finishedCount, items.size)
                } else {
                    stringResource(MR.strings.migrationListScreenTitle)
                },
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_copyActionLabel),
                                icon = if (items.size == 1) Icons.Outlined.ContentCopy else Icons.Outlined.CopyAll,
                                onClick = { openMigrationDialog(true) },
                                enabled = migrationComplete,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.migrationListScreen_migrateActionLabel),
                                icon = if (items.size == 1) Icons.Outlined.Done else Icons.Outlined.DoneAll,
                                onClick = { openMigrationDialog(false) },
                                enabled = migrationComplete,
                            ),
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        FastScrollLazyColumn(
            contentPadding = contentPadding + topSmallPaddingValues,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (items.isNotEmpty()) {
                item(key = "migration-progress-card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaterialTheme.padding.medium),
                        shape = SanctuaryShapes.deckCard,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                        border = SanctuaryBrushes.subtleBorder(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$finishedCount of ${items.size} reviewed",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                )
                                Text(
                                    text = if (migrationComplete) "Ready to migrate" else "Searching matches…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (finishedCount.toFloat() / items.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(SanctuaryShapes.pill),
                            )
                            Text(
                                text = "Compare the current library entry on the left with the proposed match on the right.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            items(items, key = { it.manga.id }) { item ->
                val result by item.searchResult.collectAsState()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemFastScroll()
                        .padding(horizontal = MaterialTheme.padding.medium),
                    shape = SanctuaryShapes.deckCard,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "New source (proposed)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            MigrationListItemAction(
                                modifier = Modifier,
                                result = result,
                                onSearchManually = { onSearchManually(item) },
                                onSkip = { onSkip(item.manga.id) },
                                onMigrate = { onMigrate(item.manga.id) },
                                onCopy = { onCopy(item.manga.id) },
                            )
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MigrationListItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top)
                                    .fillMaxHeight(),
                                manga = item.manga,
                                source = item.source,
                                chapterCount = item.chapterCount,
                                latestChapter = item.latestChapter,
                                onClick = { onItemClick(item.manga) },
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.18f),
                            )

                            MigrationListItemResult(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top)
                                    .fillMaxHeight(),
                                result = result,
                                onItemClick = onItemClick,
                            )
                        }

                        if (result !is MigratingManga.SearchResult.Searching) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (result is MigratingManga.SearchResult.Success) {
                                    FilledTonalButton(
                                        onClick = { onMigrate(item.manga.id) },
                                        shape = SanctuaryShapes.medium,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(MR.strings.migrationListScreen_migrateNowActionLabel))
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = { onSearchManually(item) },
                                        shape = SanctuaryShapes.medium,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(MR.strings.migrationListScreen_searchManuallyActionLabel))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onSkip(item.manga.id) },
                                    shape = SanctuaryShapes.medium,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(MR.strings.migrationListScreen_skipActionLabel))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
