package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

/** Full-width rows keep long durations and localized numbers visible at large text sizes. */
@Composable
internal fun JourneyOverviewMetrics(libraryCount: String, readDuration: String, completedCount: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JourneyMetric(libraryCount, stringResource(MR.strings.journey_in_library), Icons.Outlined.CollectionsBookmark)
        JourneyMetric(readDuration, stringResource(MR.strings.journey_time_read), Icons.Outlined.Schedule)
        JourneyMetric(completedCount, stringResource(MR.strings.journey_completed), Icons.Outlined.LocalLibrary)
    }
}

@Composable
private fun JourneyMetric(value: String, label: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = SanctuaryShapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
