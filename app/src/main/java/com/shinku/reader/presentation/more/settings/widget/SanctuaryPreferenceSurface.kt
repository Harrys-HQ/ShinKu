package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

/** Separate lazy rows keep large settings pages cheap while sharing the same card edge. */
@Composable
internal fun SanctuaryPreferenceSurface(
    first: Boolean = true,
    last: Boolean = true,
    standalone: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (standalone) 4.dp else 0.dp),
        shape = RoundedCornerShape(
            topStart = if (first) SanctuaryShapes.deckCard.topStart else CornerSize(0.dp),
            topEnd = if (first) SanctuaryShapes.deckCard.topEnd else CornerSize(0.dp),
            bottomStart = if (last) SanctuaryShapes.deckCard.bottomStart else CornerSize(0.dp),
            bottomEnd = if (last) SanctuaryShapes.deckCard.bottomEnd else CornerSize(0.dp),
        ),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = SanctuaryBrushes.subtleBorder(),
        content = content,
    )
}
