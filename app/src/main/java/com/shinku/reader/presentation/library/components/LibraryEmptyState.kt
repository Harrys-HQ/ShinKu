package com.shinku.reader.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun LibraryEmptyState(
    onBrowseSources: () -> Unit,
    onRestoreBackup: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = SanctuaryShapes.hero,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = SanctuaryBrushes.subtleBorder(),
        ) {
            Column(
                Modifier.background(SanctuaryBrushes.heroVerticalGradient()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), SanctuaryShapes.card),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
                Text(
                    stringResource(MR.strings.library_empty_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(stringResource(MR.strings.library_empty_guidance), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = onBrowseSources, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                    Text(stringResource(MR.strings.library_empty_browse_sources), textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                    Text(stringResource(MR.strings.library_empty_restore_backup), textAlign = TextAlign.Center)
                }
                TextButton(onClick = onOpenGuide, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(MR.strings.getting_started_guide), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
