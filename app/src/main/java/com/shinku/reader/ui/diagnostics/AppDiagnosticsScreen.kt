package com.shinku.reader.ui.diagnostics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.components.AppBar
import com.shinku.reader.presentation.components.AppBarActions
import com.shinku.reader.presentation.core.components.material.Scaffold
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.more.diagnostics.AppDiagnosticsContent
import com.shinku.reader.presentation.more.diagnostics.DiagnosticsAiConsentDialog
import com.shinku.reader.presentation.more.diagnostics.DiagnosticsExportDialog
import com.shinku.reader.presentation.util.Screen
import com.shinku.reader.ui.sourcehealth.SourceHealthScreen
import com.shinku.reader.util.CrashLogUtil
import com.shinku.reader.util.system.copyToClipboard
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

class AppDiagnosticsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { AppDiagnosticsScreenModel() }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.diagnostics_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.diagnostics_run_local_check),
                                    icon = Icons.Outlined.Refresh,
                                    onClick = screenModel::runLocalCheck,
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.diagnostics_clear_local_data),
                                    onClick = { screenModel.clearDiagnosticsData(resetSourceHealth = false) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.diagnostics_reset_all_health),
                                    onClick = { screenModel.clearDiagnosticsData(resetSourceHealth = true) },
                                ),
                            ),
                        )
                    },
                )
            },
        ) { paddingValues ->
            AppDiagnosticsContent(
                state = state,
                paddingValues = paddingValues,
                onRunLocalCheck = screenModel::runLocalCheck,
                onCheckRepos = screenModel::checkRepositories,
                onCancelRepoCheck = screenModel::cancelRepositoryCheck,
                onToggleSection = screenModel::toggleExportSection,
                onOpenExportPreview = screenModel::openExportPreview,
                onOpenSourceHealth = { navigator.push(SourceHealthScreen()) },
                onRequestAiExplanation = screenModel::requestAiExplanation,
            )
        }

        val currentReport = state.report
        if (state.showExportDialog && currentReport != null) {
            DiagnosticsExportDialog(
                report = currentReport,
                selectedSections = state.selectedExportSections,
                onToggleSection = screenModel::toggleExportSection,
                onCopy = { payload ->
                    context.copyToClipboard("ShinKu Diagnostics", payload)
                    screenModel.dismissExportPreview()
                },
                onShare = { payload ->
                    screenModel.dismissExportPreview()
                    scope.launch {
                        CrashLogUtil(context).shareDiagnosticReport(payload)
                    }
                },
                onDismissRequest = screenModel::dismissExportPreview,
            )
        }

        state.pendingAiConsent?.let { pending ->
            DiagnosticsAiConsentDialog(
                destination = pending.destination,
                prompt = pending.prompt,
                onConfirm = screenModel::confirmAiExplanation,
                onDismissRequest = screenModel::dismissAiConsent,
            )
        }
    }
}
