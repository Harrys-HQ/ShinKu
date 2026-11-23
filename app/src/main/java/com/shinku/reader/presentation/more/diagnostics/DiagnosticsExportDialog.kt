package com.shinku.reader.presentation.more.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.shinku.reader.domain.diagnostics.AppDiagnosticsReport
import com.shinku.reader.domain.diagnostics.DiagnosticReportBuilder
import com.shinku.reader.domain.diagnostics.DiagnosticSectionId
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.theme.SanctuaryBrushes
import com.shinku.reader.presentation.core.theme.SanctuaryElevation
import com.shinku.reader.presentation.core.theme.SanctuaryShapes

@Composable
fun DiagnosticsExportDialog(
    report: AppDiagnosticsReport,
    selectedSections: Set<DiagnosticSectionId>,
    onToggleSection: (DiagnosticSectionId) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val payload = DiagnosticReportBuilder.buildExportPayload(report, selectedSections)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = SanctuaryShapes.hero,
        title = { Text(stringResource(MR.strings.diagnostics_export_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.diagnostics_export_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                report.checks.forEach { check ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSection(check.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = check.id in selectedSections,
                            onCheckedChange = { onToggleSection(check.id) },
                        )
                        Text(
                            text = check.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SanctuaryShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle),
                    border = SanctuaryBrushes.subtleBorder(),
                ) {
                    Text(
                        text = payload,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel), maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { onCopy(payload) },
                    enabled = selectedSections.isNotEmpty(),
                ) {
                    Text(stringResource(MR.strings.action_copy_to_clipboard), maxLines = 1, softWrap = false)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(payload) },
                enabled = selectedSections.isNotEmpty(),
            ) {
                Text(stringResource(MR.strings.diagnostics_share_report), maxLines = 1, softWrap = false)
            }
        },
    )
}
