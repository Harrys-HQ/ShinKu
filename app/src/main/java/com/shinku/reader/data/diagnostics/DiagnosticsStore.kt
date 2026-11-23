package com.shinku.reader.data.diagnostics

import com.shinku.reader.domain.diagnostics.DiagnosticRunEvent
import com.shinku.reader.domain.diagnostics.RepositoryHealthCheck
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.SourceProbeOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class LiveHealthScanState(
    val correlationId: String? = null,
    val isRunning: Boolean = false,
    val isCancelled: Boolean = false,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val currentTargetName: String? = null,
    val currentPhase: DiagnosticPhase = DiagnosticPhase.IDLE,
    val perSourceStatus: Map<Long, SourceProbeOutcome> = emptyMap(),
    val workId: UUID? = null,
)

/**
 * Local-only in-memory store for live scan progress, repository probe results, and recent diagnostic events.
 * Zero data leaves the device automatically from this store.
 */
class DiagnosticsStore {

    private val _liveScanState = MutableStateFlow(LiveHealthScanState())
    val liveScanState: StateFlow<LiveHealthScanState> = _liveScanState.asStateFlow()

    private val _repoHealthChecks = MutableStateFlow<Map<String, RepositoryHealthCheck>>(emptyMap())
    val repoHealthChecks: StateFlow<Map<String, RepositoryHealthCheck>> = _repoHealthChecks.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<DiagnosticRunEvent>>(emptyList())
    val recentEvents: StateFlow<List<DiagnosticRunEvent>> = _recentEvents.asStateFlow()

    fun newCorrelationId(): String = "diag-${UUID.randomUUID().toString().take(8)}"

    @Synchronized
    fun startScan(correlationId: String, totalCount: Int, workId: UUID? = null): Boolean {
        if (_liveScanState.value.isRunning) return false
        _liveScanState.value = LiveHealthScanState(
            correlationId = correlationId,
            isRunning = true,
            startedAt = System.currentTimeMillis(),
            totalCount = totalCount,
            currentPhase = DiagnosticPhase.REPOSITORY_FETCH,
            workId = workId,
        )
        return true
    }

    @Synchronized
    fun updateScanTotal(correlationId: String, totalCount: Int) {
        val state = _liveScanState.value
        if (!state.isRunning || state.correlationId != correlationId) return
        _liveScanState.value = state.copy(totalCount = totalCount.coerceAtLeast(state.completedCount))
    }

    @Synchronized
    fun updateScanTarget(correlationId: String, sourceName: String) {
        val state = _liveScanState.value
        if (!state.isRunning || state.correlationId != correlationId) return
        _liveScanState.value = state.copy(currentPhase = DiagnosticPhase.SOURCE_REQUEST, currentTargetName = sourceName)
    }

    @Synchronized
    fun recordScanProbe(sourceId: Long, sourceName: String, outcome: SourceProbeOutcome) {
        val state = _liveScanState.value
        if (!state.isRunning || state.correlationId != outcome.correlationId) return
        val updatedMap = state.perSourceStatus + (sourceId to outcome)
        _liveScanState.value = state.copy(completedCount = updatedMap.size, perSourceStatus = updatedMap)
        recordSourceProbe(sourceName, outcome)
    }

    fun recordSourceProbe(sourceName: String, outcome: SourceProbeOutcome) {
        recordEvent(
            DiagnosticRunEvent(
                correlationId = outcome.correlationId ?: newCorrelationId(),
                timestamp = System.currentTimeMillis(),
                phase = outcome.phase,
                targetName = sourceName,
                category = outcome.category,
                statusCode = outcome.statusCode,
                latencyMs = outcome.latencyMs,
                summary = if (outcome.isSuccess) "OK" else outcome.category.name,
            ),
        )
    }

    @Synchronized
    fun finishScan(correlationId: String, phase: DiagnosticPhase) {
        val state = _liveScanState.value
        if (!state.isRunning || state.correlationId != correlationId) return
        _liveScanState.value = state.copy(
            isRunning = false,
            isCancelled = phase == DiagnosticPhase.CANCELLED,
            finishedAt = System.currentTimeMillis(),
            currentTargetName = null,
            currentPhase = phase,
            workId = null,
        )
    }

    fun recordRepoCheck(repoBaseUrl: String, check: RepositoryHealthCheck) {
        _repoHealthChecks.update { current ->
            current + (repoBaseUrl to check)
        }
        recordEvent(
            DiagnosticRunEvent(
                correlationId = check.correlationId ?: newCorrelationId(),
                timestamp = check.checkedAt,
                phase = check.phase,
                targetName = "${check.repoName} (${check.repoHost})",
                category = check.category,
                statusCode = check.statusCode,
                latencyMs = check.latencyMs,
                summary = check.summary,
            ),
        )
    }

    fun recordEvent(event: DiagnosticRunEvent) {
        _recentEvents.update { current ->
            (listOf(event) + current).take(MAX_RECENT_EVENTS)
        }
    }

    @Synchronized
    fun clear() {
        if (!_liveScanState.value.isRunning) _liveScanState.value = LiveHealthScanState()
        _repoHealthChecks.value = emptyMap()
        _recentEvents.value = emptyList()
    }

    private companion object {
        const val MAX_RECENT_EVENTS = 200
    }
}
