package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.secondaryItemAlpha
import com.shinku.reader.presentation.theme.TachiyomiPreviewTheme

@Composable
internal fun InfoWidget(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PrefsHorizontalPadding,
                vertical = MaterialTheme.padding.medium,
            )
            .secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            InfoWidget(text = stringResource(MR.strings.download_ahead_info))
        }
    }
}
