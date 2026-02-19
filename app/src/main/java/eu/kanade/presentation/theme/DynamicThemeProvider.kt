package eu.kanade.presentation.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.dynamicColorScheme
import eu.kanade.domain.ui.UiPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun DynamicThemeProvider(
    cover: Drawable?,
    content: @Composable () -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val isAmoled = uiPreferences.themeDarkAmoled().get()
    val darkTheme = isSystemInDarkTheme()
    var seedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(cover) {
        if (cover is BitmapDrawable) {
            val bitmap = cover.bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                Palette.from(bitmap).generate().let { palette ->
                    val color = palette.getVibrantColor(
                        palette.getDominantColor(0)
                    )
                    if (color != 0) {
                        seedColor = Color(color)
                    }
                }
            }
        }
    }

    val overrideColorScheme = if (seedColor != null) {
        dynamicColorScheme(
            seedColor = seedColor!!,
            isDark = darkTheme,
            isAmoled = isAmoled,
        )
    } else {
        null
    }

    TachiyomiTheme(overrideColorScheme = overrideColorScheme) {
        content()
    }
}

/**
 * A simpler version that just extracts the color.
 */
@Composable
fun rememberCoverColor(cover: Drawable?): Color? {
    var color by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(cover) {
        if (cover is BitmapDrawable) {
            val bitmap = cover.bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                Palette.from(bitmap).generate().let { palette ->
                    val extracted = palette.getVibrantColor(
                        palette.getDominantColor(0)
                    )
                    if (extracted != 0) {
                        color = Color(extracted)
                    }
                }
            }
        }
    }
    return color
}
