package com.shinku.reader.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.diagnostics.DiagnosticsStore
import com.shinku.reader.data.diagnostics.SourceHealthProber
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.domain.diagnostics.DiagnosticRunEvent
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepo
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.domain.source.model.HealthProbeType
import com.shinku.reader.domain.source.model.SourceHealthCategory
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.util.system.isOnline
import com.shinku.reader.util.system.setForegroundSafely
import com.shinku.reader.util.system.workManager
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RepoHealthScanJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val extensionManager: ExtensionManager = Injekt.get()
    private val networkHelper: NetworkHelper = Injekt.get()
    private val updateSourceHealth: UpdateSourceHealth = Injekt.get()
    private val diagnosticsStore: DiagnosticsStore = Injekt.get()
    private val getExtensionRepo: GetExtensionRepo = Injekt.get()
    private val protoBuf: ProtoBuf = Injekt.get()
    private val json: Json = Injekt.get()
    private val notifier = RepoHealthScanNotifier(context)

    override suspend fun doWork(): Result {
        val onlyInstalled = inputData.getBoolean(KEY_ONLY_INSTALLED, false)
        val correlationId = diagnosticsStore.newCorrelationId()

        return withIOContext {
            if (!diagnosticsStore.startScan(correlationId, totalCount = 0, workId = id)) return@withIOContext Result.retry()
            var finalPhase = DiagnosticPhase.FAILED
            try {
                setForegroundSafely()
                if (!onlyInstalled) {
                    getExtensionRepo.getAll().forEach { repo ->
                        ensureActive()
                        val check = SourceHealthProber.probeExtensionRepo(
                            client = networkHelper.nonCloudflareClient,
                            repo = repo,
                            protoBuf = protoBuf,
                            json = json,
                            isNetworkAvailable = context::isOnline,
                            correlationId = correlationId,
                        )
                        diagnosticsStore.recordRepoCheck(repo.baseUrl, check)
                    }
                }
                val latestExtensions = extensionManager.availableExtensionsFlow.value
                scanAllSources(latestExtensions, onlyInstalled, correlationId)
                finalPhase = DiagnosticPhase.COMPLETED
                Result.success()
            } catch (e: CancellationException) {
                finalPhase = DiagnosticPhase.CANCELLED
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                Result.failure()
            } finally {
                try {
                    notifier.cancelProgressNotification()
                } finally {
                    diagnosticsStore.finishScan(correlationId, finalPhase)
                }
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_REPO_HEALTH_SCAN,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun scanAllSources(
        availableExtensions: List<com.shinku.reader.extension.model.Extension.Available>,
        onlyInstalled: Boolean,
        correlationId: String,
    ) = coroutineScope {
        val sourceManager = Injekt.get<SourceManager>()
        val onlineInstalledSources = sourceManager.getOnlineSources()
        val installedSourceIds = onlineInstalledSources.map { it.id }.toSet()

        logcat(LogPriority.INFO) {
            "Total available extensions in repo: ${availableExtensions.size}, installed online sources: ${onlineInstalledSources.size}"
        }

        val filteredSources: List<com.shinku.reader.extension.model.Extension.Available.Source> = if (onlyInstalled) {
            onlineInstalledSources.mapNotNull { s ->
                if (s.baseUrl.isNotBlank()) {
                    com.shinku.reader.extension.model.Extension.Available.Source(
                        id = s.id,
                        lang = s.lang,
                        name = s.name,
                        baseUrl = s.baseUrl,
                    )
                } else {
                    null
                }
            }
        } else {
            val repoSources = availableExtensions.flatMap { ext ->
                ext.sources.filter { it.lang == "en" || it.id in installedSourceIds }
            }
            val installedAdditional = onlineInstalledSources.mapNotNull { s ->
                if (repoSources.none { it.id == s.id } && s.baseUrl.isNotBlank()) {
                    com.shinku.reader.extension.model.Extension.Available.Source(
                        id = s.id,
                        lang = s.lang,
                        name = s.name,
                        baseUrl = s.baseUrl,
                    )
                } else {
                    null
                }
            }
            repoSources + installedAdditional
        }

        val uniqueSources = filteredSources.distinctBy { it.id }
        val sourcesByUrl = uniqueSources.groupBy { it.baseUrl }
        if (sourcesByUrl.isEmpty()) return@coroutineScope

        logcat(LogPriority.INFO) { "Starting repo health scan [$correlationId] for ${sourcesByUrl.size} unique URLs (Only Installed: $onlyInstalled)" }

        val total = uniqueSources.size
        diagnosticsStore.updateScanTotal(correlationId, total)
        val processed = AtomicInteger(0)
        val semaphore = Semaphore(5)

        sourcesByUrl.entries.map { (baseUrl, sources) ->
            async {
                semaphore.withPermit {
                    ensureActive()
                    diagnosticsStore.updateScanTarget(correlationId, sources.first().name)
                    val outcome = SourceHealthProber.probeSourceBaseUrl(
                        client = networkHelper.nonCloudflareClient,
                        targetUrl = baseUrl,
                        isNetworkAvailable = context::isOnline,
                        correlationId = correlationId,
                        probeType = HealthProbeType.BASE_URL_GET,
                    )

                    ensureActive()
                    sources.forEach { source ->
                        try {
                            updateSourceHealth.await(source.id, outcome)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Failed to update source health stats for ${source.id}" }
                            diagnosticsStore.recordEvent(
                                DiagnosticRunEvent(
                                    correlationId = correlationId,
                                    timestamp = System.currentTimeMillis(),
                                    phase = DiagnosticPhase.DATABASE_UPDATE,
                                    targetName = source.name,
                                    category = SourceHealthCategory.UNKNOWN,
                                    summary = "Database update failed",
                                ),
                            )
                        }
                        diagnosticsStore.recordScanProbe(source.id, source.name, outcome)
                    }

                    val current = processed.addAndGet(sources.size)
                    notifier.showProgressNotification(current, total, sources.first().name)
                }
            }
        }.awaitAll()

        notifier.showCompleteNotification(total)
    }

    companion object {
        private const val TAG = "RepoHealthScan"
        private const val MANUAL_WORK_NAME = "RepoHealthScan_manual"
        private const val KEY_ONLY_INSTALLED = "only_installed"

        fun setupTask(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val inputData = workDataOf(
                    KEY_ONLY_INSTALLED to true,
                )

                val request = PeriodicWorkRequestBuilder<RepoHealthScanJob>(
                    3,
                    TimeUnit.DAYS,
                    12,
                    TimeUnit.HOURS,
                )
                    .addTag(TAG)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setInitialDelay(3, TimeUnit.DAYS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Failed to setup RepoHealthScanJob: ${e.message}" }
            }
        }

        fun startNow(context: Context, onlyInstalled: Boolean = false) {
            val inputData = workDataOf(
                KEY_ONLY_INSTALLED to onlyInstalled,
            )
            val request = OneTimeWorkRequestBuilder<RepoHealthScanJob>()
                .addTag(TAG)
                .setInputData(inputData)
                .build()

            context.workManager.enqueueUniqueWork(
                MANUAL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            val workManager = appContext.workManager
            val workId = Injekt.get<DiagnosticsStore>().liveScanState.value.workId
            if (workId == null) {
                workManager.cancelUniqueWork(MANUAL_WORK_NAME)
                return
            }
            val cancellation = workManager.cancelWorkById(workId).result
            cancellation.addListener(
                {
                    // KEEP preserves an existing periodic schedule and recreates one if it was cancelled.
                    if (runCatching { cancellation.get() }.isSuccess) setupTask(appContext)
                },
                ContextCompat.getMainExecutor(appContext),
            )
        }
    }
}
