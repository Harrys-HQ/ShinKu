package com.shinku.reader.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.overlineBadge
import com.shinku.reader.presentation.core.theme.overlineBadgeSmall
import com.shinku.reader.presentation.core.theme.sanctuaryShadow

/**
 * Signature Library Overline Badge with a primary square indicator dot and tracked uppercase text.
 */
@Composable
fun SanctuaryOverlineBadge(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 7.dp else 9.dp)
                .clip(SanctuaryShapes.microIndicator)
                .background(color),
        )
        Spacer(modifier = Modifier.width(if (compact) 5.dp else 7.dp))
        Text(
            text = text.uppercase(),
            style = if (compact) MaterialTheme.typography.overlineBadgeSmall else MaterialTheme.typography.overlineBadge,
            color = color,
        )
    }
}

/**
 * Signature Library section header row (e.g., "CONTINUE READING" ... "4 ONGOING").
 */
@Composable
fun SanctuarySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    trailingBadge: String? = null,
) {
    if (!overline.isNullOrBlank()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SanctuaryOverlineBadge(
                text = overline,
                compact = true,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.extraSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.overlineBadgeSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!trailingBadge.isNullOrBlank()) {
            Text(
                text = trailingBadge.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Reusable Sanctuary elevated gradient card container.
 */
@Composable
fun SanctuaryGradientCard(
    modifier: Modifier = Modifier,
    shape: Shape = SanctuaryShapes.card,
    elevation: Dp = SanctuaryElevation.card,
    brush: Brush = SanctuaryBrushes.cardHorizontalGradient(),
    innerPadding: Dp = MaterialTheme.padding.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .sanctuaryShadow(elevation = elevation, shape = shape),
        shape = shape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(innerPadding),
            content = content,
        )
    }
}

/**
 * Clean, continuous pill progress bar without Material 3 track gap or stop-indicator dot artifacts.
 */
@Composable
fun SanctuaryProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(SanctuaryShapes.pill)
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(SanctuaryShapes.pill)
                    .background(color),
            )
        }
    }
}
