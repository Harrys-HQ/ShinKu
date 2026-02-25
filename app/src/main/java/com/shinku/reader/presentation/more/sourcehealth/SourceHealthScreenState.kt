package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.runtime.Immutable
import com.shinku.reader.domain.source.model.SourceHealth
import eu.kanade.tachiyomi.source.Source

@Immutable
sealed interface SourceHealthScreenState {
    data object Loading : SourceHealthScreenState
    data class Success(
        val installedList: List<SourceHealthItem>,
        val repoList: List<SourceHealthItem>,
        val sortMode: SourceHealthSort = SourceHealthSort.Health,
    ) : SourceHealthScreenState
}

enum class SourceHealthSort {
    Health,
    Speed,
    Name
}

@Immutable
data class SourceHealthItem(
    val source: Source,
    val health: SourceHealth?,
)
