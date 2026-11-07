package com.shinku.reader.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shinku.reader.core.common.preference.TriState
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun LibraryFilterItem(
    label: String,
    state: TriState,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Outlined.FilterAlt,
    onClick: (TriState) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        BoxWithConstraints(Modifier.background(SanctuaryBrushes.compactHorizontalGradient()).padding(12.dp)) {
            val heading: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
            // Reflow at large font sizes without shrinking the three touch targets.
            if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    heading(Modifier.fillMaxWidth())
                    FilterChoices(state, enabled, onClick, Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    heading(Modifier.weight(1f))
                    FilterChoices(state, enabled, onClick, Modifier.weight(1.6f))
                }
            }
        }
    }
}

@Composable
private fun FilterChoices(state: TriState, enabled: Boolean, onClick: (TriState) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = SanctuaryShapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(Modifier.selectableGroup().padding(2.dp)) {
            listOf(
                TriState.DISABLED to MR.strings.filter_any,
                TriState.ENABLED_IS to MR.strings.filter_include,
                TriState.ENABLED_NOT to MR.strings.filter_exclude,
            ).forEach { (value, title) ->
                val selected = state == value
                Surface(
                    modifier = Modifier.weight(1f).clip(SanctuaryShapes.pill).selectable(
                        selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onClick(value) },
                    ),
                    shape = SanctuaryShapes.pill,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        .copy(alpha = if (enabled) 1f else 0.38f),
                ) {
                    Box(
                        Modifier.heightIn(min = 48.dp).padding(horizontal = 3.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(title), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
