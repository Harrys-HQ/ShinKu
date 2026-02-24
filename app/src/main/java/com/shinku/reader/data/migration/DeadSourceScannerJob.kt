package com.shinku.reader.data.migration

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.shinku.reader.R
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.util.system.setForegroundSafely
import com.shinku.reader.util.system.workManager
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.manga.interactor.GetLibraryManga
import com.shinku.reader.domain.source.model.StubSource
import com.shinku.reader.domain.source.service.SourceManager
import com.shinku.reader.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeadSourceScannerJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val sourceManager: SourceManager = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()

    private val notificationBuilder = NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
        .setSmallIcon(R.drawable.ic_refresh_24dp)
        .setContentTitle(context.stringResource(MR.strings.dead_source_scanner_title))
        .setOngoing(true)
        .setOnlyAlertOnce(true)

    override suspend fun doWork(): Result {
        setForegroundSafely()

        return try {
            val libraryManga = getLibraryManga.await()
            val distinctSources = libraryManga.map { it.manga.source }.distinct()
            val total = distinctSources.size
            val deadSourceIds = mutableSetOf<String>()

            distinctSources.forEachIndexed { index, sourceId ->
                val source = sourceManager.getOrStub(sourceId)
                if (source is StubSource && source.id != 1L) {
                    deadSourceIds.add(sourceId.toString())
                }
                updateProgress(index + 1, total)
            }

            libraryPreferences.deadSourceIds().set(deadSourceIds)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            notificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateProgress(progress: Int, total: Int) {
        val notification = notificationBuilder
            .setProgress(total, progress, false)
            .setContentText("$progress / $total")
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(Notifications.ID_LIBRARY_PROGRESS, notification)
    }

    companion object {
        private const val TAG = "DeadSourceScanner"

        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DeadSourceScannerJob>()
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }
    }
}
