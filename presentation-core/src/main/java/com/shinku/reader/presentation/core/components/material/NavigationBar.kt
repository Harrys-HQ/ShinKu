package com.shinku.reader.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floating Dynamic Island Manga Dock
 */
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) {
    val islandShape = RoundedCornerShape(22.dp)
    val borderGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = islandShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = contentColor,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.2.dp, borderGradient),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = islandShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(islandShape)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
