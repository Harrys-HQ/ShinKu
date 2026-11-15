package com.shinku.reader.presentation.more.stats.components

internal enum class ShareStatistic { LEVEL, TIME, CHAPTERS, STREAK, BADGES, PATTERNS }

internal data class ShareCardMetric(val statistic: ShareStatistic, val label: String, val value: String)

/** Preview and exporter receive the same selected payload, never the unfiltered statistics. */
internal data class ShareCardContent(
    val levelSummary: String? = null,
    val levelProgress: Float? = null,
    val persona: String? = null,
    val metrics: List<ShareCardMetric> = emptyList(),
    val genres: List<String> = emptyList(),
) {
    fun selected(statistics: Set<ShareStatistic>) = copy(
        levelSummary = levelSummary.takeIf { ShareStatistic.LEVEL in statistics },
        levelProgress = levelProgress.takeIf { ShareStatistic.LEVEL in statistics },
        persona = persona.takeIf { ShareStatistic.PATTERNS in statistics },
        metrics = metrics.filter { it.statistic in statistics },
        genres = genres.takeIf { ShareStatistic.PATTERNS in statistics }.orEmpty(),
    )
}
