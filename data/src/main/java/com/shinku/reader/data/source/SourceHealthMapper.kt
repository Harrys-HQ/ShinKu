package com.shinku.reader.data.source

import com.shinku.reader.domain.source.model.SourceHealth

fun mapSourceHealth(
    source_id: Long,
    last_success: Long?,
    last_failure: Long?,
    failure_count: Long,
    success_count: Long,
    avg_latency: Long,
    last_error: String?,
): SourceHealth {
    return SourceHealth(
        sourceId = source_id,
        lastSuccess = last_success ?: 0L,
        lastFailure = last_failure ?: 0L,
        failureCount = failure_count.toInt(),
        successCount = success_count.toInt(),
        avgLatency = avg_latency,
        lastError = last_error,
    )
}
