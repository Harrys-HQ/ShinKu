package com.shinku.reader.domain.history.model

enum class BadgeTier(val label: String, val icon: String) {
    BRONZE("Bronze", "🥉"),
    SILVER("Silver", "🥈"),
    GOLD("Gold", "🥇"),
    PLATINUM("Platinum", "💎"),
    MYTHIC("Mythic", "👑"),
}

enum class BadgeCategory(val label: String) {
    ALL("All"),
    TIME("Time"),
    CHAPTERS("Chapters"),
    STREAKS("Streaks"),
    COLLECTION("Collection"),
    GENRES("Genres"),
    HABITS("Habits"),
    SPECIAL("Special"),
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconId: String? = null,
    val isEarned: Boolean = false,
    val progress: Float = 0f,
    val progressText: String = "",
    val tier: BadgeTier = BadgeTier.BRONZE,
    val category: BadgeCategory = BadgeCategory.TIME,
    val hint: String = "",
)
