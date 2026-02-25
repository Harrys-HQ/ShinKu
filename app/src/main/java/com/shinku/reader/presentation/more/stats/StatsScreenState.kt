package com.shinku.reader.presentation.more.stats

import androidx.compose.runtime.Immutable
import com.shinku.reader.presentation.more.stats.data.StatsData

sealed interface StatsScreenState {
    @Immutable
    data object Loading : StatsScreenState

    @Immutable
    data class Success(
        val overview: StatsData.Overview,
        val titles: StatsData.Titles,
        val chapters: StatsData.Chapters,
        val trackers: StatsData.Trackers,
        // SY -->
        val streaks: StatsData.Streaks,
        val genres: StatsData.Genres,
        // SY <--
    ) : StatsScreenState
}
