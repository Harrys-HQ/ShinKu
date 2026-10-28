package com.shinku.reader.presentation.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorScheme.active: Color
    @Composable
    get() {
        return if (isSystemInDarkTheme()) Color(255, 235, 59) else Color(255, 193, 7)
    }

val ColorScheme.statusExcellent: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color(0xFF2E7D32)

val ColorScheme.statusGood: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8BC34A) else Color(0xFF558B2F)

val ColorScheme.statusWarning: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFFB300) else Color(0xFFEF6C00)

val ColorScheme.statusDegraded: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFF7043) else Color(0xFFD84315)
