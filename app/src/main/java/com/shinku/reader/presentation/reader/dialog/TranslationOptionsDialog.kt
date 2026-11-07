package com.shinku.reader.presentation.reader.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.R
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun TranslationOptionsDialog(
    totalPages: Int,
    targetLanguage: String,
    hasCachedTranslations: Boolean,
    isOverlayVisible: Boolean = false,
    currentPageBlockCount: Int = 0,
    onDismissRequest: () -> Unit,
    onToggleOverlay: () -> Unit = {},
    onTranslateCurrentPage: () -> Unit,
    onRetranslateCurrentPage: () -> Unit = {},
    onInspectBubbles: () -> Unit = {},
    onTranslateEntireChapter: () -> Unit,
    onChangeLanguages: () -> Unit,
    onClearTranslations: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = SanctuaryShapes.hero,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.card),
        tonalElevation = SanctuaryElevation.card,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_shinku_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(SanctuaryShapes.tag),
                )
                Column {
                    SanctuaryOverlineBadge(
                        text = "SHINKU AI",
                        compact = true,
                    )
                    Text(
                        text = "Translation Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        text = {
            TranslationOptionsContent(
                totalPages = totalPages,
                targetLanguage = targetLanguage,
                hasCachedTranslations = hasCachedTranslations,
                isOverlayVisible = isOverlayVisible,
                currentPageBlockCount = currentPageBlockCount,
                onDismissRequest = onDismissRequest,
                onToggleOverlay = onToggleOverlay,
                onTranslateCurrentPage = onTranslateCurrentPage,
                onRetranslateCurrentPage = onRetranslateCurrentPage,
                onInspectBubbles = onInspectBubbles,
                onTranslateEntireChapter = onTranslateEntireChapter,
                onChangeLanguages = onChangeLanguages,
                onClearTranslations = onClearTranslations,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
            }
        },
    )
}

@Composable
fun TranslationOptionsContent(
    totalPages: Int,
    targetLanguage: String,
    hasCachedTranslations: Boolean,
    isOverlayVisible: Boolean = false,
    currentPageBlockCount: Int = 0,
    onDismissRequest: () -> Unit,
    onToggleOverlay: () -> Unit = {},
    onTranslateCurrentPage: () -> Unit,
    onRetranslateCurrentPage: () -> Unit = {},
    onInspectBubbles: () -> Unit = {},
    onTranslateEntireChapter: () -> Unit,
    onChangeLanguages: () -> Unit,
    onClearTranslations: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasCachedTranslations || isOverlayVisible) {
            Surface(
                shape = SanctuaryShapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = SanctuaryBrushes.subtleBorder(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .toggleable(value = isOverlayVisible, role = Role.Switch, onValueChange = { onToggleOverlay() })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(MR.strings.reader_show_translation_overlays),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = isOverlayVisible, onCheckedChange = null)
                }
            }
        }

        if (currentPageBlockCount > 0) {
            OptionRow(
                icon = Icons.Outlined.FindInPage,
                title = "Inspect Page Bubbles ($currentPageBlockCount)",
                subtitle = "Compare original OCR & translated text, or copy dialogue",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    onDismissRequest()
                    onInspectBubbles()
                },
            )

            OptionRow(
                icon = Icons.Outlined.Refresh,
                title = "Re-Translate Current Page",
                subtitle = "Bypass cache and run fresh OCR & translation",
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    onDismissRequest()
                    onRetranslateCurrentPage()
                },
            )
        } else {
            OptionRow(
                icon = Icons.Outlined.Translate,
                title = "Translate Current Page",
                subtitle = "Live OCR & speech bubble overlay",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    onDismissRequest()
                    onTranslateCurrentPage()
                },
            )
        }

        OptionRow(
            icon = Icons.Outlined.AutoAwesomeMotion,
            title = "Pre-Translate Entire Chapter",
            subtitle = "Batch OCR all $totalPages pages into $targetLanguage",
            iconTint = MaterialTheme.colorScheme.tertiary,
            onClick = {
                onDismissRequest()
                onTranslateEntireChapter()
            },
        )

        OptionRow(
            icon = Icons.Outlined.Language,
            title = "Translation Languages",
            subtitle = "Target: $targetLanguage",
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = {
                onDismissRequest()
                onChangeLanguages()
            },
        )

        if (hasCachedTranslations) {
            OptionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = stringResource(MR.strings.clear_chapter_translations),
                subtitle = stringResource(MR.strings.clear_chapter_translations_summary),
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    confirmClear = true
                },
            )
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(MR.strings.clear_chapter_translations_question)) },
            text = { Text(stringResource(MR.strings.clear_chapter_translations_summary)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onDismissRequest()
                    onClearTranslations()
                }) { Text(stringResource(MR.strings.action_clear_translation_cache)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SanctuaryShapes.deckCard)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        shape = SanctuaryShapes.deckCard,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = SanctuaryShapes.medium,
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
