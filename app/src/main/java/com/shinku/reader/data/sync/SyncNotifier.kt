package com.shinku.reader.data.sync

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.shinku.reader.R
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.data.notification.NotificationReceiver
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.util.system.cancelNotification
import com.shinku.reader.util.system.notificationBuilder
import com.shinku.reader.util.system.notify
import uy.kohesive.injekt.injectLazy

class SyncNotifier(private val context: Context) {

    private val preferences: SecurityPreferences by injectLazy()

    private val progressNotificationBuilder = context.notificationBuilder(
        Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS,
    ) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_tachi)
        setAutoCancel(false)
        setOngoing(true)
        setOnlyAlertOnce(true)
    }

    private val completeNotificationBuilder = context.notificationBuilder(
        Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE,
    ) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_tachi)
        setAutoCancel(false)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notify(id, build())
    }

    fun showSyncProgress(content: String = "", progress: Int = 0, maxAmount: Int = 100): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.syncing_library))

            if (!preferences.hideNotificationContent().get()) {
                setContentText(content)
            }

            setProgress(maxAmount, progress, true)
            setOnlyAlertOnce(true)

            clearActions()
            addAction(
                R.drawable.ic_close_24dp,
                context.getString(R.string.action_cancel),
                NotificationReceiver.cancelSyncPendingBroadcast(context, Notifications.ID_RESTORE_PROGRESS),
            )
        }

        builder.show(Notifications.ID_RESTORE_PROGRESS)

        return builder
    }

    fun showSyncError(error: String?) {
        context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.sync_error))
            setContentText(error)

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }

    fun showSyncSuccess(message: String?) {
        context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.sync_complete))
            setContentText(message)

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }
}
