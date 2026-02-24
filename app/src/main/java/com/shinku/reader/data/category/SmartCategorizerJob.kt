package com.shinku.reader.data.category

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
import com.shinku.reader.domain.category.interactor.SmartCategorizer
import com.shinku.reader.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SmartCategorizerJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val smartCategorizer: SmartCategorizer = Injekt.get()

    private val notificationBuilder = NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
        .setSmallIcon(R.drawable.ic_refresh_24dp)
        .setContentTitle(context.stringResource(MR.strings.notification_smart_categorizing))
        .setOngoing(true)
        .setOnlyAlertOnce(true)

    override suspend fun doWork(): Result {
        setForegroundSafely()

        val names = SmartCategorizer.CategoryNames(
            all = context.stringResource(MR.strings.smart_categorizer_all),
            reading = context.stringResource(MR.strings.smart_categorizer_reading),
            queue = context.stringResource(MR.strings.smart_categorizer_queue),
            finished = context.stringResource(MR.strings.smart_categorizer_finished),
            dropped = context.stringResource(MR.strings.smart_categorizer_dropped),
        )

        return try {
            smartCategorizer.await(names) { progress, total ->
                updateProgress(progress, total)
            }
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
        private const val TAG = "SmartCategorizer"

        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmartCategorizerJob>()
                .addTag(TAG)
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }
    }
}
