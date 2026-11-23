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
    val consecutiveFailures: Int = 0,
    val lastCategory: SourceHealthCategory = SourceHealthCategory.UNKNOWN,
    val lastStatusCode: Int? = null,
    val lastProbeType: HealthProbeType = HealthProbeType.UNKNOWN,
    val lastProbePhase: DiagnosticPhase = DiagnosticPhase.IDLE,
    val lastChecked: Long = maxOf(lastSuccess, lastFailure),
    val lastCorrelationId: String? = null,
    val lastProbeTarget: String? = null,
) : Serializable {

    companion object {
        const val DEFAULT_STALE_THRESHOLD_MS = 7L * 24L * 60L * 60L * 1000L
        const val SUSPECTED_DEAD_MIN_FAILURES = 3
    }

    val effectiveCategory: SourceHealthCategory
        get() = when {
            lastCategory != SourceHealthCategory.UNKNOWN -> lastCategory
            lastSuccess > 0L && !isFailingNow -> SourceHealthCategory.HEALTHY
            else -> SourceHealthClassifier.inferLegacyCategory(lastError)
        }

    val effectiveConsecutiveFailures: Int
        get() = when {
            !isFailingNow -> 0
            consecutiveFailures > 0 -> consecutiveFailures
            effectiveCategory == SourceHealthCategory.OFFLINE -> 0
            else -> 1
        }

    val effectiveLastChecked: Long
        get() = if (lastChecked > 0L) lastChecked else maxOf(lastSuccess, lastFailure)

    val hasCheck: Boolean
        get() = effectiveLastChecked > 0L

    fun isStale(
        nowMillis: Long = System.currentTimeMillis(),
        thresholdMillis: Long = DEFAULT_STALE_THRESHOLD_MS,
    ): Boolean {
        return hasCheck && (nowMillis - effectiveLastChecked) > thresholdMillis
    }

    fun effectiveStatus(
        nowMillis: Long = System.currentTimeMillis(),
        staleThresholdMillis: Long = DEFAULT_STALE_THRESHOLD_MS,
    ): SourceHealthStatus {
        if (!hasCheck) return SourceHealthStatus.UNKNOWN
        val category = effectiveCategory
        if (category == SourceHealthCategory.OFFLINE) return SourceHealthStatus.UNKNOWN
        if (isStale(nowMillis, staleThresholdMillis)) return SourceHealthStatus.STALE

        if (!isFailingNow) {
            return if (performanceScore >= 80) SourceHealthStatus.HEALTHY else SourceHealthStatus.DEGRADED
        }

        val failures = effectiveConsecutiveFailures
        if (failures >= SUSPECTED_DEAD_MIN_FAILURES &&
            (category == SourceHealthCategory.DNS_FAILURE || category == SourceHealthCategory.HTTP_NOT_FOUND)
        ) {
            return SourceHealthStatus.SUSPECTED_DEAD
        }

        return when (category) {
            SourceHealthCategory.DNS_FAILURE,
            SourceHealthCategory.CONNECT_TIMEOUT,
            SourceHealthCategory.READ_TIMEOUT,
            SourceHealthCategory.TLS_ERROR,
            SourceHealthCategory.HTTP_SERVER_ERROR,
            SourceHealthCategory.HTTP_NOT_FOUND,
            SourceHealthCategory.HTTP_REDIRECT_BLOCKED,
            -> SourceHealthStatus.UNREACHABLE

            SourceHealthCategory.HTTP_RATE_LIMITED,
            SourceHealthCategory.CLOUDFLARE_BLOCKED,
            SourceHealthCategory.HTTP_FORBIDDEN,
            SourceHealthCategory.HTTP_UNAUTHORIZED,
            SourceHealthCategory.REPO_INDEX_INVALID,
            SourceHealthCategory.EXTENSION_UNAVAILABLE,
            SourceHealthCategory.PARSER_ERROR,
            -> SourceHealthStatus.DEGRADED

            SourceHealthCategory.HEALTHY,
            SourceHealthCategory.UNKNOWN,
            SourceHealthCategory.OFFLINE,
            -> SourceHealthStatus.UNKNOWN
        }
    }

    val isLikelyDead: Boolean
        get() = effectiveStatus() == SourceHealthStatus.SUSPECTED_DEAD

    val isFailingNow: Boolean
        get() = lastFailure > 0 && lastFailure >= lastSuccess

    val isDnsDead: Boolean
        get() = isFailingNow && effectiveCategory == SourceHealthCategory.DNS_FAILURE

    val isServerDown: Boolean
        get() = isFailingNow && effectiveCategory == SourceHealthCategory.HTTP_SERVER_ERROR

    val isCloudflareBlocked: Boolean
        get() = isFailingNow && (
            effectiveCategory == SourceHealthCategory.CLOUDFLARE_BLOCKED ||
                effectiveCategory == SourceHealthCategory.HTTP_FORBIDDEN
            )

    val isTimeout: Boolean
        get() = isFailingNow && (
            effectiveCategory == SourceHealthCategory.CONNECT_TIMEOUT ||
                effectiveCategory == SourceHealthCategory.READ_TIMEOUT
            )

    val healthScore: Int
        get() {
            val total = successCount + failureCount
            if (total == 0) return 100
            if (effectiveCategory == SourceHealthCategory.OFFLINE && !isFailingNow) {
                return (successCount.toDouble() / total * 100).toInt()
            }
            if (isDnsDead) return 0
            if (isServerDown) return 10
            if (isCloudflareBlocked) return 20
            if (isTimeout) return 25
            val base = (successCount.toDouble() / total * 100).toInt()
            // If the last test failed, heavily penalize so failing sources are never labeled STABLE
            return if (isFailingNow) (base / 2).coerceAtMost(35) else base
        }

    val speedScore: Int
        get() = when {
            successCount == 0 && failureCount == 0 -> 100
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
            isCloudflareBlocked || isServerDown || isDnsDead -> 1
            isFailingNow -> 2
            else -> 10
        }

    val recommendedDelay: Long
        get() = when {
            isCloudflareBlocked -> 300L
            isFailingNow -> 150L
            else -> 0L
        }
}
