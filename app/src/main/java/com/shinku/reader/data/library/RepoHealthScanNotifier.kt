package com.shinku.reader.data.library

import android.content.Context
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.shinku.reader.R
import com.shinku.reader.data.notification.NotificationReceiver
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.i18n.MR
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.util.system.cancelNotification
import com.shinku.reader.util.system.notificationBuilder
import com.shinku.reader.util.system.notify
import java.math.RoundingMode
import java.text.NumberFormat

class RepoHealthScanNotifier(private val context: Context) {

    private val percentFormatter = NumberFormat.getPercentInstance().apply {
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 0
    }

    private val cancelIntent by lazy {
        NotificationReceiver.cancelRepoHealthScanPendingBroadcast(context)
    }

    val progressNotificationBuilder by lazy {
        context.notificationBuilder(Notifications.CHANNEL_REPO_HEALTH_SCAN) {
            setContentTitle("Scanning Repository Health")
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close_24dp, context.stringResource(MR.strings.action_cancel), cancelIntent)
        }
    }

    fun showProgressNotification(current: Int, total: Int, name: String) {
        context.notify(
            Notifications.ID_REPO_HEALTH_SCAN,
            progressNotificationBuilder
                .setContentTitle("Scanning: $name")
                .setContentText(percentFormatter.format(current.toFloat() / total))
                .setProgress(total, current, false)
                .build(),
        )
    }

    fun cancelProgressNotification() {
        context.cancelNotification(Notifications.ID_REPO_HEALTH_SCAN)
    }
    
    fun showCompleteNotification(total: Int) {
        context.notify(
            Notifications.ID_REPO_HEALTH_SCAN,
            context.notificationBuilder(Notifications.CHANNEL_REPO_HEALTH_SCAN) {
                setContentTitle("Repo Health Scan Complete")
                setContentText("Scanned $total sources")
                setSmallIcon(R.drawable.ic_done_24dp)
                setAutoCancel(true)
            }.build()
        )
    }
}
