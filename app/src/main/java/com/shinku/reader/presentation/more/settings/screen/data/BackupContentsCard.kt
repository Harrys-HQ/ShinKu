package com.shinku.reader.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.data.backup.BackupContentsSummary
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun BackupContentsCard(contents: BackupContentsSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(MR.strings.backup_contents_preview), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(MR.strings.backup_contents_titles, contents.titles))
            Text(stringResource(MR.strings.backup_contents_chapters, contents.chapters))
            Text(stringResource(MR.strings.backup_contents_categories, contents.categories))
            Text(stringResource(MR.strings.backup_contents_history, contents.historyEntries))
            Text(stringResource(MR.strings.backup_contents_settings, contents.settings))
            Text(
                stringResource(MR.strings.backup_contents_scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
