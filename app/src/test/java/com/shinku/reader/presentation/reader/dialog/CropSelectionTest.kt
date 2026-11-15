package com.shinku.reader.presentation.reader.dialog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CropSelectionTest {
    @Test
    fun `drawing in the bottom right letterbox leaves room to resize`() {
        val drawn = cropSelection(1f, 1f, 1f, 1f)
        assertEquals(1f, drawn.right)
        assertEquals(1f, drawn.bottom)
        val resized = cropSelection(drawn.left, drawn.top, 1f, 1f, 0.05f, 0.03f)
        assertTrue(resized.left >= 0f && resized.top >= 0f)
        assertTrue(resized.width >= 0.049f && resized.height >= 0.029f)
        assertTrue(resized.right <= 1f && resized.bottom <= 1f)
    }

    @Test
    fun `regular selections retain their coordinates and outside drags stay bounded`() {
        val normal = cropSelection(0.2f, 0.35f, 0.8f, 0.55f)
        assertEquals(0.2f, normal.left)
        assertEquals(0.55f, normal.bottom)
        for (x in listOf(-1f, 0f, 0.99f, 1f, 2f)) {
            for (y in listOf(-1f, 0f, 0.99f, 1f, 2f)) {
                val rect = cropSelection(x, y, x, y)
                assertTrue(rect.left >= 0f && rect.top >= 0f && rect.right <= 1f && rect.bottom <= 1f)
                assertTrue(rect.width > 0f && rect.height > 0f)
            }
        }
    }
}
