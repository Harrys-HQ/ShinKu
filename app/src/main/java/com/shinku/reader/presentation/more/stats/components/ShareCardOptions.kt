package com.shinku.reader.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource

@Composable
internal fun ShareCardOptions(selected: Set<ShareStatistic>, onToggle: (ShareStatistic) -> Unit) {
    val labels = listOf(
        ShareStatistic.LEVEL to stringResource(MR.strings.journey_share_level),
        ShareStatistic.TIME to stringResource(MR.strings.journey_share_time),
        ShareStatistic.CHAPTERS to stringResource(MR.strings.journey_share_chapters),
        ShareStatistic.STREAK to stringResource(MR.strings.journey_share_streak),
        ShareStatistic.BADGES to stringResource(MR.strings.journey_share_badges),
        ShareStatistic.PATTERNS to stringResource(MR.strings.journey_share_patterns),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(MR.strings.journey_share_include), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(MR.strings.journey_share_scope), style = MaterialTheme.typography.bodySmall)
        labels.forEach { (statistic, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(statistic in selected, role = Role.Checkbox, onValueChange = { onToggle(statistic) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(checked = statistic in selected, onCheckedChange = null)
                Text(label, modifier = Modifier.weight(1f))
            }
        }
    }
}
