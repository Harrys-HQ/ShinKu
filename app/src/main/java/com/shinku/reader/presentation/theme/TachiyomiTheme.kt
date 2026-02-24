package com.shinku.reader.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.AppTheme
import com.shinku.reader.presentation.theme.colorscheme.BaseColorScheme
import com.shinku.reader.presentation.theme.colorscheme.CatppuccinColorScheme
import com.shinku.reader.presentation.theme.colorscheme.GreenAppleColorScheme
import com.shinku.reader.presentation.theme.colorscheme.LavenderColorScheme
import com.shinku.reader.presentation.theme.colorscheme.MidnightDuskColorScheme
import com.shinku.reader.presentation.theme.colorscheme.MonetColorScheme
import com.shinku.reader.presentation.theme.colorscheme.MonochromeColorScheme
import com.shinku.reader.presentation.theme.colorscheme.NordColorScheme
import com.shinku.reader.presentation.theme.colorscheme.StrawberryColorScheme
import com.shinku.reader.presentation.theme.colorscheme.TachiyomiColorScheme
import com.shinku.reader.presentation.theme.colorscheme.TakoColorScheme
import com.shinku.reader.presentation.theme.colorscheme.TealTurqoiseColorScheme
import com.shinku.reader.presentation.theme.colorscheme.TidalWaveColorScheme
import com.shinku.reader.presentation.theme.colorscheme.YinYangColorScheme
import com.shinku.reader.presentation.theme.colorscheme.YotsubaColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    overrideColorScheme: ColorScheme? = null,
    genres: List<String>? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    var colorScheme = overrideColorScheme ?: getThemeColorScheme(
        appTheme = appTheme ?: uiPreferences.appTheme().get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled().get(),
    )

    if (genres != null && (appTheme ?: uiPreferences.appTheme().get()) == AppTheme.STRAWBERRY_DAIQUIRI) {
        colorScheme = applyGenreTone(colorScheme, genres)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private fun applyGenreTone(scheme: ColorScheme, genres: List<String>): ColorScheme {
    val primaryHue = when {
        genres.any { it.contains("Horror", true) || it.contains("Psychological", true) } -> 0f // Deep Blood Red
        genres.any { it.contains("Sci-Fi", true) || it.contains("Cyberpunk", true) } -> 300f // Neon Crimson/Magenta
        genres.any { it.contains("Slice of Life", true) || it.contains("Romance", true) } -> 340f // Soft Pinkish Crimson
        genres.any { it.contains("Action", true) || it.contains("Adventure", true) } -> 15f // Vibrant Orange-Red
        else -> return scheme
    }

    fun adjustColor(color: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[0] = primaryHue
        return Color(ColorUtils.HSLToColor(hsl))
    }

    return scheme.copy(
        primary = adjustColor(scheme.primary),
        primaryContainer = adjustColor(scheme.primaryContainer),
        secondary = adjustColor(scheme.secondary),
        secondaryContainer = adjustColor(scheme.secondaryContainer),
        tertiary = adjustColor(scheme.tertiary),
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, null, content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    overrideColorScheme: ColorScheme?,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = overrideColorScheme ?: getThemeColorScheme(appTheme, isAmoled),
        content = content,
    )
}

@Composable
@ReadOnlyComposable
private fun getThemeColorScheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(LocalContext.current)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isSystemInDarkTheme(),
        isAmoled,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
