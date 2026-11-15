package com.shinku.reader.presentation.more.stats.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class ReadingActivityTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun calendarDaysStayAlignedAcrossBothDaylightSavingTransitions() {
        listOf(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 11, 3)).forEach { today ->
            val history = (0L..6L).associate { offset ->
                Date.from(today.minusDays(offset).atStartOfDay(zone).toInstant()) to (offset + 1L)
            }
            val days = readingActivityDays(history, 7, today, zone)
            assertEquals((6L downTo 0L).map { today.minusDays(it) }, days.map { it.date })
            assertEquals((7L downTo 1L).toList(), days.map { it.duration })
        }
    }

    @Test
    fun windowAggregatesOneDayAndExcludesFutureAndOlderActivity() {
        val today = LocalDate.of(2026, 11, 10)
        val history = mapOf(
            Date.from(today.atStartOfDay(zone).toInstant()) to 20L,
            Date.from(today.atTime(23, 0).atZone(zone).toInstant()) to 30L,
            Date.from(today.minusDays(7).atStartOfDay(zone).toInstant()) to 100L,
            Date.from(today.plusDays(1).atStartOfDay(zone).toInstant()) to 100L,
            Date.from(today.minusDays(1).atStartOfDay(zone).toInstant()) to -10L,
        )
        val days = readingActivityDays(history, 7, today, zone)
        assertEquals(7, days.size)
        assertEquals(50L, days.last().duration)
        assertEquals(50L, days.sumOf { it.duration })
    }

    @Test
    fun emptyWindowContainsEveryCalendarDayWithZeroDuration() {
        val today = LocalDate.of(2026, 11, 10)
        val days = readingActivityDays(emptyMap(), 84, today, ZoneId.of("Asia/Shanghai"))
        assertEquals(84, days.size)
        assertEquals(today.minusDays(83), days.first().date)
        assertEquals(today, days.last().date)
        assertEquals(0L, days.sumOf { it.duration })
    }
}
