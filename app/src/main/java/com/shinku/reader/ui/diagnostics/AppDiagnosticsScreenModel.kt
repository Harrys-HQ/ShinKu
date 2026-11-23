package com.shinku.reader.ui.diagnostics

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.data.diagnostics.AppDiagnosticsCollector
import com.shinku.reader.data.diagnostics.DiagnosticsStore
import com.shinku.reader.domain.diagnostics.AppDiagnosticsReport
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.domain.diagnostics.DiagnosticReportBuilder
import com.shinku.reader.domain.diagnostics.DiagnosticSectionId
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.exh.source.ShinKuPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class PendingDiagnosticsAiRequest(
    val destination: String,
    val prompt: String,
    val send: suspend (String) -> String,
)

data class AppDiagnosticsUiState(
    val isRunningLocalCheck: Boolean = true,
    val isCheckingRepos: Boolean = false,
    val report: AppDiagnosticsReport? = null,
    val selectedExportSections: Set<DiagnosticSectionId> = emptySet(),
    val showExportDialog: Boolean = false,
    val pendingAiConsent: PendingDiagnosticsAiRequest? = null,
    val isLoadingAiSummary: Boolean = false,
    val aiSummary: String? = null,
    val aiError: String? = null,
    val isAiConfigured: Boolean = false,
)

class AppDiagnosticsScreenModel(
    private val collector: AppDiagnosticsCollector = Injekt.get(),
    private val diagnosticsStore: DiagnosticsStore = Injekt.get(),
    private val updateSourceHealth: UpdateSourceHealth = Injekt.get(),
    private val geminiVibeSearch: GeminiVibeSearch = Injekt.get(),
    private val shinkuPreferences: ShinKuPreferences = Injekt.get(),
) : StateScreenModel<AppDiagnosticsUiState>(AppDiagnosticsUiState()) {

    private var repoCheckJob: Job? = null
    private var aiSummaryJob: Job? = null

    init {
        runLocalCheck()
    }

    fun runLocalCheck() {
        screenModelScope.launchIO {
            mutableState.update { it.copy(isRunningLocalCheck = true) }
            val report = collector.collectLocalReport(diagnosticsStore.newCorrelationId())
            mutableState.update { current ->
                val initialSections = if (current.report == null) {
                    DiagnosticReportBuilder.defaultSelectedSections(report)
                } else {
                    current.selectedExportSections
                }
                current.copy(
                    isRunningLocalCheck = false,
                    report = report,
                    selectedExportSections = initialSections,
                    isAiConfigured = resolveAiProvider() != null,
                )
            }
        }
    }

    fun checkRepositories() {
        if (repoCheckJob?.isActive == true) return
        repoCheckJob = screenModelScope.launchIO {
            mutableState.update { it.copy(isCheckingRepos = true) }
            try {
                val correlationId = state.value.report?.correlationId ?: diagnosticsStore.newCorrelationId()
                collector.probeRepositories(correlationId)
                val updatedReport = collector.collectLocalReport(correlationId)
                mutableState.update { it.copy(report = updatedReport) }
            } catch (e: CancellationException) {
                throw e
            } finally {
                mutableState.update { it.copy(isCheckingRepos = false) }
            }
        }
    }

    fun cancelRepositoryCheck() {
        repoCheckJob?.cancel()
        repoCheckJob = null
        mutableState.update { it.copy(isCheckingRepos = false) }
    }

    fun toggleExportSection(sectionId: DiagnosticSectionId) {
        mutableState.update { current ->
            val updated = if (sectionId in current.selectedExportSections) {
                current.selectedExportSections - sectionId
            } else {
                current.selectedExportSections + sectionId
            }
            current.copy(selectedExportSections = updated)
        }
    }

    fun openExportPreview() {
        mutableState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportPreview() {
        mutableState.update { it.copy(showExportDialog = false) }
    }

    fun buildSelectedExportPayload(): String {
        val current = state.value
        val report = current.report ?: return ""
        return DiagnosticReportBuilder.buildExportPayload(report, current.selectedExportSections)
    }

    fun requestAiExplanation() {
        val current = state.value
        val report = current.report ?: return
        val provider = resolveAiProvider()
        if (provider == null) {
            mutableState.update {
                it.copy(aiError = "No AI provider configured. Diagnostics remain fully usable without AI.")
            }
            return
        }
        val prompt = DiagnosticReportBuilder.buildAiAdvisoryPrompt(report, current.selectedExportSections)
        mutableState.update {
            it.copy(
                aiError = null,
                pendingAiConsent = PendingDiagnosticsAiRequest(
                    destination = provider.destination,
                    prompt = prompt,
                    send = provider.send,
                ),
            )
        }
    }

    fun dismissAiConsent() {
        mutableState.update { it.copy(pendingAiConsent = null) }
    }

    fun confirmAiExplanation() {
        val pending = state.value.pendingAiConsent ?: return
        mutableState.update {
            it.copy(
                pendingAiConsent = null,
                isLoadingAiSummary = true,
                aiError = null,
            )
        }
        aiSummaryJob?.cancel()
        aiSummaryJob = screenModelScope.launchIO {
            try {
                val response = pending.send(pending.prompt)
                mutableState.update {
                    it.copy(
                        isLoadingAiSummary = false,
                        aiSummary = DiagnosticRedactor.redact(response.trim()),
                    )
                }
            } catch (e: CancellationException) {
                mutableState.update { it.copy(isLoadingAiSummary = false) }
                throw e
            } catch (t: Throwable) {
                val safeError = DiagnosticRedactor.redact(t.message ?: "AI summary request failed")
                mutableState.update {
                    it.copy(
                        isLoadingAiSummary = false,
                        aiError = safeError,
                    )
                }
            }
        }
    }

    fun clearDiagnosticsData(resetSourceHealth: Boolean) {
        screenModelScope.launchIO {
            repoCheckJob?.cancel()
            aiSummaryJob?.cancel()
            mutableState.update {
                it.copy(
                    pendingAiConsent = null,
                    isLoadingAiSummary = false,
                    aiSummary = null,
                    aiError = null,
                )
            }
            repoCheckJob?.join()
            aiSummaryJob?.join()
            diagnosticsStore.clear()
            if (resetSourceHealth) {
                updateSourceHealth.resetAll()
            }
            val refreshed = collector.collectLocalReport(diagnosticsStore.newCorrelationId())
            mutableState.update {
                it.copy(
                    report = refreshed,
                    selectedExportSections = DiagnosticReportBuilder.defaultSelectedSections(refreshed),
                    aiSummary = null,
                    aiError = null,
                )
            }
        }
    }

    private fun resolveAiProvider(): GeminiVibeSearch.ReaderTranslationProvider? {
        return geminiVibeSearch.readerTranslationProvider(
            apiKey = shinkuPreferences.geminiApiKey().get().trim(),
            model = shinkuPreferences.geminiModel().get(),
        )
    }
}
