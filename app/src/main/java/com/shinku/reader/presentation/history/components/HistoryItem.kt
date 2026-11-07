package com.shinku.reader.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.DropdownMenu
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.manga.components.MangaCover
import com.shinku.reader.presentation.theme.TachiyomiPreviewTheme
import com.shinku.reader.presentation.util.formatChapterNumber
import com.shinku.reader.util.lang.toTimestampString

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = SanctuaryShapes.deckCard
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium, vertical = 4.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
        shadowElevation = SanctuaryElevation.subtle,
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .clickable(onClick = onClickResume)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Book(
                modifier = Modifier
                    .height(104.dp)
                    .clip(SanctuaryShapes.medium),
                data = history.coverData,
                contentDescription = history.title,
                onClick = onClickCover,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
            ) {
                Text(
                    text = history.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                val readAt = history.readAt?.toTimestampString().orEmpty()
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (history.chapterNumber > -1) {
                        Surface(
                            shape = SanctuaryShapes.tag,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = "Ch. ${formatChapterNumber(history.chapterNumber)}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                    if (readAt.isNotEmpty()) {
                        Text(
                            text = if (history.chapterNumber > -1) " · $readAt" else readAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onClickResume,
                    modifier = Modifier.padding(top = 6.dp).heightIn(min = 48.dp),
                    shape = SanctuaryShapes.medium,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(MR.strings.action_resume), modifier = Modifier.padding(start = 6.dp))
                }
            }

            Box(modifier = Modifier.align(Alignment.Top)) {
                var expanded by remember(history.id) { mutableStateOf(false) }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(MR.strings.action_menu_overflow_description),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (!history.coverData.isMangaFavorite) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.add_to_library)) },
                            onClick = {
                                expanded = false
                                onClickFavorite()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(MR.strings.action_delete)) },
                        onClick = {
                            expanded = false
                            onClickDelete()
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(HistoryWithRelationsProvider::class)
    historyWithRelations: HistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            HistoryItem(
                history = historyWithRelations,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
