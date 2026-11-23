package com.shinku.reader.ui.browse

enum class BrowseTabKind {
    FEED,
    SOURCES,
    EXTENSIONS,
    MIGRATE,
}

fun computeBrowseTabOrder(
    hideFeedTab: Boolean,
    feedTabInFront: Boolean,
): List<BrowseTabKind> {
    return when {
        hideFeedTab -> listOf(
            BrowseTabKind.SOURCES,
            BrowseTabKind.EXTENSIONS,
            BrowseTabKind.MIGRATE,
        )
        feedTabInFront -> listOf(
            BrowseTabKind.FEED,
            BrowseTabKind.SOURCES,
            BrowseTabKind.EXTENSIONS,
            BrowseTabKind.MIGRATE,
        )
        else -> listOf(
            BrowseTabKind.SOURCES,
            BrowseTabKind.FEED,
            BrowseTabKind.EXTENSIONS,
            BrowseTabKind.MIGRATE,
        )
    }
}

fun indexForBrowseTab(
    order: List<BrowseTabKind>,
    target: BrowseTabKind,
): Int = order.indexOf(target).coerceAtLeast(0)
