package com.shinku.reader.data.download

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.shinku.reader.R
import com.shinku.reader.data.notification.NotificationHandler
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.util.system.cancelNotification
import com.shinku.reader.util.system.notificationBuilder
import com.shinku.reader.util.system.notify

internal class DownloadMigrationNotifier(private val context: Context) {

    private val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setContentTitle("Migrating downloads")
            setSmallIcon(android.R.drawable.stat_sys_download)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
        }
    }

    private val completeNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setAutoCancel(true)
            setContentTitle("Migration complete")
            setSmallIcon(R.drawable.ic_done_24dp)
            setContentIntent(NotificationHandler.openDownloadManagerPendingActivity(context))
        }
    }

    private val errorNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_ERROR) {
            setAutoCancel(false)
            setContentTitle("Migration error")
            setSmallIcon(R.drawable.ic_warning_white_24dp)
        }
    }

    fun showIndeterminateProgress() {
        with(progressNotificationBuilder) {
            setContentText("Searching for legacy downloads...")
            setProgress(0, 0, true)
            setOngoing(true)
            show(Notifications.ID_DOWNLOAD_MIGRATION_PROGRESS)
        }
    }

    fun updateProgress(migratedCount: Int) {
        with(progressNotificationBuilder) {
            setContentText("Migrated $migratedCount chapters")
            setProgress(0, 0, true) // Keep indeterminate but update text
            show(Notifications.ID_DOWNLOAD_MIGRATION_PROGRESS)
        }
    }

    fun showComplete(migratedCount: Int) {
        dismissProgress()
        with(completeNotificationBuilder) {
            setContentText("Successfully migrated $migratedCount chapters")
            show(Notifications.ID_DOWNLOAD_MIGRATION_COMPLETE)
        }
    }

    fun showError(error: String) {
        dismissProgress()
        with(errorNotificationBuilder) {
            setContentText(error)
            show(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)
        }
    }

    fun dismissProgress() {
        context.cancelNotification(Notifications.ID_DOWNLOAD_MIGRATION_PROGRESS)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notify(id, build())
    }
}
