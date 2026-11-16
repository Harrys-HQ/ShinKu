package com.shinku.reader.domain.source.service

/**
 * Feed composition limits.
 *
 * The old legacy rule was a flat 10 sources for the entire feed. The limit is now centralized
 * to allow power readers to follow up to [MAX_SOURCES_PER_FEED] sources.
 */
object FeedLimits {
    const val MAX_SOURCES_PER_FEED: Int = 25
}
