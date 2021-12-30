package com.fsck.k9.ui.settings.general

import androidx.preference.PreferenceDataStore
import com.fsck.k9.K9
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.preferences.AppTheme
import com.fsck.k9.preferences.GeneralSettingsManager
import com.fsck.k9.preferences.SubTheme
import com.fsck.k9.ui.base.AppLanguageManager

class GeneralSettingsDataStore(
    private val jobManager: K9JobManager,
    private val appLanguageManager: AppLanguageManager,
    private val generalSettingsManager: GeneralSettingsManager
) : PreferenceDataStore() {

    private var skipSaveSettings = false

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return when (key) {
            "fixed_message_view_theme" -> generalSettingsManager.getSettings().fixedMessageViewTheme
            "animations" -> K9.isShowAnimations
            "show_unified_inbox" -> K9.isShowUnifiedInbox
            "show_starred_count" -> K9.isShowStarredCount
            "messagelist_stars" -> K9.isShowMessageListStars
            "messagelist_show_correspondent_names" -> K9.isShowCorrespondentNames
            "messagelist_sender_above_subject" -> K9.isMessageListSenderAboveSubject
            "messagelist_show_contact_name" -> K9.isShowContactName
            "messagelist_change_contact_name_color" -> K9.isChangeContactNameColor
            "messagelist_show_contact_picture" -> K9.isShowContactPicture
            "messagelist_colorize_missing_contact_pictures" -> K9.isColorizeMissingContactPictures
            "messagelist_background_as_unread_indicator" -> K9.isUseBackgroundAsUnreadIndicator
            "threaded_view" -> K9.isThreadedViewEnabled
            "messageview_fixedwidth_font" -> K9.isUseMessageViewFixedWidthFont
            "messageview_autofit_width" -> K9.isAutoFitWidth
            "messageview_return_to_list" -> K9.isMessageViewReturnToList
            "messageview_show_next" -> K9.isMessageViewShowNext
            "quiet_time_enabled" -> K9.isQuietTimeEnabled
            "disable_notifications_during_quiet_time" -> !K9.isNotificationDuringQuietTimeEnabled
            "privacy_hide_useragent" -> K9.isHideUserAgent
            "privacy_hide_timezone" -> K9.isHideTimeZone
            "debug_logging" -> K9.isDebugLoggingEnabled
            "sensitive_logging" -> K9.isSensitiveDebugLoggingEnabled
            else -> defValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        when (key) {
            "fixed_message_view_theme" -> setFixedMessageViewTheme(value)
            "animations" -> K9.isShowAnimations = value
            "show_unified_inbox" -> K9.isShowUnifiedInbox = value
            "show_starred_count" -> K9.isShowStarredCount = value
            "messagelist_stars" -> K9.isShowMessageListStars = value
            "messagelist_show_correspondent_names" -> K9.isShowCorrespondentNames = value
            "messagelist_sender_above_subject" -> K9.isMessageListSenderAboveSubject = value
            "messagelist_show_contact_name" -> K9.isShowContactName = value
            "messagelist_change_contact_name_color" -> K9.isChangeContactNameColor = value
            "messagelist_show_contact_picture" -> K9.isShowContactPicture = value
            "messagelist_colorize_missing_contact_pictures" -> K9.isColorizeMissingContactPictures = value
            "messagelist_background_as_unread_indicator" -> K9.isUseBackgroundAsUnreadIndicator = value
            "threaded_view" -> K9.isThreadedViewEnabled = value
            "messageview_fixedwidth_font" -> K9.isUseMessageViewFixedWidthFont = value
            "messageview_autofit_width" -> K9.isAutoFitWidth = value
            "messageview_return_to_list" -> K9.isMessageViewReturnToList = value
            "messageview_show_next" -> K9.isMessageViewShowNext = value
            "quiet_time_enabled" -> K9.isQuietTimeEnabled = value
            "disable_notifications_during_quiet_time" -> K9.isNotificationDuringQuietTimeEnabled = !value
            "privacy_hide_useragent" -> K9.isHideUserAgent = value
            "privacy_hide_timezone" -> K9.isHideTimeZone = value
            "debug_logging" -> K9.isDebugLoggingEnabled = value
            "sensitive_logging" -> K9.isSensitiveDebugLoggingEnabled = value
            else -> return
        }

        saveSettings()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return when (key) {
            "messagelist_contact_name_color" -> K9.contactNameColor
            "message_view_content_font_slider" -> K9.fontSizes.messageViewContentAsPercent
            else -> defValue
        }
    }

    override fun putInt(key: String?, value: Int) {
        when (key) {
            "messagelist_contact_name_color" -> K9.contactNameColor = value
            "message_view_content_font_slider" -> K9.fontSizes.messageViewContentAsPercent = value
            else -> return
        }

        saveSettings()
    }

    override fun getString(key: String, defValue: String?): String? {
        return when (key) {
            "language" -> appLanguageManager.getAppLanguage()
            "theme" -> appThemeToString(generalSettingsManager.getSettings().appTheme)
            "message_compose_theme" -> subThemeToString(generalSettingsManager.getSettings().messageComposeTheme)
            "messageViewTheme" -> subThemeToString(generalSettingsManager.getSettings().messageViewTheme)
            "messagelist_preview_lines" -> K9.messageListPreviewLines.toString()
            "splitview_mode" -> K9.splitViewMode.name
            "notification_quick_delete" -> K9.notificationQuickDeleteBehaviour.name
            "lock_screen_notification_visibility" -> K9.lockScreenNotificationVisibility.name
            "background_ops" -> K9.backgroundOps.name
            "quiet_time_starts" -> K9.quietTimeStarts
            "quiet_time_ends" -> K9.quietTimeEnds
            "account_name_font" -> K9.fontSizes.accountName.toString()
            "account_description_font" -> K9.fontSizes.accountDescription.toString()
            "folder_name_font" -> K9.fontSizes.folderName.toString()
            "folder_status_font" -> K9.fontSizes.folderStatus.toString()
            "message_list_subject_font" -> K9.fontSizes.messageListSubject.toString()
            "message_list_sender_font" -> K9.fontSizes.messageListSender.toString()
            "message_list_date_font" -> K9.fontSizes.messageListDate.toString()
            "message_list_preview_font" -> K9.fontSizes.messageListPreview.toString()
            "message_view_sender_font" -> K9.fontSizes.messageViewSender.toString()
            "message_view_to_font" -> K9.fontSizes.messageViewTo.toString()
            "message_view_cc_font" -> K9.fontSizes.messageViewCC.toString()
            "message_view_bcc_font" -> K9.fontSizes.messageViewBCC.toString()
            "message_view_subject_font" -> K9.fontSizes.messageViewSubject.toString()
            "message_view_date_font" -> K9.fontSizes.messageViewDate.toString()
            "message_view_additional_headers_font" -> K9.fontSizes.messageViewAdditionalHeaders.toString()
            "message_compose_input_font" -> K9.fontSizes.messageComposeInput.toString()
            else -> defValue
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) return

        when (key) {
            "language" -> appLanguageManager.setAppLanguage(value)
            "theme" -> setTheme(value)
            "message_compose_theme" -> setMessageComposeTheme(value)
            "messageViewTheme" -> setMessageViewTheme(value)
            "messagelist_preview_lines" -> K9.messageListPreviewLines = value.toInt()
            "splitview_mode" -> K9.splitViewMode = K9.SplitViewMode.valueOf(value)
            "notification_quick_delete" -> {
                K9.notificationQuickDeleteBehaviour = K9.NotificationQuickDelete.valueOf(value)
            }
            "lock_screen_notification_visibility" -> {
                K9.lockScreenNotificationVisibility = K9.LockScreenNotificationVisibility.valueOf(value)
            }
            "background_ops" -> setBackgroundOps(value)
            "quiet_time_starts" -> K9.quietTimeStarts = value
            "quiet_time_ends" -> K9.quietTimeEnds = value
            "account_name_font" -> K9.fontSizes.accountName = value.toInt()
            "account_description_font" -> K9.fontSizes.accountDescription = value.toInt()
            "folder_name_font" -> K9.fontSizes.folderName = value.toInt()
            "folder_status_font" -> K9.fontSizes.folderStatus = value.toInt()
            "message_list_subject_font" -> K9.fontSizes.messageListSubject = value.toInt()
            "message_list_sender_font" -> K9.fontSizes.messageListSender = value.toInt()
            "message_list_date_font" -> K9.fontSizes.messageListDate = value.toInt()
            "message_list_preview_font" -> K9.fontSizes.messageListPreview = value.toInt()
            "message_view_sender_font" -> K9.fontSizes.messageViewSender = value.toInt()
            "message_view_to_font" -> K9.fontSizes.messageViewTo = value.toInt()
            "message_view_cc_font" -> K9.fontSizes.messageViewCC = value.toInt()
            "message_view_bcc_font" -> K9.fontSizes.messageViewBCC = value.toInt()
            "message_view_subject_font" -> K9.fontSizes.messageViewSubject = value.toInt()
            "message_view_date_font" -> K9.fontSizes.messageViewDate = value.toInt()
            "message_view_additional_headers_font" -> K9.fontSizes.messageViewAdditionalHeaders = value.toInt()
            "message_compose_input_font" -> K9.fontSizes.messageComposeInput = value.toInt()
            else -> return
        }

        saveSettings()
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return when (key) {
            "confirm_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.isConfirmDelete) add("delete")
                    if (K9.isConfirmDeleteStarred) add("delete_starred")
                    if (K9.isConfirmDeleteFromNotification) add("delete_notif")
                    if (K9.isConfirmSpam) add("spam")
                    if (K9.isConfirmDiscardMessage) add("discard")
                    if (K9.isConfirmMarkAllRead) add("mark_all_read")
                }
            }
            "messageview_visible_refile_actions" -> {
                mutableSetOf<String>().apply {
                    if (K9.isMessageViewDeleteActionVisible) add("delete")
                    if (K9.isMessageViewArchiveActionVisible) add("archive")
                    if (K9.isMessageViewMoveActionVisible) add("move")
                    if (K9.isMessageViewCopyActionVisible) add("copy")
                    if (K9.isMessageViewSpamActionVisible) add("spam")
                }
            }
            "volume_navigation" -> {
                mutableSetOf<String>().apply {
                    if (K9.isUseVolumeKeysForNavigation) add("message")
                    if (K9.isUseVolumeKeysForListNavigation) add("list")
                }
            }
            else -> defValues
        }
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        val checkedValues = values ?: emptySet<String>()
        when (key) {
            "confirm_actions" -> {
                K9.isConfirmDelete = "delete" in checkedValues
                K9.isConfirmDeleteStarred = "delete_starred" in checkedValues
                K9.isConfirmDeleteFromNotification = "delete_notif" in checkedValues
                K9.isConfirmSpam = "spam" in checkedValues
                K9.isConfirmDiscardMessage = "discard" in checkedValues
                K9.isConfirmMarkAllRead = "mark_all_read" in checkedValues
            }
            "messageview_visible_refile_actions" -> {
                K9.isMessageViewDeleteActionVisible = "delete" in checkedValues
                K9.isMessageViewArchiveActionVisible = "archive" in checkedValues
                K9.isMessageViewMoveActionVisible = "move" in checkedValues
                K9.isMessageViewCopyActionVisible = "copy" in checkedValues
                K9.isMessageViewSpamActionVisible = "spam" in checkedValues
            }
            "volume_navigation" -> {
                K9.isUseVolumeKeysForNavigation = "message" in checkedValues
                K9.isUseVolumeKeysForListNavigation = "list" in checkedValues
            }
            else -> return
        }

        saveSettings()
    }

    private fun saveSettings() {
        if (skipSaveSettings) {
            skipSaveSettings = false
        } else {
            K9.saveSettingsAsync()
        }
    }

    private fun setTheme(value: String) {
        skipSaveSettings = true
        generalSettingsManager.setAppTheme(stringToAppTheme(value))
    }

    private fun setMessageComposeTheme(subThemeString: String) {
        skipSaveSettings = true
        generalSettingsManager.setMessageComposeTheme(stringToSubTheme(subThemeString))
    }

    private fun setMessageViewTheme(subThemeString: String) {
        skipSaveSettings = true
        generalSettingsManager.setMessageViewTheme(stringToSubTheme(subThemeString))
    }

    private fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean) {
        skipSaveSettings = true
        generalSettingsManager.setFixedMessageViewTheme(fixedMessageViewTheme)
    }

    private fun appThemeToString(theme: AppTheme) = when (theme) {
        AppTheme.LIGHT -> "light"
        AppTheme.DARK -> "dark"
        AppTheme.FOLLOW_SYSTEM -> "follow_system"
    }

    private fun subThemeToString(theme: SubTheme) = when (theme) {
        SubTheme.LIGHT -> "light"
        SubTheme.DARK -> "dark"
        SubTheme.USE_GLOBAL -> "global"
    }

    private fun stringToAppTheme(theme: String?) = when (theme) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "follow_system" -> AppTheme.FOLLOW_SYSTEM
        else -> throw AssertionError()
    }

    private fun stringToSubTheme(theme: String?) = when (theme) {
        "light" -> SubTheme.LIGHT
        "dark" -> SubTheme.DARK
        "global" -> SubTheme.USE_GLOBAL
        else -> throw AssertionError()
    }

    private fun setBackgroundOps(value: String) {
        val newBackgroundOps = K9.BACKGROUND_OPS.valueOf(value)
        if (newBackgroundOps != K9.backgroundOps) {
            K9.backgroundOps = newBackgroundOps
            jobManager.scheduleAllMailJobs()
        }
    }
}
