package com.shinku.reader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shinku.reader.domain.manga.model.Manga

@Composable
fun DynamicBackdrop(
    manga: Manga?,
    modifier: Modifier = Modifier,
    blurRadius: Int = 24,
    overlayColor: Color = Color.Black.copy(alpha = 0.5f),
) {
    if (manga == null) return

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = manga,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlay for better readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            overlayColor,
                            overlayColor.copy(alpha = 0.7f),
                            overlayColor,
                        ),
                    ),
                ),
        )
    }
}
