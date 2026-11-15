package com.shinku.reader.presentation.more.stats.components

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal data class ReadingActivityDay(val date: LocalDate, val duration: Long)

/** Use calendar dates so a daylight-saving transition cannot shift or lose a reading day. */
internal fun readingActivityDays(
    history: Map<Date, Long>,
    dayCount: Int,
    today: LocalDate,
    zone: ZoneId,
): List<ReadingActivityDay> {
    require(dayCount > 0)
    val durations = history.entries.groupBy { it.key.toInstant().atZone(zone).toLocalDate() }
        .mapValues { (_, entries) -> entries.sumOf { it.value.coerceAtLeast(0L) } }
    return (dayCount - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        ReadingActivityDay(date, durations[date] ?: 0L)
    }
}
