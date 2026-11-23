package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.model.SourceProbeOutcome
import com.shinku.reader.domain.source.repository.SourceHealthRepository

class UpdateSourceHealth(
    private val repository: SourceHealthRepository,
) {
    suspend fun await(sourceId: Long, isSuccess: Boolean, latency: Long, error: String? = null) {
        repository.updateStats(sourceId, isSuccess, latency, error)
    }

    suspend fun await(sourceId: Long, outcome: SourceProbeOutcome) {
        repository.updateOutcome(sourceId, outcome)
    }

    suspend fun reset(sourceId: Long) {
        repository.resetStats(sourceId)
    }

    suspend fun resetAll() {
        repository.resetAllStats()
    }
}
