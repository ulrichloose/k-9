package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference

/**
 * Holds information about active and inactive new message notifications of an account.
 */
internal data class NotificationData(
    val account: Account,
    val activeNotifications: List<NotificationHolder>,
    val inactiveNotifications: List<InactiveNotificationHolder>
) {
    val newMessagesCount: Int
        get() = activeNotifications.size + inactiveNotifications.size

    val isSingleMessageNotification: Boolean
        get() = activeNotifications.size == 1

    @OptIn(ExperimentalStdlibApi::class)
    val messageReferences: List<MessageReference>
        get() {
            return buildList(capacity = newMessagesCount) {
                for (activeNotification in activeNotifications) {
                    add(activeNotification.content.messageReference)
                }
                for (inactiveNotification in inactiveNotifications) {
                    add(inactiveNotification.content.messageReference)
                }
            }
        }

    fun isEmpty() = activeNotifications.isEmpty()

    companion object {
        fun create(account: Account): NotificationData {
            return NotificationData(account, activeNotifications = emptyList(), inactiveNotifications = emptyList())
        }
    }
}
