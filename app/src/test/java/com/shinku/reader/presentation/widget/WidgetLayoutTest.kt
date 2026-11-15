package com.shinku.reader.presentation.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.widget.util.widgetGridForSizes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetLayoutTest {
    @Test
    fun `binding before sizes are reported keeps a valid layout`() {
        assertEquals(1 to 1, widgetGridForSizes(emptyList(), 0.dp, 0.dp))
        assertEquals(3 to 4, widgetGridForSizes(listOf(DpSize(64.dp, 95.dp), DpSize(256.dp, 285.dp)), 0.dp, 0.dp))
        assertEquals(1 to 1, widgetGridForSizes(listOf(DpSize(1.dp, 1.dp)), 32.dp, 32.dp))
    }
}
