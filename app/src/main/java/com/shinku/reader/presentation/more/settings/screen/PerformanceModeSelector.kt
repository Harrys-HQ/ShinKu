package com.shinku.reader.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.util.collectAsState

@Composable
internal fun PerformanceModeSelector(preference: Preference<Int>) {
    val selected by preference.collectAsState()
    val modes = listOf(
        BasePreferences.PerformanceMode.STANDARD.value to stringResource(MR.strings.pref_performance_mode_standard),
        BasePreferences.PerformanceMode.LOW_POWER.value to stringResource(MR.strings.pref_performance_mode_low_power),
        BasePreferences.PerformanceMode.E_INK.value to stringResource(MR.strings.pref_performance_mode_eink),
    )
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(MR.strings.pref_performance_mode), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { preference.set(value) },
                    label = { Text(label) },
                    leadingIcon = if (selected == value) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    shape = SanctuaryShapes.medium,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(
            text = stringResource(
                when (selected) {
                    BasePreferences.PerformanceMode.LOW_POWER.value -> MR.strings.pref_performance_mode_low_power_summary
                    BasePreferences.PerformanceMode.E_INK.value -> MR.strings.pref_performance_mode_eink_summary
                    else -> MR.strings.performance_standard_description
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
