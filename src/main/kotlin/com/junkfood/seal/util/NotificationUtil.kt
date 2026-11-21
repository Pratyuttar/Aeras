package com.junkfood.seal.util

import android.app.PendingIntent
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.R
import io.github.pratyuttar.aeras.DesktopNotifier
import com.junkfood.seal.util.PreferenceUtil.getBoolean

/**
 * Desktop version. Progress is shown in the app window itself, so the per-task progress
 * notifications Android needs are dropped; finished and failed downloads raise a system
 * notification when the window is not in front.
 */
object NotificationUtil {
    private const val DEFAULT_NOTIFICATION_ID = 100
    const val SERVICE_NOTIFICATION_ID = 123

    fun createNotificationChannel() {}

    fun notifyProgress(
        title: String,
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        progress: Int = 0,
        taskId: String? = null,
        text: String? = null,
    ) {}

    fun finishNotification(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
        intent: PendingIntent? = null,
    ) {
        if (!NOTIFICATION.getBoolean()) return
        DesktopNotifier.notify(
            title = title ?: context.getString(R.string.app_name),
            text = text ?: "",
            onClick = intent?.let { pending -> { runCatching { context.startActivity(pending.intent) } } },
        )
    }

    fun finishNotificationForCustomCommands(
        notificationId: Int = DEFAULT_NOTIFICATION_ID,
        title: String? = null,
        text: String? = null,
    ) {}

    fun updateServiceNotificationForPlaylist(index: Int, itemCount: Int) {}

    fun cancelNotification(notificationId: Int) {}

    fun notifyError(
        title: String,
        textId: Int = R.string.download_error_msg,
        notificationId: Int,
        report: String,
    ) {
        if (!NOTIFICATION.getBoolean()) return
        DesktopNotifier.notify(
            title = title.ifEmpty { context.getString(R.string.app_name) },
            text = context.getString(textId),
            isError = true,
        )
    }

    fun makeNotificationForCustomCommand(
        notificationId: Int,
        taskId: String,
        progress: Int,
        text: String? = null,
        templateName: String,
        taskUrl: String,
    ) {}

    fun cancelAllNotifications() {}

    fun areNotificationsEnabled(): Boolean = DesktopNotifier.isSupported()
}
