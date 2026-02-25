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
        val installedSourceIds = Injekt.get<SourceManager>().getOnlineSources().map { it.id }.toSet()

        logcat(LogPriority.INFO) { "Total available extensions in repo: ${availableExtensions.size}" }

        // Filter for English sources OR sources that are already installed
        val filteredSources = availableExtensions.flatMap { ext -> 
            ext.sources.filter { 
                if (onlyInstalled) {
                    it.id in installedSourceIds
                } else {
                    it.lang == "en" || it.id in installedSourceIds
                }
            }
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
                    val latency = measureTimeMillis {
                        try {
                            val client = networkHelper.client.newBuilder()
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .readTimeout(15, TimeUnit.SECONDS)
                                .build()
                            
                            client.newCall(GET(baseUrl)).awaitSuccess()
                            success = true
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                    
                    ensureActive()
                    // Apply health result to ALL source IDs associated with this URL
                    sources.forEach { source ->
                        updateSourceHealth.await(source.id, success, latency, error)
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
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<RepoHealthScanJob>(
                3, TimeUnit.DAYS,
                12, TimeUnit.HOURS
            )
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            context.workManager.enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
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
