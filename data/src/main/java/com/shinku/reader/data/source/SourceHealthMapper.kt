package com.shinku.reader.data.source

import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.model.SourceHealthCategory

fun mapSourceHealth(
    source_id: Long,
    last_success: Long?,
    last_failure: Long?,
    failure_count: Long,
    success_count: Long,
    avg_latency: Long,
    last_error: String?,
    consecutive_failures: Long,
    last_category: String,
    last_status_code: Long?,
    last_probe_type: String,
    last_probe_phase: String,
    last_checked: Long,
    last_correlation_id: String?,
    last_probe_target: String?,
): SourceHealth {
    return SourceHealth(
        sourceId = source_id,
        lastSuccess = last_success ?: 0L,
        lastFailure = last_failure ?: 0L,
        failureCount = failure_count.toInt(),
        successCount = success_count.toInt(),
        avgLatency = avg_latency,
        lastError = last_error,
        consecutiveFailures = consecutive_failures.toInt(),
        lastCategory = SourceHealthCategory.fromStorage(last_category),
        lastStatusCode = last_status_code?.toInt(),
        lastProbeType = HealthProbeType.fromStorage(last_probe_type),
        lastProbePhase = DiagnosticPhase.fromStorage(last_probe_phase),
        lastChecked = last_checked,
        lastCorrelationId = last_correlation_id,
        lastProbeTarget = last_probe_target,
    )
}
