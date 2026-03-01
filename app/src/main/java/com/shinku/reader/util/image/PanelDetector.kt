package com.shinku.reader.util.image

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.abs

class PanelDetector {

    /**
     * Detects manga panels in the given bitmap.
     * Returns a list of RectF representing panels in reading order.
     */
    fun detect(bitmap: Bitmap): List<RectF> {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. Find the background color (usually white or black)
        val backgroundColor = findBackgroundColor(bitmap)
        
        // 2. Recursively split the image based on gutters
        val panels = mutableListOf<RectF>()
        splitRecursive(bitmap, 0f, 0f, width.toFloat(), height.toFloat(), backgroundColor, panels)
        
        // 3. Filter and sort panels
        return refinePanels(panels, width.toFloat(), height.toFloat())
    }

    private fun findBackgroundColor(bitmap: Bitmap): Int {
        // Sample corners
        val corners = listOf(
            bitmap.getPixel(0, 0),
            bitmap.getPixel(bitmap.width - 1, 0),
            bitmap.getPixel(0, bitmap.height - 1),
            bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        )
        // Return most common or just white if unsure
        return corners.groupBy { it }.maxByOrNull { it.value.size }?.key ?: Color.WHITE
    }

    private fun splitRecursive(
        bitmap: Bitmap,
        left: Float, top: Float, right: Float, bottom: Float,
        bgColor: Int,
        result: MutableList<RectF>,
        depth: Int = 0
    ) {
        if (depth > 12) {
            result.add(RectF(left, top, right, bottom))
            return
        }
        
        val w = right - left
        val h = bottom - top
        if (w < 50 || h < 50) return // Too small

        // Try to find a horizontal gutter
        val hGutter = findHorizontalGutter(bitmap, left, top, right, bottom, bgColor)
        if (hGutter != null) {
            splitRecursive(bitmap, left, top, right, hGutter.first, bgColor, result, depth + 1)
            splitRecursive(bitmap, left, hGutter.second, right, bottom, bgColor, result, depth + 1)
            return
        }

        // Try to find a vertical gutter
        val vGutter = findVerticalGutter(bitmap, left, top, right, bottom, bgColor)
        if (vGutter != null) {
            splitRecursive(bitmap, left, top, vGutter.first, bottom, bgColor, result, depth + 1)
            splitRecursive(bitmap, vGutter.second, top, right, bottom, bgColor, result, depth + 1)
            return
        }

        // No gutters found, this is a panel
        result.add(RectF(left, top, right, bottom))
    }

    private fun findHorizontalGutter(
        bitmap: Bitmap,
        left: Float, top: Float, right: Float, bottom: Float,
        bgColor: Int
    ): Pair<Float, Float>? {
        val h = bottom - top
        val start = (top + h * 0.05f).toInt()
        val end = (bottom - h * 0.05f).toInt()
        
        var bestGutter: Pair<Int, Int>? = null
        var maxGutterHeight = 0

        var currentGutterStart = -1
        for (y in start..end) {
            if (isRowEmpty(bitmap, left.toInt(), right.toInt(), y, bgColor)) {
                if (currentGutterStart == -1) currentGutterStart = y
            } else {
                if (currentGutterStart != -1) {
                    val gutterHeight = y - currentGutterStart
                    if (gutterHeight > 10 && gutterHeight > maxGutterHeight) {
                        maxGutterHeight = gutterHeight
                        bestGutter = currentGutterStart to y
                    }
                    currentGutterStart = -1
                }
            }
        }
        
        return bestGutter?.let { it.first.toFloat() to it.second.toFloat() }
    }

    private fun findVerticalGutter(
        bitmap: Bitmap,
        left: Float, top: Float, right: Float, bottom: Float,
        bgColor: Int
    ): Pair<Float, Float>? {
        val w = right - left
        val start = (left + w * 0.05f).toInt()
        val end = (right - w * 0.05f).toInt()
        
        var bestGutter: Pair<Int, Int>? = null
        var maxGutterWidth = 0

        var currentGutterStart = -1
        for (x in start..end) {
            if (isColumnEmpty(bitmap, top.toInt(), bottom.toInt(), x, bgColor)) {
                if (currentGutterStart == -1) currentGutterStart = x
            } else {
                if (currentGutterStart != -1) {
                    val gutterWidth = x - currentGutterStart
                    if (gutterWidth > 10 && gutterWidth > maxGutterWidth) {
                        maxGutterWidth = gutterWidth
                        bestGutter = currentGutterStart to x
                    }
                    currentGutterStart = -1
                }
            }
        }
        
        return bestGutter?.let { it.first.toFloat() to it.second.toFloat() }
    }

    private fun isRowEmpty(bitmap: Bitmap, left: Int, right: Int, y: Int, bgColor: Int): Boolean {
        // Sample points to check for content
        val step = maxOf(1, (right - left) / 50)
        for (x in left until right step step) {
            if (!isColorMatch(bitmap.getPixel(x, y), bgColor)) return false
        }
        return true
    }

    private fun isColumnEmpty(bitmap: Bitmap, top: Int, bottom: Int, x: Int, bgColor: Int): Boolean {
        val step = maxOf(1, (bottom - top) / 50)
        for (y in top until bottom step step) {
            if (!isColorMatch(bitmap.getPixel(x, y), bgColor)) return false
        }
        return true
    }

    private fun isColorMatch(c1: Int, c2: Int): Boolean {
        val r = abs(Color.red(c1) - Color.red(c2))
        val g = abs(Color.green(c1) - Color.green(c2))
        val b = abs(Color.blue(c1) - Color.blue(c2))
        return r < 35 && g < 35 && b < 35
    }

    private fun refinePanels(panels: List<RectF>, width: Float, height: Float): List<RectF> {
        if (panels.isEmpty()) return listOf(RectF(0f, 0f, width, height))
        
        // Remove panels that are too small or too thin (likely artifacts)
        val filtered = panels.filter { it.width() > width * 0.1f && it.height() > height * 0.05f }
        if (filtered.isEmpty()) return listOf(RectF(0f, 0f, width, height))

        // Sort in reading order: Top to Bottom, then Right to Left
        val verticalThreshold = height * 0.05f
        return filtered.sortedWith { p1, p2 ->
            if (abs(p1.top - p2.top) < verticalThreshold) {
                p2.right.compareTo(p1.right) // Right to Left
            } else {
                p1.top.compareTo(p2.top) // Top to Bottom
            }
        }
    }
}
