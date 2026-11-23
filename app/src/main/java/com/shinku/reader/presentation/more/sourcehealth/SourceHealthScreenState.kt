package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.runtime.Immutable
import com.shinku.reader.data.diagnostics.LiveHealthScanState
import com.shinku.reader.domain.diagnostics.RepositoryHealthCheck
import com.shinku.reader.domain.source.model.SourceHealth
import eu.kanade.tachiyomi.source.Source

@Immutable
sealed interface SourceHealthScreenState {
    data object Loading : SourceHealthScreenState
    data class Success(
        val installedList: List<SourceHealthItem>,
        val repoList: List<SourceHealthItem>,
        val sortMode: SourceHealthSort = SourceHealthSort.Health,
        val testingSourceIds: Set<Long> = emptySet(),
        val liveScan: LiveHealthScanState = LiveHealthScanState(),
        val repoChecks: List<RepositoryHealthCheck> = emptyList(),
    ) : SourceHealthScreenState
}

enum class SourceHealthSort {
    Health,
    Speed,
    Name,
}

enum class ExtensionDimensionStatus {
    LOADED,
    UPDATE_AVAILABLE,
    OBSOLETE,
    UNTRUSTED,
    LOAD_FAILED,
    NOT_INSTALLED,
    UNKNOWN,
}

@Immutable
data class SourceHealthItem(
    val source: Source,
    val health: SourceHealth?,
    val extensionName: String? = null,
    val extensionLoadStatus: ExtensionDimensionStatus = ExtensionDimensionStatus.UNKNOWN,
    val extensionDetail: String? = null,
    val repoHost: String? = null,
)
