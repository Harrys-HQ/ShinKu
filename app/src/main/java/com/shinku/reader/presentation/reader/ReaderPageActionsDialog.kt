package com.shinku.reader.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AdaptiveSheet
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import androidx.compose.material3.surfaceColorAtElevation

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    onSetAsCover: (useExtraPage: Boolean) -> Unit,
    onShare: (copy: Boolean, useExtraPage: Boolean) -> Unit,
    onSave: (useExtraPage: Boolean) -> Unit,
    onShareCombined: (copy: Boolean) -> Unit,
    onSaveCombined: () -> Unit,
    onFootnotes: () -> Unit,
    onCharacterCodex: () -> Unit = {},
    onChapterRecap: () -> Unit = {},
    onAreaOcr: () -> Unit = {},
    onTranslation: () -> Unit,
    onBatchTranslation: () -> Unit = {},
    hasExtraPage: Boolean,
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    var useExtraPage by remember { mutableStateOf(false) }
    var selectedSpreadTab by remember { mutableIntStateOf(0) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Page Actions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (hasExtraPage) {
                    SanctuaryOverlineBadge(
                        text = "DUAL SPREAD",
                        compact = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasExtraPage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Page 1", "Page 2", "Combined").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedSpreadTab == idx,
                            onClick = { selectedSpreadTab = idx },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            shape = SanctuaryShapes.pill,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedSpreadTab == 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PageActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Copy",
                            icon = Icons.Outlined.ContentCopy,
                            onClick = {
                                onShareCombined(true)
                                onDismissRequest()
                            },
                        )
                        PageActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Share",
                            icon = Icons.Outlined.Share,
                            onClick = {
                                onShareCombined(false)
                                onDismissRequest()
                            },
                        )
                        PageActionButton(
                            modifier = Modifier.weight(1f),
                            title = "Save",
                            icon = Icons.Outlined.Save,
                            onClick = {
                                onSaveCombined()
                                onDismissRequest()
                            },
                        )
                    }
                } else {
                    val isSecondPage = selectedSpreadTab == 1
                    StandardActionRow(
                        onSetCover = {
                            useExtraPage = isSecondPage
                            showSetCoverDialog = true
                        },
                        onCopy = {
                            onShare(true, isSecondPage)
                            onDismissRequest()
                        },
                        onShare = {
                            onShare(false, isSecondPage)
                            onDismissRequest()
                        },
                        onSave = {
                            onSave(isSecondPage)
                            onDismissRequest()
                        },
                    )
                }
            } else {
                StandardActionRow(
                    onSetCover = {
                        useExtraPage = false
                        showSetCoverDialog = true
                    },
                    onCopy = {
                        onShare(true, false)
                        onDismissRequest()
                    },
                    onShare = {
                        onShare(false, false)
                        onDismissRequest()
                    },
                    onSave = {
                        onSave(false)
                        onDismissRequest()
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "ShinKu Intelligence",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                SanctuaryOverlineBadge(
                    text = "AI COMPANION",
                    compact = true,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiToolRow(
                    title = "Who's Here? (Spoiler-Safe Codex)",
                    subtitle = "Characters, factions & lore up to this chapter",
                    icon = Icons.Outlined.MenuBook,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onCharacterCodex()
                        onDismissRequest()
                    },
                )
                AiToolRow(
                    title = "Story So Far (Chapter Recap)",
                    subtitle = "Spoiler-safe plot summary up to this chapter",
                    icon = Icons.Outlined.HistoryEdu,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        onChapterRecap()
                        onDismissRequest()
                    },
                )
                AiToolRow(
                    title = stringResource(MR.strings.action_interactive_footnotes),
                    subtitle = "Explain cultural nuance, wordplay & idioms",
                    icon = Icons.Outlined.AutoAwesome,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onFootnotes()
                        onDismissRequest()
                    },
                )
                AiToolRow(
                    title = "Area OCR & Translate",
                    subtitle = "Select a speech bubble or panel to translate",
                    icon = Icons.Outlined.CropFree,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onAreaOcr()
                    },
                )
                AiToolRow(
                    title = stringResource(MR.strings.action_live_translation),
                    subtitle = "Real-time manga bubble overlay & OCR",
                    icon = Icons.Outlined.Translate,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = {
                        onTranslation()
                        onDismissRequest()
                    },
                )
                AiToolRow(
                    title = "Pre-Translate Chapter",
                    subtitle = "Batch OCR & translate all pages in background",
                    icon = Icons.Outlined.AutoAwesomeMotion,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onBatchTranslation()
                        onDismissRequest()
                    },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(MR.strings.action_close))
            }
        }
    }

    if (showSetCoverDialog) {
        AlertDialog(
            shape = SanctuaryShapes.hero,
            text = { Text(stringResource(MR.strings.confirm_set_image_as_cover)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetAsCover(useExtraPage)
                        showSetCoverDialog = false
                        useExtraPage = false
                    },
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSetCoverDialog = false
                        useExtraPage = false
                    },
                ) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
            onDismissRequest = {
                showSetCoverDialog = false
                useExtraPage = false
            },
        )
    }
}

@Composable
private fun StandardActionRow(
    onSetCover: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PageActionButton(
            modifier = Modifier.weight(1f),
            title = "Set Cover",
            icon = Icons.Outlined.Photo,
            onClick = onSetCover,
        )
        PageActionButton(
            modifier = Modifier.weight(1f),
            title = "Copy",
            icon = Icons.Outlined.ContentCopy,
            onClick = onCopy,
        )
        PageActionButton(
            modifier = Modifier.weight(1f),
            title = "Share",
            icon = Icons.Outlined.Share,
            onClick = onShare,
        )
        PageActionButton(
            modifier = Modifier.weight(1f),
            title = "Save",
            icon = Icons.Outlined.Save,
            onClick = onSave,
        )
    }
}

@Composable
private fun PageActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(SanctuaryShapes.deckCard)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = SanctuaryShapes.medium,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
            border = SanctuaryBrushes.subtleBorder(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AiToolRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SanctuaryShapes.coverLarge)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.25f),
                shape = SanctuaryShapes.coverLarge,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = SanctuaryShapes.pill,
                color = contentColor.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
