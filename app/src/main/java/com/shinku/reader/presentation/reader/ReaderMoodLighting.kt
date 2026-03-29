package com.shinku.reader.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.shinku.reader.domain.manga.model.Manga

@Composable
fun ReaderMoodLighting(
    manga: Manga?,
    enabled: Boolean,
) {
    if (!enabled || manga == null) return

    val genres = manga.genre ?: emptyList()

    val mood = remember(genres) {
        getMoodForGenres(genres)
    }

    ReaderContentOverlay(
        brightness = mood.brightness,
        color = mood.color?.toArgb(),
        colorBlendMode = mood.blendMode,
    )
}

private data class Mood(
    val brightness: Int = 0,
    val color: Color? = null,
    val blendMode: BlendMode? = null,
)

private fun getMoodForGenres(genres: List<String>): Mood {
    return when {
        genres.any { it.contains("Horror", true) || it.contains("Thriller", true) } -> {
            // Horror: Dimmer and cooler (blueish)
            Mood(
                brightness = -20,
                color = Color(0xFF001533).copy(alpha = 0.15f),
                blendMode = BlendMode.SrcOver,
            )
        }
        genres.any { it.contains("Romance", true) || it.contains("Slice of Life", true) } -> {
            // Romance/Slice of Life: Warmer (sepia-ish)
            Mood(
                brightness = 0,
                color = Color(0xFF704214).copy(alpha = 0.1f),
                blendMode = BlendMode.SrcOver,
            )
        }
        genres.any { it.contains("Action", true) || it.contains("Adventure", true) || it.contains("Fantasy", true) } -> {
            // Action/Fantasy: Slightly more vibrant/dynamic (subtle green tint)
            Mood(
                brightness = -5,
                color = Color(0xFF1B4D3E).copy(alpha = 0.08f),
                blendMode = BlendMode.SrcOver,
            )
        }
        else -> {
            // Default: Subtle dim for eye comfort
            Mood(brightness = -10)
        }
    }
}
