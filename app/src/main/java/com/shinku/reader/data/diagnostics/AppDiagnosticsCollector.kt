package com.shinku.reader.data.diagnostics

import android.content.Context
import android.os.Build
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import com.shinku.reader.BuildConfig
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.domain.diagnostics.AppDiagnosticsReport
import com.shinku.reader.domain.diagnostics.DiagnosticCheckResult
import com.shinku.reader.domain.diagnostics.DiagnosticCheckStatus
import com.shinku.reader.domain.diagnostics.DiagnosticRedactor
import com.shinku.reader.domain.diagnostics.DiagnosticSectionId
import com.shinku.reader.domain.diagnostics.RepositoryHealthCheck
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepo
import com.shinku.reader.domain.source.interactor.GetSourceHealth
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.model.SourceHealthStatus
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.exh.syDebugVersion
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.util.system.WebViewUtil
import com.shinku.reader.util.system.isConnectedToVpn
import com.shinku.reader.util.system.isConnectedToWifi
import com.shinku.reader.util.system.isOnline
import com.shinku.reader.util.system.workManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.coroutines.coroutineContext

/**
 * Collects local, offline-safe diagnostic snapshots and runs explicit read-only repository probes on request.
 * Never includes manga/chapter titles, reading history, library contents, full repository URLs, or credentials.
 */
class AppDiagnosticsCollector(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceManager: SourceManager,
    private val getSourceHealth: GetSourceHealth,
    private val getExtensionRepo: GetExtensionRepo,
    private val networkHelper: NetworkHelper,
    private val networkPreferences: NetworkPreferences,
    private val diagnosticsStore: DiagnosticsStore,
    private val protoBuf: ProtoBuf,
    private val json: Json,
) {

    suspend fun collectLocalReport(
        correlationId: String = diagnosticsStore.newCorrelationId(),
    ): AppDiagnosticsReport = withIOContext {
        val now = System.currentTimeMillis()
        val recentEvents = diagnosticsStore.recentEvents.value
        val checks = listOf(
            collectEnvironmentCheck(now, correlationId),
            collectWebViewCheck(now, correlationId),
            collectNetworkCheck(now, correlationId),
            collectWorkManagerCheck(now, correlationId),
            collectRepositoriesCheck(now, correlationId),
            collectExtensionLoadingCheck(now, correlationId),
            collectSourceHealthCheck(now, correlationId),
            collectRecentErrorsCheck(now, correlationId, recentEvents.count { it.category != SourceHealthCategory.HEALTHY }),
        )
        AppDiagnosticsReport(
            correlationId = correlationId,
            generatedAt = now,
            checks = checks,
            recentEvents = recentEvents.take(25),
        )
    }

    suspend fun probeRepositories(
        correlationId: String = diagnosticsStore.newCorrelationId(),
    ): List<RepositoryHealthCheck> = withIOContext {
        val repos = getExtensionRepo.getAll()
        repos.map { repo ->
            coroutineContext.ensureActive()
            val result = SourceHealthProber.probeExtensionRepo(
                client = networkHelper.nonCloudflareClient,
                repo = repo,
                protoBuf = protoBuf,
                json = json,
                isNetworkAvailable = context::isOnline,
                correlationId = correlationId,
            )
            diagnosticsStore.recordRepoCheck(repo.baseUrl, result)
            result
        }
    }

    private fun collectEnvironmentCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val evidence = listOf(
            "App: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE}, ${BuildConfig.FLAVOR}, ${BuildConfig.COMMIT_SHA}, r$syDebugVersion)",
            "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})",
        )
        return DiagnosticCheckResult(
            id = DiagnosticSectionId.APP_ENVIRONMENT,
            title = DiagnosticSectionId.APP_ENVIRONMENT.title,
            status = DiagnosticCheckStatus.OK,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.COMPLETED,
            summary = "ShinKu ${BuildConfig.VERSION_NAME} on Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            evidence = evidence,
        )
    }

    private fun collectWebViewCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val supported = runCatching { WebViewUtil.supportsWebView(context) }.getOrDefault(false)
        val version = runCatching { WebViewUtil.getVersion(context) }.getOrDefault("Unknown")
        val majorVersion = Regex("""(\d+)\.""").find(version)?.groupValues?.getOrNull(1)?.toIntOrNull()

        val (status, summary, nextStep) = when {
            !supported -> Triple(
                DiagnosticCheckStatus.FAILED,
                "Android WebView package is unavailable ($version)",
                "Install or enable Android System WebView / Chrome so source verification can open.",
            )
            majorVersion != null && majorVersion < WebViewUtil.MINIMUM_WEBVIEW_VERSION -> Triple(
                DiagnosticCheckStatus.WARNING,
                "WebView may be outdated: $version (minimum recommended ${WebViewUtil.MINIMUM_WEBVIEW_VERSION})",
                "Update Android System WebView from Google Play if Cloudflare or site logins fail.",
            )
            else -> Triple(
                DiagnosticCheckStatus.OK,
                "WebView available: $version",
                null,
            )
        }
        return DiagnosticCheckResult(
            id = DiagnosticSectionId.WEBVIEW_RUNTIME,
            title = DiagnosticSectionId.WEBVIEW_RUNTIME.title,
            status = status,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.COMPLETED,
            summary = summary,
            evidence = listOf("Supported: $supported", "Version: $version"),
            nextStep = nextStep,
        )
    }

    private fun collectNetworkCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val isOnline = runCatching { context.isOnline() }.getOrDefault(false)
        val isWifi = runCatching { context.isConnectedToWifi() }.getOrDefault(false)
        val isVpn = runCatching { context.isConnectedToVpn() }.getOrDefault(false)
        val dohId = runCatching { networkPreferences.dohProvider().get() }.getOrDefault(-1)

        val evidence = listOf(
            "Online: $isOnline",
            "Wi-Fi: $isWifi | VPN: $isVpn",
            if (dohId >= 0) "DNS over HTTPS: enabled (provider #$dohId)" else "DNS over HTTPS: disabled",
        )
        return DiagnosticCheckResult(
            id = DiagnosticSectionId.NETWORK_CONNECTIVITY,
            title = DiagnosticSectionId.NETWORK_CONNECTIVITY.title,
            status = if (isOnline) DiagnosticCheckStatus.OK else DiagnosticCheckStatus.WARNING,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.COMPLETED,
            summary = if (isOnline) "Device network transport active" else "Device appears offline",
            evidence = evidence,
            nextStep = if (isOnline) null else "Connect to Wi-Fi or mobile data before running repository or source checks.",
        )
    }

    private suspend fun collectWorkManagerCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        return try {
            val workInfos = context.workManager
                .getWorkInfos(
                    WorkQuery.fromStates(
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.BLOCKED,
                        WorkInfo.State.FAILED,
                    ),
                )
                .await()

            val running = workInfos.count { it.state == WorkInfo.State.RUNNING }
            val queued = workInfos.count { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            val failed = workInfos.count { it.state == WorkInfo.State.FAILED }
            val liveScan = diagnosticsStore.liveScanState.value

            val evidence = buildList {
                add("Running: $running | Queued/Blocked: $queued | Failed records: $failed")
                if (liveScan.isRunning) {
                    add("Active health scan [${liveScan.correlationId}]: ${liveScan.completedCount}/${liveScan.totalCount} (${liveScan.currentPhase.name})")
                }
                workInfos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.FAILED }
                    .take(5)
                    .forEach { info ->
                        val tagLabel = info.tags.firstOrNull { !it.contains('.') } ?: info.tags.firstOrNull() ?: "worker"
                        add("Worker $tagLabel: ${info.state.name} (attempt ${info.runAttemptCount + 1})")
                    }
            }

            DiagnosticCheckResult(
                id = DiagnosticSectionId.WORK_MANAGER,
                title = DiagnosticSectionId.WORK_MANAGER.title,
                status = if (failed > 0) DiagnosticCheckStatus.WARNING else DiagnosticCheckStatus.OK,
                timestamp = now,
                correlationId = correlationId,
                phase = DiagnosticPhase.COMPLETED,
                summary = "Workers — $running running, $queued queued, $failed failed",
                evidence = evidence,
                nextStep = if (failed > 0) "Review background worker status or retry the failed task." else null,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            DiagnosticCheckResult(
                id = DiagnosticSectionId.WORK_MANAGER,
                title = DiagnosticSectionId.WORK_MANAGER.title,
                status = DiagnosticCheckStatus.NOT_CHECKED,
                timestamp = now,
                correlationId = correlationId,
                phase = DiagnosticPhase.IDLE,
                summary = "WorkManager state unavailable",
            )
        }
    }

    private suspend fun collectRepositoriesCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val repos = getExtensionRepo.getAll()
        if (repos.isEmpty()) {
            return DiagnosticCheckResult(
                id = DiagnosticSectionId.EXTENSION_REPOS,
                title = DiagnosticSectionId.EXTENSION_REPOS.title,
                status = DiagnosticCheckStatus.NOT_CHECKED,
                timestamp = now,
                correlationId = correlationId,
                phase = DiagnosticPhase.REPOSITORY_FETCH,
                summary = "No extension repositories configured",
                nextStep = "Add a repository in More → Settings → Browse → Extension repositories if you need catalog updates.",
            )
        }

        val recorded = diagnosticsStore.repoHealthChecks.value
        val checks = repos.map { repo ->
            recorded[repo.baseUrl] ?: RepositoryHealthCheck(
                repoName = repo.shortName?.takeIf { it.isNotBlank() } ?: repo.name.ifBlank { "Repository" },
                repoHost = DiagnosticRedactor.redactRepoUrl(repo.baseUrl),
                status = DiagnosticCheckStatus.NOT_CHECKED,
                category = SourceHealthCategory.UNKNOWN,
                checkedAt = 0L,
                phase = DiagnosticPhase.IDLE,
                summary = "Not checked yet",
            )
        }

        val failedCount = checks.count { it.status == DiagnosticCheckStatus.FAILED }
        val warningCount = checks.count { it.status == DiagnosticCheckStatus.WARNING }
        val uncheckedCount = checks.count { it.status == DiagnosticCheckStatus.NOT_CHECKED }

        val status = when {
            failedCount > 0 -> DiagnosticCheckStatus.FAILED
            warningCount > 0 -> DiagnosticCheckStatus.WARNING
            uncheckedCount == checks.size -> DiagnosticCheckStatus.NOT_CHECKED
            uncheckedCount > 0 -> DiagnosticCheckStatus.WARNING
            else -> DiagnosticCheckStatus.OK
        }

        val evidence = checks.map { check ->
            val codePart = check.statusCode?.let { " [HTTP $it]" }.orEmpty()
            "${check.repoName} (${check.repoHost}): ${check.status.name}$codePart — ${check.summary}"
        }

        val summary = when (status) {
            DiagnosticCheckStatus.OK -> "All ${checks.size} configured repositories healthy"
            DiagnosticCheckStatus.NOT_CHECKED -> "${checks.size} repositories configured (not checked yet)"
            DiagnosticCheckStatus.WARNING -> "${checks.size} repositories: $warningCount warning, $uncheckedCount unchecked"
            DiagnosticCheckStatus.FAILED -> "${checks.size} repositories: $failedCount failed"
        }

        val nextStep = checks.firstOrNull { it.status == DiagnosticCheckStatus.FAILED || it.status == DiagnosticCheckStatus.WARNING }?.nextStep
            ?: if (uncheckedCount > 0) "Tap Check repositories to run a read-only index verification." else null

        return DiagnosticCheckResult(
            id = DiagnosticSectionId.EXTENSION_REPOS,
            title = DiagnosticSectionId.EXTENSION_REPOS.title,
            status = status,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.REPOSITORY_FETCH,
            summary = summary,
            evidence = evidence,
            nextStep = nextStep,
        )
    }

    private suspend fun collectExtensionLoadingCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val installed = extensionManager.installedExtensionsFlow.first()
        val untrusted = extensionManager.untrustedExtensionsFlow.first()
        val failed = extensionManager.failedExtensionsFlow.value
        val obsoleteCount = installed.count { it.isObsolete }
        val updatesCount = installed.count { it.hasUpdate }

        val status = when {
            failed.isNotEmpty() -> DiagnosticCheckStatus.FAILED
            untrusted.isNotEmpty() || obsoleteCount > 0 -> DiagnosticCheckStatus.WARNING
            else -> DiagnosticCheckStatus.OK
        }

        val evidence = buildList {
            add("Loaded: ${installed.size} (Updates: $updatesCount, Not in repos: $obsoleteCount)")
            add("Untrusted: ${untrusted.size} | Failed to load: ${failed.size}")
            failed.take(8).forEach { issue ->
                val detailPart = issue.detail?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
                add("Load failed: ${issue.name} (${issue.reason.name})$detailPart")
            }
            untrusted.take(5).forEach { ext ->
                add("Untrusted: ${ext.name} v${ext.versionName}")
            }
        }

        val summary = when {
            failed.isNotEmpty() -> "${failed.size} extension(s) failed to load; ${installed.size} loaded"
            untrusted.isNotEmpty() -> "${untrusted.size} extension(s) require trust approval"
            obsoleteCount > 0 -> "${installed.size} loaded ($obsoleteCount not found in current repos)"
            else -> "${installed.size} installed extension(s) loaded normally"
        }

        val nextStep = when {
            failed.isNotEmpty() -> "Inspect failed extension compatibility/signing or reinstall from a trusted repository."
            untrusted.isNotEmpty() -> "Open Browse → Extensions to review and trust pending extensions."
            else -> null
        }

        return DiagnosticCheckResult(
            id = DiagnosticSectionId.EXTENSION_LOADING,
            title = DiagnosticSectionId.EXTENSION_LOADING.title,
            status = status,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.EXTENSION_LOAD,
            summary = summary,
            evidence = evidence,
            nextStep = nextStep,
        )
    }

    private suspend fun collectSourceHealthCheck(now: Long, correlationId: String): DiagnosticCheckResult {
        val onlineSources = sourceManager.getOnlineSources()
        val healthMap = getSourceHealth.subscribeAll().first().associateBy { it.sourceId }
        val checkedInstalled = onlineSources.mapNotNull { source ->
            healthMap[source.id]?.takeIf { it.hasCheck }?.let { source.name to it }
        }

        if (checkedInstalled.isEmpty()) {
            return DiagnosticCheckResult(
                id = DiagnosticSectionId.SOURCE_HEALTH,
                title = DiagnosticSectionId.SOURCE_HEALTH.title,
                status = DiagnosticCheckStatus.NOT_CHECKED,
                timestamp = now,
                correlationId = correlationId,
                phase = DiagnosticPhase.SOURCE_REQUEST,
                summary = "${onlineSources.size} installed online source(s); none checked yet",
                evidence = listOf("Installed online sources: ${onlineSources.size}"),
                nextStep = "Open Source Health to run an on-demand reachability scan.",
            )
        }

        var healthy = 0
        var stale = 0
        var degraded = 0
        var unreachable = 0
        var suspectedDead = 0
        var unknown = 0

        val issueLines = mutableListOf<String>()
        checkedInstalled.forEach { (name, health) ->
            when (val status = health.effectiveStatus(now)) {
                SourceHealthStatus.HEALTHY -> healthy++
                SourceHealthStatus.STALE -> stale++
                SourceHealthStatus.DEGRADED -> {
                    degraded++
                    issueLines += "$name: DEGRADED (${health.effectiveCategory.name})"
                }
                SourceHealthStatus.UNREACHABLE -> {
                    unreachable++
                    issueLines += "$name: UNREACHABLE (${health.effectiveCategory.name}, streak ${health.effectiveConsecutiveFailures})"
                }
                SourceHealthStatus.SUSPECTED_DEAD -> {
                    suspectedDead++
                    issueLines += "$name: LIKELY DEAD (${health.effectiveCategory.name}, streak ${health.effectiveConsecutiveFailures})"
                }
                SourceHealthStatus.UNKNOWN -> unknown++
            }
        }

        val checkStatus = when {
            suspectedDead > 0 -> DiagnosticCheckStatus.FAILED
            unreachable > 0 || degraded > 0 || stale > 0 -> DiagnosticCheckStatus.WARNING
            healthy > 0 -> DiagnosticCheckStatus.OK
            else -> DiagnosticCheckStatus.NOT_CHECKED
        }

        val evidence = buildList {
            add("Installed online: ${onlineSources.size} | Checked: ${checkedInstalled.size}")
            add("Healthy: $healthy | Degraded: $degraded | Unreachable: $unreachable | Likely dead (3+): $suspectedDead | Stale: $stale | Unknown: $unknown")
            addAll(issueLines.take(10))
        }

        val summary = "Sources — $healthy healthy, $degraded degraded, $unreachable unreachable, $suspectedDead likely dead (3+ failures)"
        val nextStep = if (unreachable > 0 || degraded > 0 || suspectedDead > 0) {
            "Open Source Health for per-source HTTP/DNS/TLS status and suggested actions. Single failures are not treated as dead."
        } else if (stale > 0) {
            "Some source checks are older than 7 days. Re-scan in Source Health for current results."
        } else {
            null
        }

        return DiagnosticCheckResult(
            id = DiagnosticSectionId.SOURCE_HEALTH,
            title = DiagnosticSectionId.SOURCE_HEALTH.title,
            status = checkStatus,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.SOURCE_REQUEST,
            summary = summary,
            evidence = evidence,
            nextStep = nextStep,
        )
    }

    private fun collectRecentErrorsCheck(now: Long, correlationId: String, errorCount: Int): DiagnosticCheckResult {
        return DiagnosticCheckResult(
            id = DiagnosticSectionId.RECENT_ERRORS,
            title = DiagnosticSectionId.RECENT_ERRORS.title,
            status = if (errorCount == 0) DiagnosticCheckStatus.OK else DiagnosticCheckStatus.WARNING,
            timestamp = now,
            correlationId = correlationId,
            phase = DiagnosticPhase.COMPLETED,
            summary = "$errorCount recent diagnostic warnings or failures (opt-in for export)",
        )
    }
}
