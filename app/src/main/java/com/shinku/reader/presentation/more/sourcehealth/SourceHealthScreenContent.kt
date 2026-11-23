package com.shinku.reader.presentation.more.sourcehealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.data.diagnostics.LiveHealthScanState
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.diagnostics.DiagnosticCheckStatus
import com.shinku.reader.domain.diagnostics.RepositoryHealthCheck
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.SanctuaryProgressBar
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.statusExcellent
import com.shinku.reader.presentation.core.theme.statusWarning

@Composable
fun SourceHealthScreenContent(
    healthList: List<SourceHealthItem>,
    paddingValues: PaddingValues,
    testingSourceIds: Set<Long> = emptySet(),
    liveScan: LiveHealthScanState = LiveHealthScanState(),
    repoChecks: List<RepositoryHealthCheck> = emptyList(),
    onTestSingleSource: ((Long) -> Unit)? = null,
    onCancelSingleSource: ((Long) -> Unit)? = null,
    onCancelScan: (() -> Unit)? = null,
    onRetryScan: (() -> Unit)? = null,
) {
    if (healthList.isEmpty() && !liveScan.isRunning && repoChecks.isEmpty()) {
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
        if (liveScan.isRunning || liveScan.isCancelled || liveScan.finishedAt != null) {
            item(key = "live-scan-banner") {
                LiveScanProgressCard(
                    liveScan = liveScan,
                    onCancelScan = onCancelScan,
                    onRetryScan = onRetryScan,
                )
            }
        }

        if (repoChecks.isNotEmpty()) {
            item(key = "repo-health-summary") {
                RepositoryHealthSummaryCard(repoChecks = repoChecks)
            }
        }

        item(key = "source-health-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Source health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = SanctuaryShapes.tag,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "Checked on request",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(healthList, key = { it.source.id }) { item ->
            SourceHealthItemCard(
                item = item,
                isTesting = item.source.id in testingSourceIds,
                onTestSingleSource = onTestSingleSource,
                onCancelSingleSource = onCancelSingleSource,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveScanProgressCard(
    liveScan: LiveHealthScanState,
    onCancelScan: (() -> Unit)?,
    onRetryScan: (() -> Unit)?,
) {
    val progress = if (liveScan.totalCount > 0) {
        liveScan.completedCount.toFloat() / liveScan.totalCount.toFloat()
    } else {
        0f
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.compact),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SanctuaryOverlineBadge(
                    text = when {
                        liveScan.isRunning -> "LOCAL SCAN IN PROGRESS"
                        liveScan.isCancelled -> "SCAN CANCELLED"
                        liveScan.currentPhase == DiagnosticPhase.FAILED -> "SCAN FAILED"
                        else -> "SCAN COMPLETE"
                    },
                    compact = true,
                    color = when {
                        liveScan.isRunning -> MaterialTheme.colorScheme.primary
                        liveScan.isCancelled -> MaterialTheme.colorScheme.statusWarning
                        liveScan.currentPhase == DiagnosticPhase.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.statusExcellent
                    },
                )
                liveScan.correlationId?.let { id ->
                    Text(
                        text = id,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (liveScan.totalCount > 0) {
                SanctuaryProgressBar(progress = progress)
            }

            val detailText = when {
                liveScan.isRunning -> buildString {
                    append("${liveScan.completedCount} / ${liveScan.totalCount}")
                    append(" · ${liveScan.currentPhase.name}")
                    liveScan.currentTargetName?.let { append(" · $it") }
                }
                liveScan.isCancelled -> "Stopped at ${liveScan.completedCount} / ${liveScan.totalCount}."
                liveScan.currentPhase == DiagnosticPhase.FAILED -> "Stopped after checking ${liveScan.completedCount} sources."
                else -> "Checked ${liveScan.completedCount} sources."
            }
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (liveScan.isRunning && onCancelScan != null) {
                    OutlinedButton(onClick = onCancelScan) {
                        Text(stringResource(MR.strings.action_cancel), maxLines = 1, softWrap = false)
                    }
                } else if (!liveScan.isRunning && onRetryScan != null) {
                    OutlinedButton(onClick = onRetryScan) {
                        Text(stringResource(MR.strings.action_retry), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryHealthSummaryCard(repoChecks: List<RepositoryHealthCheck>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.deckCard,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SanctuaryOverlineBadge(text = "EXTENSION REPOSITORIES", compact = true)
            repoChecks.forEach { repo ->
                val color = when (repo.status) {
                    DiagnosticCheckStatus.OK -> MaterialTheme.colorScheme.statusExcellent
                    DiagnosticCheckStatus.WARNING -> MaterialTheme.colorScheme.statusWarning
                    DiagnosticCheckStatus.FAILED -> MaterialTheme.colorScheme.error
                    DiagnosticCheckStatus.NOT_CHECKED -> MaterialTheme.colorScheme.outline
                }
                Text(
                    text = "${repo.repoName} (${repo.repoHost}): ${repo.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
        }
    }
}
