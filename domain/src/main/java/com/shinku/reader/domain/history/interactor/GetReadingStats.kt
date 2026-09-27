package com.shinku.reader.domain.history.interactor

import com.shinku.reader.domain.history.model.Badge
import com.shinku.reader.domain.history.model.BadgeCategory
import com.shinku.reader.domain.history.model.BadgeTier
import com.shinku.reader.domain.history.repository.HistoryRepository
import com.shinku.reader.domain.manga.repository.MangaRepository
import com.shinku.reader.domain.chapter.repository.ChapterRepository
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GetReadingStats(
    private val historyRepository: HistoryRepository,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(
        libraryMangaCount: Int = 0,
        completedMangaCount: Int = 0,
        trackedTitleCount: Int = 0,
        trackerCount: Int = 0,
    ): ReadingStats {
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
        var lunchDuration = 0L
        var weekendDuration = 0L

        var actionChapters = 0
        var romanceChapters = 0
        var fantasyChapters = 0
        var solChapters = 0

        history.forEach { entry ->
            val mangaId = allChapters[entry.chapterId]?.mangaId
            val manga = allManga[mangaId]
            
            // Genres
            manga?.genre?.forEach { genre ->
                genreDuration[genre] = (genreDuration[genre] ?: 0L) + entry.readDuration
                genreReadCount[genre] = (genreReadCount[genre] ?: 0) + 1

                val g = genre.lowercase(Locale.ENGLISH)
                if (g.contains("action") || g.contains("shounen")) actionChapters++
                if (g.contains("romance") || g.contains("shoujo")) romanceChapters++
                if (g.contains("fantasy") || g.contains("isekai")) fantasyChapters++
                if (g.contains("slice of life") || g.contains("comedy")) solChapters++
            }

            // Authors
            val authors = manga?.author?.split(",", ";", "/")?.map { it.trim() }?.filter { it.isNotBlank() }
            authors?.forEach { author ->
                authorDuration[author] = (authorDuration[author] ?: 0L) + entry.readDuration
                authorReadCount[author] = (authorReadCount[author] ?: 0) + 1
            }

            // Time of Day and special habits
            entry.readAt?.let { readAt ->
                val calendar = Calendar.getInstance()
                calendar.time = readAt
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                timeOfDayHistory[hour] = (timeOfDayHistory[hour] ?: 0L) + entry.readDuration
                
                if (hour >= 23 || hour <= 4) {
                    nightOwlDuration += entry.readDuration
                } else if (hour in 5..9) {
                    earlyBirdDuration += entry.readDuration
                } else if (hour in 12..14) {
                    lunchDuration += entry.readDuration
                }

                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    weekendDuration += entry.readDuration
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

        val actualLibraryCount = if (libraryMangaCount > 0) {
            libraryMangaCount
        } else {
            allManga.values.count { it.favorite }
        }

        val actualCompletedCount = if (completedMangaCount > 0) {
            completedMangaCount
        } else {
            allManga.values.count { it.favorite && it.status.toInt() == 2 }
        }

        val badges = calculateBadges(
            totalDuration = totalDuration,
            currentStreak = currentStreak,
            genreCount = genreReadCount.size,
            nightOwlDuration = nightOwlDuration,
            earlyBirdDuration = earlyBirdDuration,
            lunchDuration = lunchDuration,
            weekendDuration = weekendDuration,
            maxDailyDuration = maxDailyDuration,
            averageVelocity = averageVelocity,
            chapterCount = history.size,
            libraryCount = actualLibraryCount,
            completedCount = actualCompletedCount,
            actionChapters = actionChapters,
            romanceChapters = romanceChapters,
            fantasyChapters = fantasyChapters,
            solChapters = solChapters,
            trackedTitleCount = trackedTitleCount,
            trackerCount = trackerCount,
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
        lunchDuration: Long,
        weekendDuration: Long,
        maxDailyDuration: Long,
        averageVelocity: Double,
        chapterCount: Int,
        libraryCount: Int,
        completedCount: Int,
        actionChapters: Int,
        romanceChapters: Int,
        fantasyChapters: Int,
        solChapters: Int,
        trackedTitleCount: Int,
        trackerCount: Int,
    ): List<Badge> {
        val badges = mutableListOf<Badge>()
        val hoursRead = TimeUnit.MILLISECONDS.toHours(totalDuration)
        val nightOwlHours = TimeUnit.MILLISECONDS.toHours(nightOwlDuration)
        val earlyBirdHours = TimeUnit.MILLISECONDS.toHours(earlyBirdDuration)
        val lunchHours = TimeUnit.MILLISECONDS.toHours(lunchDuration)
        val weekendHours = TimeUnit.MILLISECONDS.toHours(weekendDuration)
        val maxDailyHours = TimeUnit.MILLISECONDS.toHours(maxDailyDuration)

        // 1. Reading Time Progression
        badges.add(createBadge("time_1", "First Page Turned", "Read for at least 1 hour", BadgeCategory.TIME, BadgeTier.BRONZE, "time", hoursRead, 1, "h", "Spend 1 hour reading any manga in your library."))
        badges.add(createBadge("time_10", "Novice Reader", "Read for 10 hours", BadgeCategory.TIME, BadgeTier.BRONZE, "time", hoursRead, 10, "h", "Log 10 hours of active reading time."))
        badges.add(createBadge("time_50", "Avid Reader", "Read for 50 hours", BadgeCategory.TIME, BadgeTier.SILVER, "time", hoursRead, 50, "h", "Keep immersing yourself to reach 50 reading hours."))
        badges.add(createBadge("time_100", "Dedicated Reader", "Read for 100 hours", BadgeCategory.TIME, BadgeTier.SILVER, "time", hoursRead, 100, "h", "A century of reading hours dedicated to your stories."))
        badges.add(createBadge("time_250", "Seasoned Reader", "Read for 250 hours", BadgeCategory.TIME, BadgeTier.GOLD, "time", hoursRead, 250, "h", "Log 250 hours across your manga collection."))
        badges.add(createBadge("time_500", "Master Reader", "Read for 500 hours", BadgeCategory.TIME, BadgeTier.GOLD, "time", hoursRead, 500, "h", "Achieve 500 reading hours—a true manga connoisseur."))
        badges.add(createBadge("time_1000", "Sage Reader", "Read for 1,000 hours", BadgeCategory.TIME, BadgeTier.PLATINUM, "time", hoursRead, 1000, "h", "Cross the quadruple-digit milestone of 1,000 hours."))
        badges.add(createBadge("time_2500", "Living Library", "Read for 2,500 hours", BadgeCategory.TIME, BadgeTier.MYTHIC, "time", hoursRead, 2500, "h", "A mythic realm of devotion with over 2,500 hours read."))

        // 2. Chapter Conquest
        badges.add(createBadge("chap_25", "Chapter Initiate", "Read 25 chapters", BadgeCategory.CHAPTERS, BadgeTier.BRONZE, "chapter", chapterCount, 25, "", "Complete your first 25 chapters."))
        badges.add(createBadge("centurion", "Centurion", "Read 100 chapters in total", BadgeCategory.CHAPTERS, BadgeTier.BRONZE, "chapter", chapterCount, 100, "", "Read 100 chapters across any manga series."))
        badges.add(createBadge("chap_250", "Volume Devourer", "Read 250 chapters", BadgeCategory.CHAPTERS, BadgeTier.SILVER, "chapter", chapterCount, 250, "", "Reach 250 chapters completed."))
        badges.add(createBadge("chap_500", "Legionnaire", "Read 500 chapters", BadgeCategory.CHAPTERS, BadgeTier.SILVER, "chapter", chapterCount, 500, "", "Complete 500 manga chapters."))
        badges.add(createBadge("chap_1000", "Millennium Club", "Read 1,000 chapters", BadgeCategory.CHAPTERS, BadgeTier.GOLD, "chapter", chapterCount, 1000, "", "Enter the prestigious 1,000-chapter club."))
        badges.add(createBadge("chap_2500", "Grand Chronicler", "Read 2,500 chapters", BadgeCategory.CHAPTERS, BadgeTier.GOLD, "chapter", chapterCount, 2500, "", "Reach 2,500 chapters documented in history."))
        badges.add(createBadge("chap_5000", "Archivist Supreme", "Read 5,000 chapters", BadgeCategory.CHAPTERS, BadgeTier.PLATINUM, "chapter", chapterCount, 5000, "", "An astonishing library of 5,000 chapters read."))
        badges.add(createBadge("chap_10000", "Transcendent", "Read 10,000 chapters", BadgeCategory.CHAPTERS, BadgeTier.MYTHIC, "chapter", chapterCount, 10000, "", "Supreme reader mastery with 10,000 chapters read."))

        // 3. Daily Streaks
        badges.add(createBadge("streak_3", "Ignition", "Maintain a 3-day reading streak", BadgeCategory.STREAKS, BadgeTier.BRONZE, "streak", currentStreak, 3, " days", "Read at least one chapter every day for 3 days."))
        badges.add(createBadge("streak_7", "Weekly Warrior", "Maintain a 7-day reading streak", BadgeCategory.STREAKS, BadgeTier.BRONZE, "streak", currentStreak, 7, " days", "Keep your reading habit unbroken for a full week."))
        badges.add(createBadge("streak_14", "Fortnight Flame", "Maintain a 14-day reading streak", BadgeCategory.STREAKS, BadgeTier.SILVER, "streak", currentStreak, 14, " days", "Two consecutive weeks of daily reading."))
        badges.add(createBadge("streak_30", "Monthly Master", "Maintain a 30-day reading streak", BadgeCategory.STREAKS, BadgeTier.SILVER, "streak", currentStreak, 30, " days", "Read every day for an entire 30-day month."))
        badges.add(createBadge("streak_60", "Bimonthly Titan", "Maintain a 60-day reading streak", BadgeCategory.STREAKS, BadgeTier.GOLD, "streak", currentStreak, 60, " days", "Two months without missing a single day of reading."))
        badges.add(createBadge("streak_100", "Centennial Streak", "Maintain a 100-day reading streak", BadgeCategory.STREAKS, BadgeTier.GOLD, "streak", currentStreak, 100, " days", "Cross the historic 100-day consecutive streak."))
        badges.add(createBadge("streak_180", "Half-Year Habit", "Maintain a 180-day reading streak", BadgeCategory.STREAKS, BadgeTier.PLATINUM, "streak", currentStreak, 180, " days", "Half a year of continuous daily manga enjoyment."))
        badges.add(createBadge("streak_365", "Year of the Reader", "Maintain a 365-day reading streak", BadgeCategory.STREAKS, BadgeTier.MYTHIC, "streak", currentStreak, 365, " days", "A legendary achievement: reading daily for 365 days."))

        // 4. Collection & Completion
        badges.add(createBadge("lib_10", "Personal Shelf", "Add 10 manga to your library", BadgeCategory.COLLECTION, BadgeTier.BRONZE, "library", libraryCount, 10, "", "Bookmark 10 titles to your personal library."))
        badges.add(createBadge("lib_50", "Bookcase Builder", "Add 50 manga to your library", BadgeCategory.COLLECTION, BadgeTier.BRONZE, "library", libraryCount, 50, "", "Expand your reading backlog to 50 series."))
        badges.add(createBadge("lib_100", "Bibliophile", "Add 100 manga to your library", BadgeCategory.COLLECTION, BadgeTier.SILVER, "library", libraryCount, 100, "", "Curate a 100-title personal manga sanctuary."))
        badges.add(createBadge("lib_250", "Sanctuary Keeper", "Add 250 manga to your library", BadgeCategory.COLLECTION, BadgeTier.GOLD, "library", libraryCount, 250, "", "Reach 250 manga in your active library collection."))
        badges.add(createBadge("lib_500", "Grand Curator", "Add 500 manga to your library", BadgeCategory.COLLECTION, BadgeTier.PLATINUM, "library", libraryCount, 500, "", "A colossal library of 500 saved manga titles."))
        badges.add(createBadge("comp_1", "First Finale", "Complete 1 manga title", BadgeCategory.COLLECTION, BadgeTier.BRONZE, "library", completedCount, 1, "", "Finish reading all chapters of a completed manga."))
        badges.add(createBadge("comp_5", "Series Finisher", "Complete 5 manga titles", BadgeCategory.COLLECTION, BadgeTier.SILVER, "library", completedCount, 5, "", "See 5 complete stories through to their ending."))
        badges.add(createBadge("comp_25", "Storyline Connoisseur", "Complete 25 manga titles", BadgeCategory.COLLECTION, BadgeTier.GOLD, "library", completedCount, 25, "", "Finish 25 entire manga series."))
        badges.add(createBadge("comp_50", "Master of Endings", "Complete 50 manga titles", BadgeCategory.COLLECTION, BadgeTier.PLATINUM, "library", completedCount, 50, "", "Complete 50 full manga titles from start to finish."))

        // 5. Genre Diversity & Specialization
        badges.add(createBadge("genre_5", "Genre Explorer", "Read from 5 different genres", BadgeCategory.GENRES, BadgeTier.BRONZE, "genre", genreCount, 5, "", "Explore stories across 5 diverse manga genres."))
        badges.add(createBadge("genre_15", "Genre Adventurer", "Read from 15 different genres", BadgeCategory.GENRES, BadgeTier.SILVER, "genre", genreCount, 15, "", "Broaden your palate with 15 different genres."))
        badges.add(createBadge("genre_20", "Genre Polymath", "Read from 20 different genres", BadgeCategory.GENRES, BadgeTier.GOLD, "genre", genreCount, 20, "", "Read across 20 distinct genres."))
        badges.add(createBadge("genre_35", "Omnivore", "Read from 35 different genres", BadgeCategory.GENRES, BadgeTier.PLATINUM, "genre", genreCount, 35, "", "An eclectic taste spanning 35 different manga genres."))
        badges.add(createBadge("genre_action", "Shounen Spirit", "Read 30 Action/Shounen chapters", BadgeCategory.GENRES, BadgeTier.SILVER, "genre", actionChapters, 30, "", "Read 30 chapters from Action or Shounen manga."))
        badges.add(createBadge("genre_romance", "Heartstrings", "Read 20 Romance/Shoujo chapters", BadgeCategory.GENRES, BadgeTier.SILVER, "genre", romanceChapters, 20, "", "Read 20 chapters of Romance or Shoujo titles."))
        badges.add(createBadge("genre_fantasy", "Isekai Wanderer", "Read 30 Fantasy/Isekai chapters", BadgeCategory.GENRES, BadgeTier.SILVER, "genre", fantasyChapters, 30, "", "Venture through 30 Fantasy or Isekai chapters."))
        badges.add(createBadge("genre_sol", "Cozy Corner", "Read 20 Slice of Life chapters", BadgeCategory.GENRES, BadgeTier.SILVER, "genre", solChapters, 20, "", "Savor 20 chapters of Slice of Life or Comedy."))

        // 6. Reading Habits & Quirks
        badges.add(createBadge("night_owl", "Night Owl", "Read for 5 hours late at night (11 PM - 4 AM)", BadgeCategory.HABITS, BadgeTier.SILVER, "time", nightOwlHours, 5, "h", "Spend 5 hours reading late into the midnight hours."))
        badges.add(createBadge("night_owl_25", "Creature of the Night", "Read for 25 hours late at night (11 PM - 4 AM)", BadgeCategory.HABITS, BadgeTier.GOLD, "time", nightOwlHours, 25, "h", "A midnight specialist: 25 hours read in the dark."))
        badges.add(createBadge("early_bird", "Early Bird", "Read for 5 hours early in the morning (5 AM - 9 AM)", BadgeCategory.HABITS, BadgeTier.SILVER, "time", earlyBirdHours, 5, "h", "Start your mornings with 5 hours of dawn reading."))
        badges.add(createBadge("early_bird_25", "Dawn Patrol", "Read for 25 hours early in the morning (5 AM - 9 AM)", BadgeCategory.HABITS, BadgeTier.GOLD, "time", earlyBirdHours, 25, "h", "25 hours read with your morning brew."))
        badges.add(createBadge("lunch_reader", "Lunchtime Escape", "Read for 5 hours during midday (12 PM - 2 PM)", BadgeCategory.HABITS, BadgeTier.SILVER, "time", lunchHours, 5, "h", "Enjoy 5 hours of reading during lunch breaks."))
        badges.add(createBadge("marathoner", "Manga Marathoner", "Read for more than 3 hours in a single day", BadgeCategory.HABITS, BadgeTier.SILVER, "streak", maxDailyHours, 3, "h", "Spend 3 hours reading in a single calendar day."))
        badges.add(createBadge("marathon_6", "All-Day Binge", "Read for more than 6 hours in a single day", BadgeCategory.HABITS, BadgeTier.GOLD, "streak", maxDailyHours, 6, "h", "The ultimate binge: 6 hours read in one day."))
        badges.add(createBadge("weekend_war", "Weekend Warrior", "Read for more than 5 hours on weekends", BadgeCategory.HABITS, BadgeTier.SILVER, "time", weekendHours, 5, "h", "Log 5 hours of reading on Saturdays and Sundays."))

        // 7. Velocity, Tracking & Special
        badges.add(createBadge("speed_demon", "Speed Demon", "Read with an average velocity over 2.5 pages/min", BadgeCategory.SPECIAL, BadgeTier.SILVER, "genre", averageVelocity, 2.5, " p/m", "Fast-paced immersion with an average speed > 2.5 pages/min."))
        badges.add(createBadge("speed_40", "Lightning Reader", "Read with an average velocity over 4.0 pages/min", BadgeCategory.SPECIAL, BadgeTier.GOLD, "genre", averageVelocity, 4.0, " p/m", "Exceptional speed reading at over 4.0 pages/min."))
        badges.add(createBadge("tracker_1", "Tracker Linked", "Connect at least 1 tracking service", BadgeCategory.SPECIAL, BadgeTier.BRONZE, "library", trackerCount, 1, "", "Link an AniList, MAL, Kitsu, or other tracking account."))
        badges.add(createBadge("tracker_50", "Synced Scholar", "Track 50 manga titles with online trackers", BadgeCategory.SPECIAL, BadgeTier.SILVER, "library", trackedTitleCount, 50, "", "Keep 50 manga titles synced with your tracker."))
        badges.add(createBadge("tracker_100", "Master of Records", "Track 100 manga titles with online trackers", BadgeCategory.SPECIAL, BadgeTier.GOLD, "library", trackedTitleCount, 100, "", "A synchronized record keeper with 100 tracked manga."))

        return badges
    }

    private fun createBadge(
        id: String,
        name: String,
        description: String,
        category: BadgeCategory,
        tier: BadgeTier,
        iconId: String,
        current: Number,
        target: Number,
        unit: String = "",
        hint: String = "",
    ): Badge {
        val currentVal = current.toDouble()
        val targetVal = target.toDouble()
        val isEarned = currentVal >= targetVal
        val progress = if (targetVal > 0.0) (currentVal / targetVal).toFloat().coerceIn(0f, 1f) else 0f

        val formattedCurrent = if (current is Double || current is Float) {
            "%.1f".format(Locale.ENGLISH, currentVal)
        } else {
            current.toLong().toString()
        }
        val formattedTarget = if (target is Double || target is Float) {
            "%.1f".format(Locale.ENGLISH, targetVal)
        } else {
            target.toLong().toString()
        }

        val progressText = if (isEarned) {
            if (unit.isNotBlank()) "$formattedTarget$unit / $formattedTarget$unit" else "$formattedTarget / $formattedTarget"
        } else {
            if (unit.isNotBlank()) "$formattedCurrent$unit / $formattedTarget$unit" else "$formattedCurrent / $formattedTarget"
        }

        return Badge(
            id = id,
            name = name,
            description = description,
            iconId = iconId,
            isEarned = isEarned,
            progress = progress,
            progressText = progressText,
            tier = tier,
            category = category,
            hint = hint.ifBlank { "Reach $formattedTarget$unit to unlock this achievement." },
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
        val bestAuthors: List<String>,
        val genreReadCount: Map<String, Int>,
        val authorReadCount: Map<String, Int>,
        val timeOfDayHistory: Map<Int, Long>,
        val dailyHistory: Map<Date, Long>,
        val averageVelocity: Double,
        val dailyVelocity: Map<Date, Double>,
        val badges: List<Badge>,
    )
}
