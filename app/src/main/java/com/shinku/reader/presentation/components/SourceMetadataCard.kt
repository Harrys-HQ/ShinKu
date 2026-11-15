package com.shinku.reader.presentation.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun SourceMetadataCard(label: String, value: String, onCopy: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = SanctuaryShapes.deckCard, color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface, border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            Modifier.combinedClickable(onClick = {}, onLongClick = onCopy).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
