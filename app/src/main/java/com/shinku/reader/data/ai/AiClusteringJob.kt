package com.shinku.reader.data.ai

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.shinku.reader.R
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.domain.ai.interactor.AiClustering
import com.shinku.reader.util.system.notificationManager
import com.shinku.reader.util.system.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class AiClusteringJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val aiClustering: AiClustering = Injekt.get()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
            .setSmallIcon(R.drawable.ic_tachi)
            .setContentTitle("AI: Grouping Library")
            .setContentText("Finding vibes...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        
        context.notificationManager.notify(Notifications.ID_LIBRARY_PROGRESS, builder.build())

        try {
            aiClustering.await()
            
            context.notificationManager.notify(
                Notifications.ID_LIBRARY_PROGRESS,
                builder.setContentTitle("AI: Library Grouping Complete")
                    .setContentText("Your library is now organized by vibes")
                    .setOngoing(false)
                    .build()
            )
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to run AiClusteringJob" }
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AiClusteringJob"

        fun startNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AiClusteringJob>()
                .addTag(TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            context.workManager.enqueueUniqueWork(
                TAG,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
