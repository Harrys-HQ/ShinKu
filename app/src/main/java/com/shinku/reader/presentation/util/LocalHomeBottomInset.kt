package com.shinku.reader.presentation.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/** Scrollable home-tab content reserves this space at its end, behind the floating dock. */
val LocalHomeBottomInset = compositionLocalOf { 0.dp }
