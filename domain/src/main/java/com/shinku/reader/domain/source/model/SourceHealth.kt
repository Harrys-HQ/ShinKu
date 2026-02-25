package com.shinku.reader.domain.source.model

import java.io.Serializable

data class SourceHealth(
    val sourceId: Long,
    val lastSuccess: Long,
    val lastFailure: Long,
    val failureCount: Int,
    val successCount: Int,
    val avgLatency: Long,
    val lastError: String?,
) : Serializable {

    val healthScore: Int
        get() {
            val total = successCount + failureCount
            if (total == 0) return 100
            return (successCount.toDouble() / total * 100).toInt()
        }

    val speedScore: Int
        get() = when {
            avgLatency == 0L -> 100
            avgLatency < 500 -> 100
            avgLatency < 1500 -> 80
            avgLatency < 3000 -> 50
            else -> 20
        }

    val performanceScore: Int
        get() = ((healthScore * 0.7) + (speedScore * 0.3)).toInt()

    val isSensitive: Boolean
        get() = performanceScore < 80 || failureCount > 5

    val recommendedConcurrency: Int
        get() = when {
            performanceScore > 90 -> 10 // High speed
            performanceScore > 70 -> 3  // Throttled
            else -> 1             // Safe mode
        }

    val recommendedDelay: Long
        get() = when {
            performanceScore > 90 -> 50
            performanceScore > 70 -> 500
            else -> 1500
        }
}
