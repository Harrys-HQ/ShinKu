package com.shinku.reader.ui.browse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrowseTabOrderTest {

    @Test
    fun `hideFeedTab omits feed tab regardless of feedTabInFront`() {
        val expected = listOf(BrowseTabKind.SOURCES, BrowseTabKind.EXTENSIONS, BrowseTabKind.MIGRATE)
        assertEquals(expected, computeBrowseTabOrder(hideFeedTab = true, feedTabInFront = true))
        assertEquals(expected, computeBrowseTabOrder(hideFeedTab = true, feedTabInFront = false))
        assertEquals(0, indexForBrowseTab(expected, BrowseTabKind.SOURCES))
        assertEquals(1, indexForBrowseTab(expected, BrowseTabKind.EXTENSIONS))
        assertEquals(0, indexForBrowseTab(expected, BrowseTabKind.FEED))
    }

    @Test
    fun `feedTabInFront true puts feed before sources`() {
        val order = computeBrowseTabOrder(hideFeedTab = false, feedTabInFront = true)
        assertEquals(
            listOf(BrowseTabKind.FEED, BrowseTabKind.SOURCES, BrowseTabKind.EXTENSIONS, BrowseTabKind.MIGRATE),
            order,
        )
        assertEquals(1, indexForBrowseTab(order, BrowseTabKind.SOURCES))
        assertEquals(2, indexForBrowseTab(order, BrowseTabKind.EXTENSIONS))
    }

    @Test
    fun `feedTabInFront false puts sources before feed`() {
        val order = computeBrowseTabOrder(hideFeedTab = false, feedTabInFront = false)
        assertEquals(
            listOf(BrowseTabKind.SOURCES, BrowseTabKind.FEED, BrowseTabKind.EXTENSIONS, BrowseTabKind.MIGRATE),
            order,
        )
        assertEquals(0, indexForBrowseTab(order, BrowseTabKind.SOURCES))
        assertEquals(2, indexForBrowseTab(order, BrowseTabKind.EXTENSIONS))
    }
}
