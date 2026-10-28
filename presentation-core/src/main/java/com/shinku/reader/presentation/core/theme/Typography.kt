package com.shinku.reader.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )

val Typography.overlineBadge: TextStyle
    @Composable
    get() = labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    )

val Typography.overlineBadgeSmall: TextStyle
    @Composable
    get() = labelSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp,
    )
