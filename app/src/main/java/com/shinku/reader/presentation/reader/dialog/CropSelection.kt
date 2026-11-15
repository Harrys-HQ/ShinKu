package com.shinku.reader.presentation.reader.dialog

import androidx.compose.ui.geometry.Rect

/** Keep a minimum-sized selection inside the snapshot, including drags into its letterbox. */
internal fun cropSelection(left: Float, top: Float, right: Float, bottom: Float, minWidth: Float = 0.02f, minHeight: Float = 0.02f): Rect {
    val x = left.coerceIn(0f, 1f - minWidth)
    val y = top.coerceIn(0f, 1f - minHeight)
    return Rect(x, y, right.coerceIn(x + minWidth, 1f), bottom.coerceIn(y + minHeight, 1f))
}
