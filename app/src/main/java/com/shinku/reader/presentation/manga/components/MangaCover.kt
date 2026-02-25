package com.shinku.reader.presentation.manga.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import com.shinku.reader.presentation.util.rememberResourceBitmapPainter
import com.shinku.reader.R

enum class MangaCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f),
    ;

    @Composable
    operator fun invoke(
        data: Any?,
        modifier: Modifier = Modifier,
        contentDescription: String = "",
        shape: Shape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
    ) {
        var isLoading by remember { mutableStateOf(true) }
        Box(
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (isLoading) Modifier.shimmer() else Modifier
                )
        ) {
            AsyncImage(
                model = data,
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClick = onClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentScale = ContentScale.Crop,
                onSuccess = { isLoading = false },
                onLoading = { isLoading = true },
                onError = { isLoading = false }
            )
        }
    }
}

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim),
    )
    background(brush)
}

private val CoverPlaceholderColor = Color(0x1F888888)
