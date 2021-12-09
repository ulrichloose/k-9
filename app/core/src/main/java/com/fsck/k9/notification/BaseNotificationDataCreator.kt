package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility

private const val MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5

internal class BaseNotificationDataCreator {

    fun createBaseNotificationData(notificationData: NotificationData): BaseNotificationData {
        val account = notificationData.account
        return BaseNotificationData(
            account = account,
            groupKey = NotificationGroupKeys.getGroupKey(account),
            accountName = getAccountName(account),
            color = account.chipColor,
            newMessagesCount = notificationData.newMessagesCount,
            lockScreenNotificationData = createLockScreenNotificationData(notificationData),
            appearance = createNotificationAppearance(account)
        )
    }

    private fun getAccountName(account: Account): String {
        val accountDescription = account.description?.takeIf { it.isNotEmpty() }
        return accountDescription ?: account.email
    }

    private fun createLockScreenNotificationData(data: NotificationData): LockScreenNotificationData {
        return when (K9.lockScreenNotificationVisibility) {
            LockScreenNotificationVisibility.NOTHING -> LockScreenNotificationData.None
            LockScreenNotificationVisibility.APP_NAME -> LockScreenNotificationData.AppName
            LockScreenNotificationVisibility.EVERYTHING -> LockScreenNotificationData.Public
            LockScreenNotificationVisibility.MESSAGE_COUNT -> LockScreenNotificationData.MessageCount
            LockScreenNotificationVisibility.SENDERS -> LockScreenNotificationData.SenderNames(getSenderNames(data))
        }
    }

    private fun getSenderNames(data: NotificationData): String {
        return data.activeNotifications.asSequence()
            .map { it.content.sender }
            .distinct()
            .take(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION)
            .joinToString()
    }

    private fun createNotificationAppearance(account: Account): NotificationAppearance {
        return with(account.notificationSetting) {
            val vibrationPattern = if (isVibrateEnabled) vibration else null
            NotificationAppearance(ringtone, vibrationPattern, ledColor)
        }
    }
}
