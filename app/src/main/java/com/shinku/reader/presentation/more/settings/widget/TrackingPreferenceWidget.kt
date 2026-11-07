package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.data.track.Tracker
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.more.settings.LocalPreferenceHighlighted
import com.shinku.reader.presentation.track.components.TrackLogoIcon

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    tracker: Tracker,
    checked: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    Box(modifier = Modifier.highlightBackground(highlighted)) {
        Row(
            modifier = modifier
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .fillMaxWidth()
                .padding(horizontal = PrefsHorizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackLogoIcon(tracker)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(tracker.name, maxLines = 2, style = MaterialTheme.typography.titleLarge, fontSize = TitleFontSize)
                Text(
                    stringResource(if (checked) MR.strings.tracker_connected else MR.strings.tracker_not_connected),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (checked) {
                OutlinedButton(
                    onClick = { onClick?.invoke() },
                    enabled = onClick != null,
                    shape = SanctuaryShapes.tag,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.tracker_disconnect),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Button(
                    onClick = { onClick?.invoke() },
                    enabled = onClick != null,
                    shape = SanctuaryShapes.tag,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.tracker_connect),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
