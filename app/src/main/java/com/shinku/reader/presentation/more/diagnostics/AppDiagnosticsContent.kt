package com.shinku.reader.presentation.more.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinku.reader.domain.diagnostics.AppDiagnosticsReport
import com.shinku.reader.domain.diagnostics.DiagnosticCheckResult
import com.shinku.reader.domain.diagnostics.DiagnosticCheckStatus
import com.shinku.reader.domain.diagnostics.DiagnosticSectionId
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.components.SanctuaryOverlineBadge
import com.shinku.reader.presentation.core.components.material.padding
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes
import com.shinku.reader.presentation.core.theme.statusExcellent
import com.shinku.reader.presentation.core.theme.statusWarning
import com.shinku.reader.presentation.util.relativeTimeSpanString
import com.shinku.reader.ui.diagnostics.AppDiagnosticsUiState

@Composable
fun AppDiagnosticsContent(
    state: AppDiagnosticsUiState,
    paddingValues: PaddingValues,
    onRunLocalCheck: () -> Unit,
    onCheckRepos: () -> Unit,
    onCancelRepoCheck: () -> Unit,
    onToggleSection: (DiagnosticSectionId) -> Unit,
    onOpenExportPreview: () -> Unit,
    onOpenSourceHealth: () -> Unit,
    onRequestAiExplanation: () -> Unit,
) {
    val report = state.report
    if (report == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(MaterialTheme.padding.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(MR.strings.diagnostics_running_local),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = MaterialTheme.padding.medium,
            end = MaterialTheme.padding.medium,
            top = paddingValues.calculateTopPadding() + MaterialTheme.padding.small,
            bottom = paddingValues.calculateBottomPadding() + MaterialTheme.padding.large,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item(key = "overview-card") {
            DiagnosticsOverviewCard(
                report = report,
                state = state,
                onRunLocalCheck = onRunLocalCheck,
                onCheckRepos = onCheckRepos,
                onCancelRepoCheck = onCancelRepoCheck,
                onOpenExportPreview = onOpenExportPreview,
                onOpenSourceHealth = onOpenSourceHealth,
                onRequestAiExplanation = onRequestAiExplanation,
            )
        }

        if (state.isLoadingAiSummary || !state.aiSummary.isNullOrBlank() || !state.aiError.isNullOrBlank()) {
            item(key = "ai-advisory-card") {
                DiagnosticsAiResultCard(state = state)
            }
        }

        items(report.checks, key = { it.id.name }) { check ->
            DiagnosticCheckCard(
                check = check,
                includedInExport = check.id in state.selectedExportSections,
                onToggleInclude = { onToggleSection(check.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticsOverviewCard(
    report: AppDiagnosticsReport,
    state: AppDiagnosticsUiState,
    onRunLocalCheck: () -> Unit,
    onCheckRepos: () -> Unit,
    onCancelRepoCheck: () -> Unit,
    onOpenExportPreview: () -> Unit,
    onOpenSourceHealth: () -> Unit,
    onRequestAiExplanation: () -> Unit,
) {
    val okCount = report.checks.count { it.status == DiagnosticCheckStatus.OK }
    val warnCount = report.checks.count { it.status == DiagnosticCheckStatus.WARNING }
    val failCount = report.checks.count { it.status == DiagnosticCheckStatus.FAILED }
    val uncheckedCount = report.checks.count { it.status == DiagnosticCheckStatus.NOT_CHECKED }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SanctuaryShapes.hero,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.compact),
        border = SanctuaryBrushes.subtleBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SanctuaryOverlineBadge(text = "LOCAL DIAGNOSTICS · ZERO TELEMETRY", compact = true)
                Text(
                    text = report.correlationId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(MR.strings.diagnostics_overview_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "OK: $okCount · Warning: $warnCount · Failed: $failCount · Not checked: $uncheckedCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRunLocalCheck,
                    enabled = !state.isRunningLocalCheck,
                ) {
                    Text(stringResource(MR.strings.diagnostics_run_local_check), maxLines = 1, softWrap = false)
                }

                if (state.isCheckingRepos) {
                    OutlinedButton(onClick = onCancelRepoCheck) {
                        Text(stringResource(MR.strings.action_cancel), maxLines = 1, softWrap = false)
                    }
                } else {
                    OutlinedButton(onClick = onCheckRepos) {
                        Text(stringResource(MR.strings.diagnostics_check_repos), maxLines = 1, softWrap = false)
                    }
                }

                OutlinedButton(onClick = onOpenExportPreview) {
                    Text(stringResource(MR.strings.diagnostics_preview_export), maxLines = 1, softWrap = false)
                }

                TextButton(onClick = onOpenSourceHealth) {
                    Text(stringResource(MR.strings.diagnostics_open_source_health), maxLines = 1, softWrap = false)
                }

                if (state.isAiConfigured) {
                    TextButton(
                        onClick = onRequestAiExplanation,
                        enabled = !state.isLoadingAiSummary,
                    ) {
                        Text(stringResource(MR.strings.diagnostics_ai_explain), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsAiResultCard(state: AppDiagnosticsUiState) {
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
            SanctuaryOverlineBadge(text = "ADVISORY AI EXPLANATION", compact = true)
            when {
                state.isLoadingAiSummary -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Analyzing redacted diagnostic facts...",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                !state.aiError.isNullOrBlank() -> {
                    Text(
                        text = state.aiError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                !state.aiSummary.isNullOrBlank() -> {
                    Text(
                        text = state.aiSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCheckCard(
    check: DiagnosticCheckResult,
    includedInExport: Boolean,
    onToggleInclude: () -> Unit,
) {
    val (statusLabel, statusColor) = when (check.status) {
        DiagnosticCheckStatus.OK -> "OK" to MaterialTheme.colorScheme.statusExcellent
        DiagnosticCheckStatus.WARNING -> "WARNING" to MaterialTheme.colorScheme.statusWarning
        DiagnosticCheckStatus.FAILED -> "FAILED" to MaterialTheme.colorScheme.error
        DiagnosticCheckStatus.NOT_CHECKED -> "NOT CHECKED" to MaterialTheme.colorScheme.outline
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SanctuaryOverlineBadge(
                        text = "$statusLabel · ${relativeTimeSpanString(check.timestamp)}",
                        compact = true,
                        color = statusColor,
                    )
                    Text(
                        text = check.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Checkbox(
                    checked = includedInExport,
                    onCheckedChange = { onToggleInclude() },
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = check.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            check.evidence.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            check.nextStep?.takeIf { it.isNotBlank() }?.let { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
