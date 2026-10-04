package com.shinku.reader.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.domain.source.interactor.UpdateSourceHealth
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.util.system.setForegroundSafely
import com.shinku.reader.util.system.workManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class RepoHealthScanJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val extensionManager: ExtensionManager = Injekt.get()
    private val networkHelper: NetworkHelper = Injekt.get()
    private val updateSourceHealth: UpdateSourceHealth = Injekt.get()
    private val notifier = RepoHealthScanNotifier(context)

    override suspend fun doWork(): Result {
        setForegroundSafely()
        val onlyInstalled = inputData.getBoolean(KEY_ONLY_INSTALLED, false)
        
        return withIOContext {
            try {
                // Fetch latest extension list and wait for it
                extensionManager.findAvailableExtensions()
                val latestExtensions = extensionManager.availableExtensionsFlow.value
                
                scanAllSources(latestExtensions, onlyInstalled)
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
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

    private suspend fun scanAllSources(availableExtensions: List<com.shinku.reader.extension.model.Extension.Available>, onlyInstalled: Boolean) = coroutineScope {
        val sourceManager = Injekt.get<SourceManager>()
        val onlineInstalledSources = sourceManager.getOnlineSources()
        val installedSourceIds = onlineInstalledSources.map { it.id }.toSet()

        logcat(LogPriority.INFO) { "Total available extensions in repo: ${availableExtensions.size}, installed online sources: ${onlineInstalledSources.size}" }

        // Gather sources to scan with preference for installed instances
        val filteredSources: List<com.shinku.reader.extension.model.Extension.Available.Source> = if (onlyInstalled) {
            onlineInstalledSources.mapNotNull { s ->
                if (s.baseUrl.isNotBlank()) {
                    com.shinku.reader.extension.model.Extension.Available.Source(
                        id = s.id,
                        lang = s.lang,
                        name = s.name,
                        baseUrl = s.baseUrl,
                    )
                } else null
            }
        } else {
            val repoSources = availableExtensions.flatMap { ext -> 
                ext.sources.filter { it.lang == "en" || it.id in installedSourceIds }
            }
            val installedAdditional = onlineInstalledSources.mapNotNull { s ->
                if (repoSources.none { it.id == s.id }) {
                    if (s.baseUrl.isNotBlank()) {
                        com.shinku.reader.extension.model.Extension.Available.Source(
                            id = s.id,
                            lang = s.lang,
                            name = s.name,
                            baseUrl = s.baseUrl,
                        )
                    } else null
                } else null
            }
            repoSources + installedAdditional
        }

        // Group by baseUrl so we only ping each site once
        val sourcesByUrl = filteredSources.groupBy { it.baseUrl }

        if (sourcesByUrl.isEmpty()) return@coroutineScope

        logcat(LogPriority.INFO) { "Starting repo health scan for ${sourcesByUrl.size} unique URLs (Only Installed: $onlyInstalled)" }

        val total = sourcesByUrl.size
        val processed = AtomicInteger(0)
        
        val semaphore = Semaphore(5)

        sourcesByUrl.entries.map { (baseUrl, sources) ->
            async {
                semaphore.withPermit {
                    ensureActive()
                    var success = false
                    var error: String? = null

                    val installedHttpSource = sources.firstNotNullOfOrNull { sourceManager.get(it.id) as? eu.kanade.tachiyomi.source.online.HttpSource }
                    val targetUrl = installedHttpSource?.baseUrl ?: baseUrl
                    val headers = installedHttpSource?.headers ?: okhttp3.Headers.Builder()
                        .add("User-Agent", networkHelper.defaultUserAgentProvider())
                        .build()
                    val client = (installedHttpSource?.client ?: networkHelper.client).newBuilder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()

                    val latency = measureTimeMillis {
                        try {
                            client.newCall(GET(targetUrl, headers)).awaitSuccess()
                            success = true
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                    
                    ensureActive()
                    // Apply health result to ALL source IDs associated with this URL
                    sources.forEach { source ->
                        try {
                            updateSourceHealth.await(source.id, success, if (success) latency else 0L, error)
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Failed to update source health stats for ${source.id}" }
                        }
                    }
                    
                    val current = processed.incrementAndGet()
                    notifier.showProgressNotification(current, total, sources.first().name)
                }
            }
        }.awaitAll()
        
        notifier.showCompleteNotification(total)
    }

    companion object {
        private const val TAG = "RepoHealthScan"
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
                    3, TimeUnit.DAYS,
                    12, TimeUnit.HOURS
                )
                    .addTag(TAG)
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setInitialDelay(3, TimeUnit.DAYS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "Failed to setup RepoHealthScanJob: ${e.message}" }
            }
        }

        fun startNow(context: Context, onlyInstalled: Boolean = false) {
            val inputData = workDataOf(
                KEY_ONLY_INSTALLED to onlyInstalled
            )
            val request = OneTimeWorkRequestBuilder<RepoHealthScanJob>()
                .addTag(TAG)
                .setInputData(inputData)
                .build()

            context.workManager.enqueueUniqueWork(
                TAG + "_manual",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun stop(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }
    }
}
