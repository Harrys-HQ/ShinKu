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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.ui.unit.sp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AdaptiveSheet
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    onSetAsCover: (useExtraPage: Boolean) -> Unit,
    onShare: (copy: Boolean, useExtraPage: Boolean) -> Unit,
    onSave: (useExtraPage: Boolean) -> Unit,
    onShareCombined: (copy: Boolean) -> Unit,
    onSaveCombined: () -> Unit,
    onFootnotes: () -> Unit,
    onTranslation: () -> Unit,
    hasExtraPage: Boolean,
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    var useExtraPage by remember { mutableStateOf(false) }
    var selectedSpreadTab by remember { mutableIntStateOf(0) } // 0: Page 1, 1: Page 2, 2: Spread

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = CircleShape,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Features Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "ShinKu Intelligence",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "AI Assist",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Feature Cards (Interactive Footnotes & Live Translation)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiToolRow(
                    title = stringResource(MR.strings.action_interactive_footnotes),
                    subtitle = "Explain cultural nuance, wordplay & character lore",
                    icon = Icons.Outlined.AutoAwesome,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onFootnotes()
                        onDismissRequest()
                    },
                )
                AiToolRow(
                    title = stringResource(MR.strings.action_live_translation),
                    subtitle = "Real-time manga bubble overlay & OCR",
                    icon = Icons.Outlined.Translate,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onTranslation()
                        onDismissRequest()
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // Page Actions Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Page Actions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (hasExtraPage) {
                    Text(
                        text = "Dual Page Spread",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (hasExtraPage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedSpreadTab == 0,
                        onClick = { selectedSpreadTab = 0 },
                        label = { Text("Page 1") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = selectedSpreadTab == 1,
                        onClick = { selectedSpreadTab = 1 },
                        label = { Text("Page 2") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = selectedSpreadTab == 2,
                        onClick = { selectedSpreadTab = 2 },
                        label = { Text("Combined") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedSpreadTab) {
                    0 -> {
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
                    1 -> {
                        StandardActionRow(
                            onSetCover = {
                                useExtraPage = true
                                showSetCoverDialog = true
                            },
                            onCopy = {
                                onShare(true, true)
                                onDismissRequest()
                            },
                            onShare = {
                                onShare(false, true)
                                onDismissRequest()
                            },
                            onSave = {
                                onSave(true)
                                onDismissRequest()
                            },
                        )
                    }
                    else -> {
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
                    }
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
        }
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                onSetAsCover(useExtraPage)
                showSetCoverDialog = false
                useExtraPage = false
            },
            onDismiss = {
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
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
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
