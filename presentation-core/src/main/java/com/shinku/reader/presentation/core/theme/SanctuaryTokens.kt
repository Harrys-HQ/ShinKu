package com.shinku.reader.presentation.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized "ShinKu Sanctuary" design tokens derived from the Library experience.
 */
object SanctuaryShapes {
    val hero = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(18.dp)
    val deckCard = RoundedCornerShape(16.dp)
    val coverLarge = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(10.dp)
    val tag = RoundedCornerShape(8.dp)
    val microIndicator = RoundedCornerShape(2.5.dp)
}

object SanctuaryElevation {
    val hero = 10.dp
    val card = 6.dp
    val compact = 4.dp
    val subtle = 2.dp
}

object SanctuaryBrushes {

    @Composable
    fun heroVerticalGradient(): Brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        ),
    )

    @Composable
    fun cardHorizontalGradient(): Brush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
            MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ),
    )

    @Composable
    fun compactHorizontalGradient(): Brush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        ),
    )

    @Composable
    fun subtleBorder(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
fun Modifier.sanctuaryShadow(
    elevation: Dp = SanctuaryElevation.card,
    shape: Shape = SanctuaryShapes.card,
    spotAlpha: Float = 0.30f,
    ambientAlpha: Float = 0.14f,
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = spotAlpha),
    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = ambientAlpha),
)
