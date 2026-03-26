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

        // Stats Breakdown
        val allManga = mangaRepository.getAll().associateBy { it.id }
        val allChapters = chapterRepository.getChaptersByIds(history.map { it.chapterId }).associateBy { it.id }
        
        val genreDuration = mutableMapOf<String, Long>()
        val authorDuration = mutableMapOf<String, Long>()
        val genreReadCount = mutableMapOf<String, Int>()
        val authorReadCount = mutableMapOf<String, Int>()
        val timeOfDayHistory = mutableMapOf<Int, Long>() // Hour of day -> Duration

        history.forEach { entry ->
            val mangaId = allChapters[entry.chapterId]?.mangaId
            val manga = allManga[mangaId]
            
            // Genres
            manga?.genre?.forEach { genre ->
                genreDuration[genre] = (genreDuration[genre] ?: 0L) + entry.readDuration
                genreReadCount[genre] = (genreReadCount[genre] ?: 0) + 1
            }

            // Authors
            val authors = manga?.author?.split(",", ";", "/")?.map { it.trim() }?.filter { it.isNotBlank() }
            authors?.forEach { author ->
                authorDuration[author] = (authorDuration[author] ?: 0L) + entry.readDuration
                authorReadCount[author] = (authorReadCount[author] ?: 0) + 1
            }

            // Time of Day
            entry.readAt?.let { readAt ->
                val calendar = Calendar.getInstance()
                calendar.time = readAt
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                timeOfDayHistory[hour] = (timeOfDayHistory[hour] ?: 0L) + entry.readDuration
            }
        }

        val badges = calculateBadges(totalDuration, currentStreak, genreReadCount.size)

        return ReadingStats(
            totalReadDuration = totalDuration,
            currentStreak = currentStreak,
            bestGenres = genreDuration.entries.sortedByDescending { it.value }.take(10).map { it.key },
            bestAuthors = authorDuration.entries.sortedByDescending { it.value }.take(10).map { it.key },
            genreReadCount = genreReadCount,
            authorReadCount = authorReadCount,
            timeOfDayHistory = timeOfDayHistory,
            dailyHistory = history.groupBy { truncateDate(it.readAt ?: Date()) }
                .mapValues { it.value.sumOf { h -> h.readDuration } },
            badges = badges
        )
    }

    private fun calculateBadges(totalDuration: Long, currentStreak: Int, genreCount: Int): List<com.shinku.reader.domain.history.model.Badge> {
        val badges = mutableListOf<com.shinku.reader.domain.history.model.Badge>()
        val hoursRead = TimeUnit.MILLISECONDS.toHours(totalDuration)

        // Reading Time Badges
        if (hoursRead >= 10) badges.add(com.shinku.reader.domain.history.model.Badge("time_10", "Novice Reader", "Read for 10 hours", iconId = "time", isEarned = true))
        if (hoursRead >= 100) badges.add(com.shinku.reader.domain.history.model.Badge("time_100", "Dedicated Reader", "Read for 100 hours", iconId = "time", isEarned = true))
        if (hoursRead >= 1000) badges.add(com.shinku.reader.domain.history.model.Badge("time_1000", "Sage Reader", "Read for 1,000 hours", iconId = "time", isEarned = true))

        // Streak Badges
        if (currentStreak >= 7) badges.add(com.shinku.reader.domain.history.model.Badge("streak_7", "Weekly Warrior", "Maintain a 7-day reading streak", iconId = "streak", isEarned = true))
        if (currentStreak >= 30) badges.add(com.shinku.reader.domain.history.model.Badge("streak_30", "Monthly Master", "Maintain a 30-day reading streak", iconId = "streak", isEarned = true))

        // Diversity Badges
        if (genreCount >= 5) badges.add(com.shinku.reader.domain.history.model.Badge("genre_5", "Genre Explorer", "Read from 5 different genres", iconId = "genre", isEarned = true))
        if (genreCount >= 20) badges.add(com.shinku.reader.domain.history.model.Badge("genre_20", "Genre Polymath", "Read from 20 different genres", iconId = "genre", isEarned = true))

        return badges
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
        val bestAuthors: List<String>,
        val genreReadCount: Map<String, Int>,
        val authorReadCount: Map<String, Int>,
        val timeOfDayHistory: Map<Int, Long>,
        val dailyHistory: Map<Date, Long>,
        val badges: List<com.shinku.reader.domain.history.model.Badge>,
    )
}
