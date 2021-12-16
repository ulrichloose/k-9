package com.fsck.k9.ui.settings.account

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.notification.NotificationChannelManager
import java.util.concurrent.ExecutorService

class AccountSettingsDataStoreFactory(
    private val preferences: Preferences,
    private val jobManager: K9JobManager,
    private val executorService: ExecutorService,
    private val notificationChannelManager: NotificationChannelManager
) {
    fun create(account: Account): AccountSettingsDataStore {
        return AccountSettingsDataStore(preferences, executorService, account, jobManager, notificationChannelManager)
    }
}
