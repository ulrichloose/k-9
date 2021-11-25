package com.fsck.k9.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.WearableExtender
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import com.fsck.k9.notification.NotificationIds.getNewMailSummaryNotificationId
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal class SummaryNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator,
    private val singleMessageNotificationCreator: SingleMessageNotificationCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val notificationManager: NotificationManagerCompat
) {
    fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData
    ) {
        when (summaryNotificationData) {
            is SummarySingleNotificationData -> {
                createSingleMessageNotification(baseNotificationData, summaryNotificationData.singleNotificationData)
            }
            is SummaryInboxNotificationData -> {
                createInboxStyleSummaryNotification(baseNotificationData, summaryNotificationData)
            }
        }
    }

    private fun createSingleMessageNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData
    ) {
        singleMessageNotificationCreator.createSingleNotification(
            baseNotificationData,
            singleNotificationData,
            isGroupSummary = true
        )
    }

    private fun createInboxStyleSummaryNotification(
        baseNotificationData: BaseNotificationData,
        notificationData: SummaryInboxNotificationData
    ) {
        val account = baseNotificationData.account
        val accountName = baseNotificationData.accountName
        val newMessagesCount = baseNotificationData.newMessagesCount
        val title = resourceProvider.newMessagesTitle(newMessagesCount)
        val summary = buildInboxSummaryText(accountName, notificationData)

        val notification = notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setAutoCancel(true)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(true)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setWhen(notificationData.timestamp)
            .setNumber(notificationData.additionalMessagesCount)
            .setTicker(notificationData.content.firstOrNull())
            .setContentTitle(title)
            .setSubText(accountName)
            .setInboxStyle(title, summary, notificationData.content)
            .setContentIntent(createViewIntent(account, notificationData))
            .setDeleteIntent(createDismissIntent(account, notificationData.notificationId))
            .setDeviceActions(account, notificationData)
            .setWearActions(account, notificationData)
            .setAppearance(notificationData.isSilent, baseNotificationData.appearance)
            .setLockScreenNotification(baseNotificationData)
            .build()

        notificationManager.notify(notificationData.notificationId, notification)
    }

    private fun buildInboxSummaryText(accountName: String, notificationData: SummaryInboxNotificationData): String {
        return if (notificationData.additionalMessagesCount > 0) {
            resourceProvider.additionalMessages(notificationData.additionalMessagesCount, accountName)
        } else {
            accountName
        }
    }

    private fun NotificationBuilder.setInboxStyle(
        title: String,
        summary: String,
        contentLines: List<CharSequence>
    ) = apply {
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summary)

        for (line in contentLines) {
            style.addLine(line)
        }

        setStyle(style)
    }

    private fun createViewIntent(account: Account, notificationData: SummaryInboxNotificationData): PendingIntent {
        return actionCreator.createViewMessagesPendingIntent(
            account = account,
            messageReferences = notificationData.messageReferences,
            notificationId = notificationData.notificationId
        )
    }

    private fun createDismissIntent(account: Account, notificationId: Int): PendingIntent {
        return actionCreator.createDismissAllMessagesPendingIntent(account, notificationId)
    }

    private fun NotificationBuilder.setDeviceActions(
        account: Account,
        notificationData: SummaryInboxNotificationData
    ) = apply {
        for (action in notificationData.actions) {
            when (action) {
                SummaryNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                SummaryNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
            }
        }
    }

    private fun NotificationBuilder.addMarkAllAsReadAction(
        account: Account,
        notificationData: SummaryInboxNotificationData
    ) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReferences = notificationData.messageReferences
        val notificationId = notificationData.notificationId
        val markAllAsReadPendingIntent =
            actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId)

        addAction(icon, title, markAllAsReadPendingIntent)
    }

    private fun NotificationBuilder.addDeleteAllAction(
        account: Account,
        notificationData: SummaryInboxNotificationData
    ) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val notificationId = getNewMailSummaryNotificationId(account)
        val messageReferences = notificationData.messageReferences
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId)

        addAction(icon, title, action)
    }

    private fun NotificationBuilder.setWearActions(
        account: Account,
        notificationData: SummaryInboxNotificationData
    ) = apply {
        val wearableExtender = WearableExtender().apply {
            for (action in notificationData.wearActions) {
                when (action) {
                    SummaryWearNotificationAction.MarkAsRead -> addMarkAllAsReadAction(account, notificationData)
                    SummaryWearNotificationAction.Delete -> addDeleteAllAction(account, notificationData)
                    SummaryWearNotificationAction.Archive -> addArchiveAllAction(account, notificationData)
                }
            }
        }

        extend(wearableExtender)
    }

    private fun WearableExtender.addMarkAllAsReadAction(
        account: Account,
        notificationData: SummaryInboxNotificationData
    ) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAllAsRead()
        val messageReferences = notificationData.messageReferences
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun WearableExtender.addDeleteAllAction(account: Account, notificationData: SummaryInboxNotificationData) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDeleteAll()
        val messageReferences = notificationData.messageReferences
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun WearableExtender.addArchiveAllAction(account: Account, notificationData: SummaryInboxNotificationData) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchiveAll()
        val messageReferences = notificationData.messageReferences
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }

    private fun NotificationBuilder.setLockScreenNotification(notificationData: BaseNotificationData) = apply {
        lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
    }
}
