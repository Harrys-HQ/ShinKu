package com.shinku.reader.extension.api

import android.content.Context
import androidx.core.app.NotificationCompat
import com.shinku.reader.R
import com.shinku.reader.core.security.SecurityPreferences
import com.shinku.reader.data.notification.NotificationReceiver
import com.shinku.reader.data.notification.Notifications
import com.shinku.reader.util.system.cancelNotification
import com.shinku.reader.util.system.notify
import com.shinku.reader.core.common.i18n.pluralStringResource
import com.shinku.reader.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionUpdateNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences = Injekt.get(),
) {
    fun promptUpdates(names: List<String>) {
        context.notify(
            Notifications.ID_UPDATES_TO_EXTS,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(
                context.pluralStringResource(
                    MR.plurals.update_check_notification_ext_updates,
                    names.size,
                    names.size,
                ),
            )
            if (!securityPreferences.hideNotificationContent().get()) {
                val extNames = names.joinToString(", ")
                setContentText(extNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
            }
            setSmallIcon(R.drawable.ic_extension_24dp)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }

    fun dismiss() {
        context.cancelNotification(Notifications.ID_UPDATES_TO_EXTS)
    }
}
