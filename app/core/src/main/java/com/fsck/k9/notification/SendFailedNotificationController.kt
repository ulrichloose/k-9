package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.helper.ExceptionHelper

internal class SendFailedNotificationController(
    private val notificationHelper: NotificationHelper,
    private val actionBuilder: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider
) {
    fun showSendFailedNotification(account: Account, exception: Exception) {
        val title = resourceProvider.sendFailedTitle()
        val text = ExceptionHelper.getRootCauseMessage(exception)

        val notificationId = NotificationIds.getSendFailedNotificationId(account)

        val pendingIntent = account.outboxFolderId.let { outboxFolderId ->
            if (outboxFolderId != null) {
                actionBuilder.createViewFolderPendingIntent(
                    account, outboxFolderId, notificationId
                )
            } else {
                actionBuilder.createViewFolderListPendingIntent(
                    account, notificationId
                )
            }
        }

        val notificationBuilder = notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPublicVersion(createLockScreenNotification(account))
            .setCategory(NotificationCompat.CATEGORY_ERROR)

        notificationHelper.configureNotification(
            builder = notificationBuilder,
            ringtone = null,
            vibrationPattern = null,
            ledColor = NotificationHelper.NOTIFICATION_LED_FAILURE_COLOR,
            ledSpeed = NotificationHelper.NOTIFICATION_LED_BLINK_FAST,
            ringAndVibrate = true
        )

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun clearSendFailedNotification(account: Account) {
        val notificationId = NotificationIds.getSendFailedNotificationId(account)
        notificationManager.cancel(notificationId)
    }

    private fun createLockScreenNotification(account: Account): Notification {
        return notificationHelper
            .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
            .setSmallIcon(resourceProvider.iconWarning)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(resourceProvider.sendFailedTitle())
            .build()
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
