package com.fsck.k9.controller.push

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.FolderRepositoryManager

internal class AccountPushControllerFactory(
    private val backendManager: BackendManager,
    private val messagingController: MessagingController,
    private val folderRepositoryManager: FolderRepositoryManager,
    private val preferences: Preferences
) {
    fun create(account: Account): AccountPushController {
        return AccountPushController(
            backendManager,
            messagingController,
            preferences,
            folderRepositoryManager,
            account = account
        )
    }
}
