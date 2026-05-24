package com.shinku.reader.data.database

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.cash.sqldelight.db.SqlDriver
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

/**
 * Worker that performs periodic database maintenance, such as VACUUM.
 */
class DatabaseMaintenanceWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        logcat(LogPriority.INFO) { "Starting database maintenance" }
        return try {
            val driver = Injekt.get<SqlDriver>()
            driver.execute(null, "VACUUM", 0)
            logcat(LogPriority.INFO) { "Database maintenance finished successfully" }
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "Database maintenance failed: ${e.message}" }
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DatabaseMaintenance"

        fun setupPeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(
                7, TimeUnit.DAYS, // Run weekly
                1, TimeUnit.HOURS, // Flex period
            )
                .addTag(TAG)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
