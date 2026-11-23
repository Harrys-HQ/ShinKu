package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealth
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthStatus
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.theme.active
import com.shinku.reader.presentation.core.theme.statusDegraded
import com.shinku.reader.presentation.core.theme.statusExcellent
import com.shinku.reader.presentation.core.theme.statusGood
import com.shinku.reader.presentation.core.theme.statusWarning
import com.shinku.reader.presentation.util.relativeTimeSpanString

@Composable
internal fun ExtensionDimensionPill(status: ExtensionDimensionStatus) {
    val (label, container, content) = when (status) {
        ExtensionDimensionStatus.LOADED -> Triple("Ext: Loaded", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        ExtensionDimensionStatus.UPDATE_AVAILABLE -> Triple("Ext: Update ready", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        ExtensionDimensionStatus.OBSOLETE -> Triple("Ext: Not in repos", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        ExtensionDimensionStatus.UNTRUSTED -> Triple("Ext: Untrusted", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        ExtensionDimensionStatus.LOAD_FAILED -> Triple("Ext: Load failed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        ExtensionDimensionStatus.NOT_INSTALLED -> Triple("Ext: Not installed", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        ExtensionDimensionStatus.UNKNOWN -> Triple("Ext: Unknown", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Pill(text = label, color = container, contentColor = content, style = MaterialTheme.typography.labelSmall)
}

@Composable
internal fun statusPresentation(status: SourceHealthStatus, health: SourceHealth?): Pair<String, Color> {
    return when (status) {
        SourceHealthStatus.UNKNOWN -> "NOT CHECKED" to MaterialTheme.colorScheme.outline
        SourceHealthStatus.HEALTHY -> if ((health?.performanceScore ?: 0) >= 95) {
            "EXCELLENT" to MaterialTheme.colorScheme.statusExcellent
        } else {
            "STABLE" to MaterialTheme.colorScheme.statusGood
        }
        SourceHealthStatus.STALE -> "STALE CHECK" to MaterialTheme.colorScheme.statusWarning
        SourceHealthStatus.DEGRADED -> "DEGRADED" to MaterialTheme.colorScheme.statusWarning
        SourceHealthStatus.UNREACHABLE -> "UNREACHABLE" to MaterialTheme.colorScheme.statusDegraded
        SourceHealthStatus.SUSPECTED_DEAD -> "LIKELY DEAD (3+)" to MaterialTheme.colorScheme.error
    }
}

@Composable
internal fun buildLastCheckLine(health: SourceHealth?, hasCheck: Boolean): String {
    if (health == null || !hasCheck) return "Not checked yet"
    val checkedAt = health.effectiveLastChecked
    val whenStr = if (checkedAt > 0L) relativeTimeSpanString(checkedAt) else "Unknown time"
    val staleSuffix = if (health.isStale()) " · Stale" else ""
    return when {
        health.effectiveCategory == SourceHealthCategory.OFFLINE -> "Device offline during probe ($whenStr)"
        health.isFailingNow -> "Last check failed ($whenStr)$staleSuffix"
        else -> {
            val latencyStr = if (health.avgLatency > 0L) " · ${health.avgLatency}ms" else ""
            "Last check succeeded ($whenStr)$latencyStr$staleSuffix"
        }
    }
}

internal fun categoryLabel(category: SourceHealthCategory, statusCode: Int?): String = when (category) {
    SourceHealthCategory.DNS_FAILURE -> "DNS failure"
    SourceHealthCategory.CONNECT_TIMEOUT -> "Connect timeout"
    SourceHealthCategory.READ_TIMEOUT -> "Read timeout"
    SourceHealthCategory.TLS_ERROR -> "TLS error"
    SourceHealthCategory.HTTP_UNAUTHORIZED -> "HTTP ${statusCode ?: 401} Auth"
    SourceHealthCategory.HTTP_FORBIDDEN -> "HTTP ${statusCode ?: 403} Forbidden"
    SourceHealthCategory.CLOUDFLARE_BLOCKED -> "Cloudflare ${statusCode ?: 403}"
    SourceHealthCategory.HTTP_NOT_FOUND -> "HTTP ${statusCode ?: 404} Not found"
    SourceHealthCategory.HTTP_RATE_LIMITED -> "HTTP 429 Rate limit"
    SourceHealthCategory.HTTP_SERVER_ERROR -> "HTTP ${statusCode ?: 500} Server"
    SourceHealthCategory.HTTP_REDIRECT_BLOCKED -> "HTTP ${statusCode ?: 302} Redirect"
    SourceHealthCategory.REPO_INDEX_INVALID -> "Repo index invalid"
    SourceHealthCategory.EXTENSION_UNAVAILABLE -> "Extension unavailable"
    SourceHealthCategory.PARSER_ERROR -> "Parser error"
    SourceHealthCategory.OFFLINE -> "Device offline"
    SourceHealthCategory.HEALTHY -> "Healthy"
    SourceHealthCategory.UNKNOWN -> "Insufficient evidence"
}

internal fun probeTypeLabel(type: HealthProbeType): String = when (type) {
    HealthProbeType.BASE_URL_GET -> "Base URL check"
    HealthProbeType.REPO_INDEX_FETCH -> "Repo index"
    HealthProbeType.EXTENSION_LOAD -> "Extension load"
    HealthProbeType.SOURCE_OPERATION -> "Library update"
    HealthProbeType.UNKNOWN -> "Unclassified check"
}

@Composable
internal fun HealthRatingStars(score: Int) {
    if (score < 0) return
    val rating = score / 20f
    Row {
        repeat(5) { index ->
            val starIcon = when {
                rating >= index + 1 -> Icons.Default.Star
                rating >= index + 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = starIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.active,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun HealthStat(
    icon: ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}
