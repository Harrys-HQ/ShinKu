package com.shinku.reader.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun SearchFeedbackCard(message: String, title: String? = null, onShowAllSources: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(message, style = MaterialTheme.typography.bodyMedium)
            onShowAllSources?.let { action ->
                OutlinedButton(onClick = action, modifier = Modifier.heightIn(min = 48.dp), shape = SanctuaryShapes.medium) {
                    Text(stringResource(MR.strings.search_show_all_sources))
                }
            }
        }
    }
}
