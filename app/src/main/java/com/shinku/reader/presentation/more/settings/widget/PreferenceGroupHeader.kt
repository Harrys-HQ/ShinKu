package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge

@Composable
fun PreferenceGroupHeader(title: String) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp, top = 16.dp),
    ) {
        SanctuaryOverlineBadge(
            text = title,
            compact = true,
            modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
        )
    }
}
