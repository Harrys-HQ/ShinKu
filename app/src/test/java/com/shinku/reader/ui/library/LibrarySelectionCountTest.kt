package com.shinku.reader.ui.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibrarySelectionCountTest {

    @Test
    fun `empty selection returns zero`() {
        assertEquals(0, countSelectedOutOfView(emptySet(), listOf(1L, 2L, 3L)))
    }

    @Test
    fun `all selected ids visible returns zero`() {
        assertEquals(0, countSelectedOutOfView(setOf(1L, 3L), listOf(1L, 2L, 3L)))
    }

    @Test
    fun `selected ids outside visible list returns exact hidden count`() {
        assertEquals(2, countSelectedOutOfView(setOf(1L, 2L, 4L, 5L), listOf(1L, 2L, 3L)))
    }

    @Test
    fun `empty visible list returns full selection size`() {
        assertEquals(3, countSelectedOutOfView(setOf(10L, 20L, 30L), emptyList()))
    }
}
