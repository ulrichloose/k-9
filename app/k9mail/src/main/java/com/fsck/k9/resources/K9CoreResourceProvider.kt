package com.fsck.k9.resources

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.R
import com.fsck.k9.notification.PushNotificationState

class K9CoreResourceProvider(private val context: Context) : CoreResourceProvider {
    override fun defaultSignature(): String = context.getString(R.string.default_signature)
    override fun defaultIdentityDescription(): String = context.getString(R.string.default_identity_description)

    override fun contactDisplayNamePrefix(): String = context.getString(R.string.message_to_label)
    override fun contactUnknownSender(): String = context.getString(R.string.unknown_sender)
    override fun contactUnknownRecipient(): String = context.getString(R.string.unknown_recipient)

    override fun messageHeaderFrom(): String = context.getString(R.string.message_compose_quote_header_from)
    override fun messageHeaderTo(): String = context.getString(R.string.message_compose_quote_header_to)
    override fun messageHeaderCc(): String = context.getString(R.string.message_compose_quote_header_cc)
    override fun messageHeaderDate(): String = context.getString(R.string.message_compose_quote_header_send_date)
    override fun messageHeaderSubject(): String = context.getString(R.string.message_compose_quote_header_subject)
    override fun messageHeaderSeparator(): String = context.getString(R.string.message_compose_quote_header_separator)

    override fun noSubject(): String = context.getString(R.string.general_no_subject)

    override fun userAgent(): String = context.getString(R.string.message_header_mua)
    override fun encryptedSubject(): String = context.getString(R.string.encrypted_subject)

    override fun replyHeader(sender: String): String =
        context.getString(R.string.message_compose_reply_header_fmt, sender)

    override fun replyHeader(sender: String, sentDate: String): String =
        context.getString(R.string.message_compose_reply_header_fmt_with_date, sentDate, sender)

    override fun searchAllMessagesTitle(): String = context.getString(R.string.search_all_messages_title)
    override fun searchAllMessagesDetail(): String = context.getString(R.string.search_all_messages_detail)
    override fun searchUnifiedInboxTitle(): String = context.getString(R.string.integrated_inbox_title)
    override fun searchUnifiedInboxDetail(): String = context.getString(R.string.integrated_inbox_detail)

    override fun outboxFolderName(): String = context.getString(R.string.special_mailbox_name_outbox)

    override val iconPushNotification: Int = R.drawable.ic_push_notification

    override fun pushNotificationText(notificationState: PushNotificationState): String {
        val resId = when (notificationState) {
            PushNotificationState.INITIALIZING -> R.string.push_notification_state_initializing
            PushNotificationState.LISTENING -> R.string.push_notification_state_listening
            PushNotificationState.WAIT_BACKGROUND_SYNC -> R.string.push_notification_state_wait_background_sync
            PushNotificationState.WAIT_NETWORK -> R.string.push_notification_state_wait_network
        }
        return context.getString(resId)
    }

    override fun pushNotificationInfoText(): String = context.getString(R.string.push_notification_info)
}
