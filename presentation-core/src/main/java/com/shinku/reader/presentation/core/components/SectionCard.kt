package com.shinku.reader.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.sanctuaryShadow
import dev.icerock.moko.resources.StringResource

@Composable
fun LazyItemScope.SectionCard(
    titleRes: StringResource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = 6.dp,
            ),
    ) {
        if (titleRes != null) {
            SanctuaryOverlineBadge(
                text = stringResource(titleRes),
                compact = true,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .sanctuaryShadow(
                    elevation = SanctuaryElevation.compact,
                    shape = SanctuaryShapes.card,
                    spotAlpha = 0.20f,
                    ambientAlpha = 0.08f,
                ),
            shape = SanctuaryShapes.card,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
            border = SanctuaryBrushes.subtleBorder(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SanctuaryBrushes.compactHorizontalGradient())
                    .padding(MaterialTheme.padding.medium),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content,
                )
            }
        }
    }
}
