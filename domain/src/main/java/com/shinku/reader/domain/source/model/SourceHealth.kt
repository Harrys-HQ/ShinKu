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

    val isFailingNow: Boolean
        get() = lastFailure > 0 && lastFailure >= lastSuccess

    val isDnsDead: Boolean
        get() = isFailingNow && lastError?.contains("unable to resolve host", ignoreCase = true) == true

    val isServerDown: Boolean
        get() = isFailingNow && (lastError?.contains("522") == true || lastError?.contains("524") == true || lastError?.contains("502") == true || lastError?.contains("504") == true)

    val isCloudflareBlocked: Boolean
        get() = isFailingNow && (lastError?.contains("403") == true || lastError?.contains("cloudflare", ignoreCase = true) == true)

    val isTimeout: Boolean
        get() = isFailingNow && lastError?.contains("timeout", ignoreCase = true) == true

    val healthScore: Int
        get() {
            val total = successCount + failureCount
            if (total == 0) return 0
            if (isDnsDead) return 0
            if (isServerDown) return 10
            if (isCloudflareBlocked) return 20
            if (isTimeout) return 25
            val base = (successCount.toDouble() / total * 100).toInt()
            // If the last test failed, heavily penalize so dead/broken sources are never labeled STABLE
            return if (isFailingNow) (base / 2).coerceAtMost(35) else base
        }

    val speedScore: Int
        get() = when {
            successCount == 0 || isFailingNow -> 0
            avgLatency == 0L -> 100
            avgLatency < 500 -> 100
            avgLatency < 1500 -> 80
            avgLatency < 3000 -> 50
            else -> 20
        }

    val performanceScore: Int
        get() {
            if (isDnsDead) return 0
            if (isServerDown) return 10
            if (isFailingNow && successCount == 0) return 0
            return ((healthScore * 0.7) + (speedScore * 0.3)).toInt()
        }

    val isSensitive: Boolean
        get() = !isDnsDead && !isServerDown && (performanceScore < 80 || failureCount > 3)

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
