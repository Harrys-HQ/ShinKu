package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.active
import com.shinku.reader.presentation.core.theme.statusDegraded
import com.shinku.reader.presentation.core.theme.statusExcellent
import com.shinku.reader.presentation.core.theme.statusGood
import com.shinku.reader.presentation.core.theme.statusWarning

@Composable
fun SourceHealthScreenContent(
    healthList: List<SourceHealthItem>,
    paddingValues: PaddingValues,
    testingSourceIds: Set<Long> = emptySet(),
    onTestSingleSource: ((Long) -> Unit)? = null,
) {
    if (healthList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(MaterialTheme.padding.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No sources found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.medium,
            top = paddingValues.calculateTopPadding() + MaterialTheme.padding.small,
            bottom = paddingValues.calculateBottomPadding() + MaterialTheme.padding.medium,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(healthList, key = { it.source.id }) { item ->
            SourceHealthItemCard(
                item = item,
                isTesting = item.source.id in testingSourceIds,
                onTestSingleSource = onTestSingleSource,
            )
        }
    }
}

@Composable
private fun SourceHealthItemCard(
    item: SourceHealthItem,
    isTesting: Boolean = false,
    onTestSingleSource: ((Long) -> Unit)? = null,
) {
    val health = item.health
    val performanceScore = health?.performanceScore ?: -1

    val (statusLabel, statusColor) = when {
        health == null -> "UNTESTED" to MaterialTheme.colorScheme.outline
        health.isDnsDead -> "DEAD DOMAIN" to MaterialTheme.colorScheme.error
        health.isServerDown -> "SERVER DOWN" to MaterialTheme.colorScheme.error
        health.isCloudflareBlocked -> "BLOCKED (403)" to MaterialTheme.colorScheme.statusWarning
        health.isTimeout -> "TIMEOUT" to MaterialTheme.colorScheme.statusWarning
        health.isFailingNow -> "OFFLINE" to MaterialTheme.colorScheme.error
        performanceScore >= 95 -> "EXCELLENT" to MaterialTheme.colorScheme.statusExcellent
        performanceScore >= 80 -> "STABLE" to MaterialTheme.colorScheme.statusGood
        performanceScore >= 60 -> "UNSTABLE" to MaterialTheme.colorScheme.statusWarning
        else -> "DEGRADED" to MaterialTheme.colorScheme.statusDegraded
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.card,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
        shadowElevation = SanctuaryElevation.subtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(
                    text = item.source.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                HealthRatingStars(performanceScore)

                if (onTestSingleSource != null) {
                    IconButton(
                        onClick = { onTestSingleSource(item.source.id) },
                        enabled = !isTesting,
                        modifier = Modifier.size(40.dp),
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Test Source",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                SanctuaryOverlineBadge(
                    text = statusLabel,
                    compact = true,
                    color = statusColor,
                )

                if (health?.isFailingNow == true) {
                    val failureBadge = when {
                        health.isDnsDead -> "DNS FAILED"
                        health.isServerDown -> "HTTP 522"
                        health.isCloudflareBlocked -> "CLOUDFLARE 403"
                        health.isTimeout -> "TIMED OUT"
                        else -> "OFFLINE"
                    }
                    Pill(
                        text = failureBadge,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else if (health?.isSensitive == true) {
                    Pill(
                        text = "SAFE CONCURRENCY (3x)",
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            if (health != null) {
                Row(
                    modifier = Modifier.padding(top = MaterialTheme.padding.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                ) {
                    HealthStat(
                        icon = Icons.Default.HealthAndSafety,
                        label = "${health.healthScore}%",
                        color = statusColor,
                    )
                    HealthStat(
                        icon = Icons.Default.Speed,
                        label = if (health.avgLatency > 0L) {
                            "${health.avgLatency}ms"
                        } else if (health.isFailingNow) {
                            "Failed"
                        } else {
                            "--"
                        },
                    )
                    Text(
                        text = "S: ${health.successCount} / F: ${health.failureCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val lastError = health.lastError
                if (lastError != null) {
                    val friendlyError = formatFriendlyError(lastError)
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = friendlyError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun formatFriendlyError(error: String): String {
    return when {
        error.contains("unable to resolve host", ignoreCase = true) -> {
            val host = error.substringAfter("unable to resolve host ").substringBefore(":")
                .replace("\"", "").trim()
            "Host unreachable ($host) — Domain is offline, changed, or blocked by DNS"
        }
        error.contains("522") -> "Cloudflare Origin Timeout (522) — Manga site host server is offline"
        error.contains("524") -> "Cloudflare Gateway Timeout (524) — Site server took too long to reply"
        error.contains("502") || error.contains("504") -> "Bad Gateway / Host Unreachable — Server down"
        error.contains("403") -> "Cloudflare Bot Protection (403) — Anti-bot challenge or IP blocked"
        error.contains("429") -> "Rate Limited (429) — Too many requests sent"
        error.contains("timeout", ignoreCase = true) -> "Connection Timed Out — No response within 15 seconds"
        else -> error
    }
}

@Composable
private fun HealthRatingStars(score: Int) {
    if (score == -1) return

    val rating = (score / 20f) // 0-5 scale
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
private fun HealthStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
