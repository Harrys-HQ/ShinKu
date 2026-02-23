package eu.kanade.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme

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
