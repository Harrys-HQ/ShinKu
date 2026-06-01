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

        var totalPages = 0L
        val dailyPages = mutableMapOf<Date, Long>()
        val dailyDurations = mutableMapOf<Date, Long>()
        var nightOwlDuration = 0L
        var earlyBirdDuration = 0L

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

            // Time of Day and special badges
            entry.readAt?.let { readAt ->
                val calendar = Calendar.getInstance()
                calendar.time = readAt
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                timeOfDayHistory[hour] = (timeOfDayHistory[hour] ?: 0L) + entry.readDuration
                
                if (hour >= 23 || hour <= 4) {
                    nightOwlDuration += entry.readDuration
                } else if (hour in 5..9) {
                    earlyBirdDuration += entry.readDuration
                }

                val dateKey = truncateDate(readAt)
                dailyDurations[dateKey] = (dailyDurations[dateKey] ?: 0L) + entry.readDuration
                
                val chapter = allChapters[entry.chapterId]
                val pages = if (chapter?.read == true) {
                    if (chapter.lastPageRead > 0) chapter.lastPageRead else 20L
                } else {
                    if (chapter != null && chapter.lastPageRead > 0) chapter.lastPageRead else 10L
                }
                totalPages += pages
                dailyPages[dateKey] = (dailyPages[dateKey] ?: 0L) + pages
            }
        }

        val maxDailyDuration = if (dailyDurations.isNotEmpty()) dailyDurations.values.maxOrNull() ?: 0L else 0L
        val averageVelocity = if (totalDuration > 0) {
            (totalPages.toDouble() / (totalDuration.toDouble() / 60000.0))
        } else {
            0.0
        }

        val dailyVelocity = dailyDurations.mapValues { (date, duration) ->
            val pages = dailyPages[date] ?: 0L
            if (duration > 0) {
                (pages.toDouble() / (duration.toDouble() / 60000.0)).coerceAtMost(10.0)
            } else {
                0.0
            }
        }

        val badges = calculateBadges(
            totalDuration = totalDuration,
            currentStreak = currentStreak,
            genreCount = genreReadCount.size,
            nightOwlDuration = nightOwlDuration,
            earlyBirdDuration = earlyBirdDuration,
            maxDailyDuration = maxDailyDuration,
            averageVelocity = averageVelocity,
            chapterCount = history.size
        )

        return ReadingStats(
            totalReadDuration = totalDuration,
            currentStreak = currentStreak,
            bestGenres = genreDuration.entries.sortedByDescending { it.value }.take(10).map { it.key },
            bestAuthors = authorDuration.entries.sortedByDescending { it.value }.take(10).map { it.key },
            genreReadCount = genreReadCount,
            authorReadCount = authorReadCount,
            timeOfDayHistory = timeOfDayHistory,
            dailyHistory = dailyDurations,
            averageVelocity = averageVelocity,
            dailyVelocity = dailyVelocity,
            badges = badges
        )
    }

    private fun calculateBadges(
        totalDuration: Long,
        currentStreak: Int,
        genreCount: Int,
        nightOwlDuration: Long,
        earlyBirdDuration: Long,
        maxDailyDuration: Long,
        averageVelocity: Double,
        chapterCount: Int
    ): List<com.shinku.reader.domain.history.model.Badge> {
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

        // Gamified ShinKu Custom Badges
        if (TimeUnit.MILLISECONDS.toHours(nightOwlDuration) >= 5) {
            badges.add(com.shinku.reader.domain.history.model.Badge("night_owl", "Night Owl", "Read for 5 hours late at night (11 PM - 4 AM)", iconId = "time", isEarned = true))
        }
        if (TimeUnit.MILLISECONDS.toHours(earlyBirdDuration) >= 5) {
            badges.add(com.shinku.reader.domain.history.model.Badge("early_bird", "Early Bird", "Read for 5 hours early in the morning (5 AM - 9 AM)", iconId = "time", isEarned = true))
        }
        if (TimeUnit.MILLISECONDS.toHours(maxDailyDuration) >= 3) {
            badges.add(com.shinku.reader.domain.history.model.Badge("marathoner", "Manga Marathoner", "Read for more than 3 hours in a single day", iconId = "streak", isEarned = true))
        }
        if (chapterCount >= 100) {
            badges.add(com.shinku.reader.domain.history.model.Badge("centurion", "Centurion", "Read 100 chapters in total", iconId = "genre", isEarned = true))
        }
        if (averageVelocity >= 2.5) {
            badges.add(com.shinku.reader.domain.history.model.Badge("speed_demon", "Speed Demon", "Read with an average velocity of over 2.5 pages/min", iconId = "genre", isEarned = true))
        }

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
        val averageVelocity: Double,
        val dailyVelocity: Map<Date, Double>,
        val badges: List<com.shinku.reader.domain.history.model.Badge>,
    )
}
