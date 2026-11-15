package com.shinku.reader.presentation.more.stats.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ShareCardContentTest {
    private val content = ShareCardContent(
        levelSummary = "Level 42 · 123456 XP",
        levelProgress = 0.75f,
        persona = "Fixture persona",
        metrics = listOf(
            ShareCardMetric(ShareStatistic.TIME, "Time", "123h"),
            ShareCardMetric(ShareStatistic.CHAPTERS, "Chapters", "98765"),
            ShareCardMetric(ShareStatistic.STREAK, "Streak", "99 days"),
            ShareCardMetric(ShareStatistic.BADGES, "Badges", "24"),
        ),
        genres = listOf("Fixture genre"),
    )

    @Test
    fun defaultExportPayloadContainsNoStatistics() {
        assertEquals(ShareCardContent(), content.selected(emptySet()))
    }

    @Test
    fun includingOneStatisticDoesNotIncludeUnrelatedDetailsAndCanBeReversed() {
        val selected = content.selected(setOf(ShareStatistic.CHAPTERS))
        assertEquals(listOf(content.metrics[1]), selected.metrics)
        assertNull(selected.levelSummary)
        assertNull(selected.levelProgress)
        assertNull(selected.persona)
        assertEquals(emptyList<String>(), selected.genres)
        assertEquals(ShareCardContent(), selected.selected(emptySet()))
        assertEquals(content, content.selected(ShareStatistic.entries.toSet()))
    }
}
