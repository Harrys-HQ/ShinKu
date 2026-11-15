package com.shinku.reader.presentation.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
internal fun ReaderSettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
