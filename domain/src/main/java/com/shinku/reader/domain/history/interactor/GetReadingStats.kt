package com.shinku.reader.domain.history.interactor

import com.shinku.reader.domain.history.repository.HistoryRepository
import com.shinku.reader.domain.manga.repository.MangaRepository
import com.shinku.reader.domain.chapter.repository.ChapterRepository
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class GetReadingStats(
    private val historyRepository: HistoryRepository,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(): ReadingStats {
        val history = historyRepository.getAllHistory()
        val totalDuration = history.sumOf { it.readDuration }
        
        // Calculate Streaks
        val readDates = history.mapNotNull { it.readAt }
            .map { truncateDate(it) }
            .distinct()
            .sortedDescending()

        var currentStreak = 0
        if (readDates.isNotEmpty()) {
            val today = truncateDate(Date())
            val yesterday = Date(today.time - TimeUnit.DAYS.toMillis(1))
            
            if (readDates[0] == today || readDates[0] == yesterday) {
                currentStreak = 1
                for (i in 0 until readDates.size - 1) {
                    val current = readDates[i]
                    val next = readDates[i + 1]
                    if (current.time - next.time <= TimeUnit.DAYS.toMillis(1)) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
        }

        // Genre Breakdown
        val allManga = mangaRepository.getAll().associateBy { it.id }
        val allChapters = chapterRepository.getChaptersByIds(history.map { it.chapterId }).associateBy { it.id }
        
        val genreDuration = mutableMapOf<String, Long>()
        history.forEach { entry ->
            val mangaId = allChapters[entry.chapterId]?.mangaId
            val manga = allManga[mangaId]
            manga?.genre?.forEach { genre ->
                genreDuration[genre] = (genreDuration[genre] ?: 0L) + entry.readDuration
            }
        }

        return ReadingStats(
            totalReadDuration = totalDuration,
            currentStreak = currentStreak,
            bestGenres = genreDuration.entries.sortedByDescending { it.value }.take(5).map { it.key },
            dailyHistory = history.groupBy { truncateDate(it.readAt ?: Date()) }
                .mapValues { it.value.sumOf { h -> h.readDuration } }
        )
    }

    private fun truncateDate(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    data class ReadingStats(
        val totalReadDuration: Long,
        val currentStreak: Int,
        val bestGenres: List<String>,
        val dailyHistory: Map<Date, Long>,
    )
}
