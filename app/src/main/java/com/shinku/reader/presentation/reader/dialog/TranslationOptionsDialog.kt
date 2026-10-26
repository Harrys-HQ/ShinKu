package com.shinku.reader.presentation.reader.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.R

@Composable
fun TranslationOptionsDialog(
    totalPages: Int,
    targetLanguage: String,
    hasCachedTranslations: Boolean,
    isOverlayVisible: Boolean = false,
    onDismissRequest: () -> Unit,
    onToggleOverlay: () -> Unit = {},
    onTranslateCurrentPage: () -> Unit,
    onTranslateEntireChapter: () -> Unit,
    onChangeLanguages: () -> Unit,
    onClearTranslations: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = "Translation Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasCachedTranslations || isOverlayVisible) {
                    OptionRow(
                        icon = Icons.Outlined.Translate,
                        title = if (isOverlayVisible) "Hide Translation Overlays" else "Show Translation Overlays",
                        subtitle = if (isOverlayVisible) "Hide all speech bubble overlays in this chapter" else "Display speech bubble overlays across all translated pages",
                        iconTint = if (isOverlayVisible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismissRequest()
                            onToggleOverlay()
                        },
                    )
                }

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
                        title = "Clear Chapter Translations",
                        subtitle = "Purge cached overlay data for this chapter",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDismissRequest()
                            onClearTranslations()
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        },
    )
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
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
