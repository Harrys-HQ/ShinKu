package com.shinku.reader.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.theme.TachiyomiPreviewTheme

@Composable
fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    val text = "$currentPage / $totalPages"

    Surface(
        shape = SanctuaryShapes.pill,
        color = MaterialTheme.colorScheme
            .surfaceColorAtElevation(SanctuaryElevation.subtle)
            .copy(alpha = 0.85f),
        border = SanctuaryBrushes.subtleBorder(),
        shadowElevation = SanctuaryElevation.subtle,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReaderPageIndicatorPreview() {
    TachiyomiPreviewTheme {
        Surface {
            ReaderPageIndicator(currentPage = 10, totalPages = 69)
        }
    }
}
