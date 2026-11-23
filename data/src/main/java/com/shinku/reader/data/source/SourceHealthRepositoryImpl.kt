package com.shinku.reader.data.source

import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthClassifier
import com.shinku.reader.domain.source.model.SourceProbeOutcome
import com.shinku.reader.domain.source.repository.SourceHealthRepository
import kotlinx.coroutines.flow.Flow

class SourceHealthRepositoryImpl(
    private val handler: DatabaseHandler,
) : SourceHealthRepository {

    override fun subscribeAll(): Flow<List<SourceHealth>> {
        return handler.subscribeToList { source_healthQueries.findAll(::mapSourceHealth) }
    }

    override suspend fun getBySourceId(sourceId: Long): SourceHealth? {
        return handler.awaitOneOrNull { source_healthQueries.findOne(sourceId, ::mapSourceHealth) }
    }

    override suspend fun updateStats(sourceId: Long, isSuccess: Boolean, latency: Long, error: String?) {
        val category = if (isSuccess) {
            SourceHealthCategory.HEALTHY
        } else {
            SourceHealthClassifier.inferLegacyCategory(error)
        }
        val redactedError = error?.let(DiagnosticRedactor::redact)
        handler.await {
            source_healthQueries.updateStats(
                id = sourceId,
                is_success = if (isSuccess) 1L else 0L,
                timestamp = System.currentTimeMillis(),
                latency = latency,
                error = redactedError,
                category = category.name,
            )
        }
    }

    override suspend fun updateOutcome(sourceId: Long, outcome: SourceProbeOutcome) {
        val redactedError = if (outcome.isSuccess) null else outcome.sanitizedSummary?.let(DiagnosticRedactor::redact)
        val now = System.currentTimeMillis()
        val successDelta = if (outcome.isSuccess) 1L else 0L
        val failureDelta = if (!outcome.isSuccess && outcome.countAsFailure) 1L else 0L
        val lastSuccess = if (outcome.isSuccess) now else 0L
        val lastFailure = if (!outcome.isSuccess && outcome.countAsFailure) now else 0L
        val latency = if (outcome.isSuccess) outcome.latencyMs else 0L
        handler.await {
            source_healthQueries.updateOutcome(
                id = sourceId,
                last_success = lastSuccess,
                last_failure = lastFailure,
                success_delta = successDelta,
                failure_delta = failureDelta,
                latency = latency,
                error = redactedError,
                category = outcome.category.name,
                status_code = outcome.statusCode?.toLong(),
                probe_type = outcome.probeType.name,
                probe_phase = outcome.phase.name,
                timestamp = now,
                correlation_id = outcome.correlationId,
                probe_target = outcome.sanitizedTarget,
            )
        }
    }

    override suspend fun resetStats(sourceId: Long) {
        handler.await {
            source_healthQueries.resetStats(sourceId)
        }
    }

    override suspend fun resetAllStats() {
        handler.await {
            source_healthQueries.resetAllStats()
        }
    }
}
