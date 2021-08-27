package com.fsck.k9.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.compose.MessageActions
import com.fsck.k9.activity.setup.AccountSetupIncoming
import com.fsck.k9.activity.setup.AccountSetupOutgoing
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.ui.messagelist.DefaultFolderProvider
import com.fsck.k9.ui.notification.DeleteConfirmationActivity

/**
 * This class contains methods to create the [PendingIntent]s for the actions of our notifications.
 *
 * **Note:**
 * We need to take special care to ensure the `PendingIntent`s are unique as defined in the documentation of
 * [PendingIntent]. Otherwise selecting a notification action might perform the action on the wrong message.
 *
 * We use the notification ID as `requestCode` argument to ensure each notification/action pair gets a unique
 * `PendingIntent`.
 */
internal class K9NotificationActionCreator(
    private val context: Context,
    private val defaultFolderProvider: DefaultFolderProvider
) : NotificationActionCreator {

    override fun createViewMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = createMessageViewIntent(messageReference)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createViewFolderPendingIntent(account: Account, folderId: Long, notificationId: Int): PendingIntent {
        val intent = createMessageListIntent(account, folderId)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createViewMessagesPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val folderServerId = getFolderIdOfAllMessages(messageReferences)
        val intent = if (folderServerId != null) {
            createMessageListIntent(account, folderServerId)
        } else {
            createMessageListIntent(account)
        }

        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createViewFolderListPendingIntent(account: Account, notificationId: Int): PendingIntent {
        val intent = createMessageListIntent(account)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createDismissAllMessagesPendingIntent(account: Account, notificationId: Int): PendingIntent {
        val intent = NotificationActionService.createDismissAllMessagesIntent(context, account)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createDismissMessagePendingIntent(
        context: Context,
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createDismissMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createReplyPendingIntent(messageReference: MessageReference, notificationId: Int): PendingIntent {
        val intent = MessageActions.getActionReplyIntent(context, messageReference)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createMarkMessageAsReadPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createMarkAllAsReadPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent = NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun getEditIncomingServerSettingsIntent(account: Account): PendingIntent {
        val intent = AccountSetupIncoming.intentActionEditIncomingSettings(context, account)
        return PendingIntent.getActivity(context, account.accountNumber, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun getEditOutgoingServerSettingsIntent(account: Account): PendingIntent {
        val intent = AccountSetupOutgoing.intentActionEditOutgoingSettings(context, account)
        return PendingIntent.getActivity(context, account.accountNumber, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createDeleteMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        return if (K9.isConfirmDeleteFromNotification) {
            createDeleteConfirmationPendingIntent(messageReference, notificationId)
        } else {
            createDeleteServicePendingIntent(messageReference, notificationId)
        }
    }

    private fun createDeleteServicePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createDeleteMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createDeleteConfirmationPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReference)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createDeleteAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        return if (K9.isConfirmDeleteFromNotification) {
            getDeleteAllConfirmationPendingIntent(messageReferences, notificationId)
        } else {
            getDeleteAllServicePendingIntent(account, messageReferences, notificationId)
        }
    }

    private fun getDeleteAllConfirmationPendingIntent(
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val intent = DeleteConfirmationActivity.getIntent(context, messageReferences)
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getDeleteAllServicePendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val accountUuid = account.uuid
        val intent = NotificationActionService.createDeleteAllMessagesIntent(context, accountUuid, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createArchiveMessagePendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveMessageIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createArchiveAllPendingIntent(
        account: Account,
        messageReferences: List<MessageReference>,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun createMarkMessageAsSpamPendingIntent(
        messageReference: MessageReference,
        notificationId: Int
    ): PendingIntent {
        val intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference)
        return PendingIntent.getService(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createMessageListIntent(account: Account): Intent {
        val folderId = defaultFolderProvider.getDefaultFolder(account)
        val search = LocalSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MessageList.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true
        )
    }

    private fun createMessageListIntent(account: Account, folderId: Long): Intent {
        val search = LocalSearch().apply {
            addAllowedFolder(folderId)
            addAccountUuid(account.uuid)
        }

        return MessageList.intentDisplaySearch(
            context = context,
            search = search,
            noThreading = false,
            newTask = true,
            clearTop = true
        )
    }

    private fun createMessageViewIntent(message: MessageReference): Intent {
        return MessageList.actionDisplayMessageIntent(context, message)
    }

    private fun getFolderIdOfAllMessages(messageReferences: List<MessageReference>): Long? {
        val firstMessage = messageReferences.first()
        val folderId = firstMessage.folderId

        return if (messageReferences.all { it.folderId == folderId }) folderId else null
    }
}
