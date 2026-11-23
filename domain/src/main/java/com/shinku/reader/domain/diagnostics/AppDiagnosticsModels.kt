package com.shinku.reader.domain.diagnostics

import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.SourceHealthCategory
import java.time.Instant

enum class DiagnosticCheckStatus {
    OK,
    WARNING,
    FAILED,
    NOT_CHECKED,
}

enum class DiagnosticSectionId(
    val title: String,
    val defaultSelectedForExport: Boolean,
) {
    APP_ENVIRONMENT("App & device environment", true),
    WEBVIEW_RUNTIME("WebView runtime", true),
    NETWORK_CONNECTIVITY("Network connectivity", true),
    WORK_MANAGER("Background workers", true),
    EXTENSION_REPOS("Extension repositories", true),
    EXTENSION_LOADING("Extension install & load", true),
    SOURCE_HEALTH("Source health summary", true),
    RECENT_ERRORS("Recent diagnostic events (opt-in)", false),
}

data class DiagnosticCheckResult(
    val id: DiagnosticSectionId,
    val title: String,
    val status: DiagnosticCheckStatus,
    val timestamp: Long,
    val correlationId: String,
    val phase: DiagnosticPhase,
    val summary: String,
    val evidence: List<String> = emptyList(),
    val nextStep: String? = null,
)

data class RepositoryHealthCheck(
    val repoName: String,
    val repoHost: String,
    val status: DiagnosticCheckStatus,
    val category: SourceHealthCategory,
    val statusCode: Int? = null,
    val latencyMs: Long = 0L,
    val checkedAt: Long,
    val correlationId: String? = null,
    val phase: DiagnosticPhase = DiagnosticPhase.REPOSITORY_FETCH,
    val summary: String,
    val nextStep: String? = null,
)

enum class ExtensionLoadFailureReason {
    MISSING_VERSION,
    INCOMPATIBLE_LIB,
    UNSIGNED,
    UNTRUSTED,
    NSFW_DISABLED,
    CLASSLOADER_ERROR,
    INSTANTIATION_ERROR,
    MISSING_PACKAGE,
    UNKNOWN,
}

data class ExtensionLoadIssue(
    val pkgName: String,
    val name: String,
    val reason: ExtensionLoadFailureReason,
    val detail: String? = null,
    val detectedAt: Long = System.currentTimeMillis(),
)

data class DiagnosticRunEvent(
    val correlationId: String,
    val timestamp: Long,
    val phase: DiagnosticPhase,
    val targetName: String,
    val category: SourceHealthCategory,
    val statusCode: Int? = null,
    val latencyMs: Long = 0L,
    val summary: String,
)

data class AppDiagnosticsReport(
    val correlationId: String,
    val generatedAt: Long,
    val checks: List<DiagnosticCheckResult>,
    val recentEvents: List<DiagnosticRunEvent> = emptyList(),
)

object DiagnosticReportBuilder {

    fun defaultSelectedSections(report: AppDiagnosticsReport): Set<DiagnosticSectionId> {
        return report.checks
            .map { it.id }
            .filter { it.defaultSelectedForExport }
            .toSet()
    }

    fun buildExportPayload(
        report: AppDiagnosticsReport,
        selectedSections: Set<DiagnosticSectionId>,
    ): String {
        val raw = buildString {
            appendLine("ShinKu Diagnostic Report")
            appendLine("Run ID: ${report.correlationId}")
            appendLine("Generated: ${Instant.ofEpochMilli(report.generatedAt)}")
            appendLine()

            val selectedChecks = report.checks.filter { it.id in selectedSections }
            if (selectedChecks.isEmpty()) {
                appendLine("(No diagnostic sections selected.)")
            } else {
                selectedChecks.forEach { check ->
                    appendLine("[${check.id.name}] ${check.title}")
                    appendLine("Status: ${check.status.name} | Phase: ${check.phase.name} | Checked: ${Instant.ofEpochMilli(check.timestamp)}")
                    appendLine("Summary: ${check.summary}")
                    check.evidence.forEach { item ->
                        appendLine("  - $item")
                    }
                    check.nextStep?.takeIf { it.isNotBlank() }?.let { step ->
                        appendLine("Next step: $step")
                    }
                    appendLine()
                }
            }

            if (DiagnosticSectionId.RECENT_ERRORS in selectedSections && report.recentEvents.isNotEmpty()) {
                appendLine("[RECENT_DIAGNOSTIC_EVENTS]")
                report.recentEvents.take(25).forEach { event ->
                    val statusPart = event.statusCode?.let { " HTTP $it" }.orEmpty()
                    val latencyPart = if (event.latencyMs > 0L) " (${event.latencyMs}ms)" else ""
                    appendLine(
                        "  - ${Instant.ofEpochMilli(event.timestamp)} [${event.correlationId}] ${event.phase.name} | ${event.targetName}: ${event.category.name}$statusPart$latencyPart — ${event.summary}",
                    )
                }
            }
        }
        return DiagnosticRedactor.redact(raw).trimEnd()
    }

    fun buildAiAdvisoryPrompt(
        report: AppDiagnosticsReport,
        selectedSections: Set<DiagnosticSectionId>,
    ): String {
        val redactedReport = buildExportPayload(report, selectedSections)
        return """
            You are a read-only troubleshooting assistant for the ShinKu Android manga reader.
            Analyze the user-approved redacted diagnostic report below.
            Rules:
            1. Cite only the section IDs, statuses, categories, and evidence explicitly present in the report.
            2. Clearly state uncertainty for any NOT_CHECKED, UNKNOWN, OFFLINE, STALE, or single-failure items.
            3. Never claim a source or extension is permanently dead from a single failed probe.
            4. Provide concise, practical next steps. Do not instruct changing or bypassing security controls, and note that this summary is advisory only.

            Diagnostic Report:
            $redactedReport
        """.trimIndent()
    }
}
