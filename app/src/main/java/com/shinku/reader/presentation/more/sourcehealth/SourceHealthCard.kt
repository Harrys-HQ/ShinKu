package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthClassifier
import com.shinku.reader.domain.source.model.SourceHealthStatus
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.Pill
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SourceHealthItemCard(
    item: SourceHealthItem,
    isTesting: Boolean = false,
    onTestSingleSource: ((Long) -> Unit)? = null,
    onCancelSingleSource: ((Long) -> Unit)? = null,
) {
    val health = item.health
    val hasCheck = health?.hasCheck == true
    val status = health?.effectiveStatus() ?: SourceHealthStatus.UNKNOWN
    val category = health?.effectiveCategory ?: SourceHealthCategory.UNKNOWN
    val performanceScore = if (hasCheck) health.performanceScore else -1
    val (statusLabel, statusColor) = statusPresentation(status, health)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
        shadowElevation = SanctuaryElevation.subtle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(
                    text = item.source.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                HealthRatingStars(performanceScore)

                if (onTestSingleSource != null) {
                    IconButton(
                        onClick = {
                            if (isTesting) {
                                onCancelSingleSource?.invoke(item.source.id)
                            } else {
                                onTestSingleSource(item.source.id)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (isTesting) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(MR.strings.action_cancel),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(MR.strings.diagnostics_test_source),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            if (isTesting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Text(
                        text = stringResource(MR.strings.diagnostics_testing_source),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SanctuaryOverlineBadge(
                    text = statusLabel,
                    compact = true,
                    color = statusColor,
                )

                ExtensionDimensionPill(item.extensionLoadStatus)

                if (hasCheck && (health.isFailingNow || category == SourceHealthCategory.OFFLINE)) {
                    val isErrorTint = status == SourceHealthStatus.SUSPECTED_DEAD || status == SourceHealthStatus.UNREACHABLE
                    Pill(
                        text = categoryLabel(category, health.lastStatusCode),
                        color = if (isErrorTint) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isErrorTint) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else if (health?.isSensitive == true) {
                    Pill(
                        text = "Safe concurrency (3x)",
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                if (health != null && health.lastProbeType != HealthProbeType.UNKNOWN) {
                    Pill(
                        text = probeTypeLabel(health.lastProbeType),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                item.repoHost?.takeIf { it.isNotBlank() }?.let { host ->
                    Pill(
                        text = "Repo: $host",
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text(
                text = buildLastCheckLine(health, hasCheck),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (health != null && hasCheck) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                            "No reply"
                        } else {
                            "--"
                        },
                    )
                    Text(
                        text = "S: ${health.successCount} / F: ${health.failureCount} · Streak: ${health.effectiveConsecutiveFailures}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val lastError = health.lastError
                if (!lastError.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (!health.isFailingNow && health.lastSuccess >= health.lastFailure) {
                                stringResource(MR.strings.source_health_previous_failure, lastError)
                            } else {
                                lastError
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (health.isFailingNow || category == SourceHealthCategory.OFFLINE) {
                    Text(
                        text = SourceHealthClassifier.plainLanguageNextStep(
                            category = category,
                            consecutiveFailures = health.effectiveConsecutiveFailures,
                            statusCode = health.lastStatusCode,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (!item.extensionDetail.isNullOrBlank()) {
                Text(
                    text = item.extensionDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
