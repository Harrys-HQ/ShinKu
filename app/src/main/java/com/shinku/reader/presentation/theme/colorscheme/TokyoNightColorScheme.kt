package com.shinku.reader.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Tokyo Night theme
 */
internal object TokyoNightColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF15161E),
        primaryContainer = Color(0xFF7AA2F7),
        onPrimaryContainer = Color(0xFF15161E),
        secondary = Color(0xFF7AA2F7),
        onSecondary = Color(0xFF15161E),
        secondaryContainer = Color(0xFF24283B),
        onSecondaryContainer = Color(0xFF7AA2F7),
        tertiary = Color(0xFFBB9AF7),
        onTertiary = Color(0xFF15161E),
        tertiaryContainer = Color(0xFF1F2335),
        onTertiaryContainer = Color(0xFFC0CAF5),
        error = Color(0xFFF7768E),
        onError = Color(0xFF15161E),
        errorContainer = Color(0xFFDB4B4B),
        onErrorContainer = Color(0xFFFFC0CB),
        background = Color(0xFF1A1B26),
        onBackground = Color(0xFFA9B1D6),
        surface = Color(0xFF1A1B26),
        onSurface = Color(0xFFA9B1D6),
        surfaceVariant = Color(0xFF24283B),
        onSurfaceVariant = Color(0xFFC0CAF5),
        outline = Color(0xFF7AA2F7),
        outlineVariant = Color(0xFF414868),
        scrim = Color(0xFF15161E),
        inverseSurface = Color(0xFFD5D6DB),
        inverseOnSurface = Color(0xFF24283B),
        inversePrimary = Color(0xFF34548A),
        surfaceDim = Color(0xFF1A1B26),
        surfaceBright = Color(0xFF24283B),
        surfaceContainerLowest = Color(0xFF1A1B26),
        surfaceContainerLow = Color(0xFF1F2335),
        surfaceContainer = Color(0xFF24283B),
        surfaceContainerHigh = Color(0xFF24283B),
        surfaceContainerHighest = Color(0xFF414868),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF34548A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF34548A),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF34548A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCFD5E5),
        onSecondaryContainer = Color(0xFF34548A),
        tertiary = Color(0xFF5A4A78),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE1E2E7),
        onTertiaryContainer = Color(0xFF343B58),
        error = Color(0xFF8C4351),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF962F39),
        onErrorContainer = Color(0xFFF7768E),
        background = Color(0xFFE1E2E7),
        onBackground = Color(0xFF343B58),
        surface = Color(0xFFE1E2E7),
        onSurface = Color(0xFF343B58),
        surfaceVariant = Color(0xFFCFD5E5),
        onSurfaceVariant = Color(0xFF343B58),
        outline = Color(0xFF34548A),
        outlineVariant = Color(0xFF9699A8),
        scrim = Color(0xFF0F0F14),
        inverseSurface = Color(0xFF1A1B26),
        inverseOnSurface = Color(0xFFA9B1D6),
        inversePrimary = Color(0xFF7AA2F7),
        surfaceDim = Color(0xFFE1E2E7),
        surfaceBright = Color(0xFFCFD5E5),
        surfaceContainerLowest = Color(0xFFE1E2E7),
        surfaceContainerLow = Color(0xFFE6E7ED),
        surfaceContainer = Color(0xFFCFD5E5),
        surfaceContainerHigh = Color(0xFFCFD5E5),
        surfaceContainerHighest = Color(0xFFB4B9C9),
    )
}
