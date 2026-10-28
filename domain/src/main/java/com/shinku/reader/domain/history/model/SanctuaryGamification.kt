package com.shinku.reader.domain.history.model

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val xpReward: Int,
    val isCompleted: Boolean = current >= target,
) {
    val progress: Float
        get() = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
}

data class SanctuaryLevelInfo(
    val totalXp: Long,
    val level: Int,
    val currentLevelXp: Long,
    val nextLevelXp: Long,
    val levelProgress: Float,
    val defaultRankTitle: String,
    val streakShields: Int,
    val shieldProtectedDays: Int,
    val dailyQuests: List<DailyQuest>,
)

object SanctuaryXpEngine {

    fun calculate(
        chaptersRead: Int,
        totalDurationMs: Long,
        currentStreak: Int,
        completedMangaCount: Int,
        earnedBadges: List<Badge> = emptyList(),
        dailyChaptersToday: Int = 0,
        dailyMinutesToday: Int = 0,
        shieldProtectedDays: Int = 0,
    ): SanctuaryLevelInfo {
        val hoursRead = totalDurationMs / (1000L * 60L * 60L)
        val badgeXp = earnedBadges.sumOf { badge ->
            when (badge.tier) {
                BadgeTier.BRONZE -> 50L
                BadgeTier.SILVER -> 100L
                BadgeTier.GOLD -> 200L
                BadgeTier.PLATINUM -> 350L
                BadgeTier.MYTHIC -> 500L
            }
        }

        val dailyQuests = listOf(
            DailyQuest(
                id = "daily_immerse",
                title = "Daily Immersion",
                description = "Read at least 1 chapter today",
                current = dailyChaptersToday.coerceAtMost(1),
                target = 1,
                xpReward = 50,
            ),
            DailyQuest(
                id = "daily_momentum",
                title = "Chapter Momentum",
                description = "Complete 3 chapters today",
                current = dailyChaptersToday.coerceAtMost(3),
                target = 3,
                xpReward = 75,
            ),
            DailyQuest(
                id = "daily_focus",
                title = "Deep Sanctuary Focus",
                description = "Spend 15 minutes reading today",
                current = dailyMinutesToday.coerceAtMost(15),
                target = 15,
                xpReward = 100,
            ),
        )

        val questBonusXp = dailyQuests.filter { it.isCompleted }.sumOf { it.xpReward.toLong() }

        val totalXp = (chaptersRead.toLong() * 25L) +
            (hoursRead * 40L) +
            (currentStreak.toLong() * 60L) +
            (completedMangaCount.toLong() * 120L) +
            badgeXp +
            questBonusXp

        // Progressive RPG Level Curve: Level N requires 300 * N XP for the next level
        var level = 1
        var remainingXp = totalXp
        var xpForNext = 300L
        while (remainingXp >= xpForNext) {
            remainingXp -= xpForNext
            level++
            xpForNext = 300L + (level - 1) * 150L
        }

        val progress = if (xpForNext > 0L) {
            (remainingXp.toFloat() / xpForNext.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val rankTitle = when {
            level >= 50 -> "Mythic Sovereign"
            level >= 35 -> "Sanctuary Grandmaster"
            level >= 25 -> "Sanctuary Master"
            level >= 18 -> "Grand Chronicler"
            level >= 12 -> "Manga Scholar"
            level >= 7 -> "Avid Reader"
            level >= 3 -> "Page Turner"
            level >= 2 -> "Apprentice"
            else -> "Sanctuary Initiate"
        }

        // Earn 1 Streak Shield for every 7 days of streak (plus 1 base shield), minus shields used
        val earnedShields = 1 + (currentStreak / 7) + if (dailyQuests.all { it.isCompleted }) 1 else 0
        val availableShields = (earnedShields - shieldProtectedDays).coerceIn(0, 5)

        return SanctuaryLevelInfo(
            totalXp = totalXp,
            level = level,
            currentLevelXp = remainingXp,
            nextLevelXp = xpForNext,
            levelProgress = progress,
            defaultRankTitle = rankTitle,
            streakShields = availableShields,
            shieldProtectedDays = shieldProtectedDays,
            dailyQuests = dailyQuests,
        )
    }
}
