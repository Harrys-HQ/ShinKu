package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun AppearanceReadingPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(MR.strings.appearance_preview), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = SanctuaryShapes.tag,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(width = 44.dp, height = 62.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(MR.strings.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(MR.strings.appearance_preview_sample), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                stringResource(MR.strings.appearance_preview_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
