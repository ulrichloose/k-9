package com.fsck.k9.backend.imap

import app.k9mail.backend.testing.InMemoryBackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.buildMessage
import com.google.common.truth.Truth.assertThat
import java.util.Date
import org.apache.james.mime4j.dom.field.DateTimeField
import org.apache.james.mime4j.field.DefaultFieldParser
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

private const val ACCOUNT_NAME = "Account-1"
private const val FOLDER_SERVER_ID = "FOLDER_ONE"
private const val MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE = 1000
private const val DEFAULT_VISIBLE_LIMIT = 25
private const val DEFAULT_MESSAGE_DATE = "Tue, 04 Jan 2022 10:00:00 +0100"

class ImapSyncTest {
    private val backendStorage = createBackendStorage()
    private val backendFolder = backendStorage.getFolder(FOLDER_SERVER_ID)
    private val imapStore = TestImapStore()
    private val imapFolder = imapStore.addFolder(FOLDER_SERVER_ID)
    private val imapSync = ImapSync(ACCOUNT_NAME, backendStorage, imapStore)
    private val syncListener = mock<SyncListener>()
    private val defaultSyncConfig = createSyncConfig()

    @Test
    fun `sync of empty folder should notify listener`() {
        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        verify(syncListener).syncStarted(FOLDER_SERVER_ID)
        verify(syncListener).syncAuthenticationSuccess()
        verify(syncListener).syncFinished(FOLDER_SERVER_ID)
        verify(syncListener, never()).syncFailed(folderServerId = any(), message = any(), exception = any())
    }

    @Test
    fun `sync of folder with negative messageCount should return an error`() {
        imapFolder.messageCount = -1

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        verify(syncListener).syncFailed(
            folderServerId = eq(FOLDER_SERVER_ID),
            message = eq("Exception: Message count -1 for folder $FOLDER_SERVER_ID"),
            exception = any()
        )
    }

    @Test
    fun `successful sync should close folder`() {
        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        assertThat(imapFolder.isClosed).isTrue()
    }

    @Test
    fun `sync with error should close folder`() {
        imapFolder.messageCount = -1

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        assertThat(imapFolder.isClosed).isTrue()
    }

    @Test
    fun `sync with ExpungePolicy ON_POLL should expunge remote folder`() {
        val syncConfig = defaultSyncConfig.copy(expungePolicy = ExpungePolicy.ON_POLL)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(imapFolder.wasExpunged).isTrue()
    }

    @Test
    fun `sync with ExpungePolicy MANUALLY should not expunge remote folder`() {
        val syncConfig = defaultSyncConfig.copy(expungePolicy = ExpungePolicy.MANUALLY)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(imapFolder.wasExpunged).isFalse()
    }

    @Test
    fun `sync with ExpungePolicy IMMEDIATELY should not expunge remote folder`() {
        val syncConfig = defaultSyncConfig.copy(expungePolicy = ExpungePolicy.IMMEDIATELY)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(imapFolder.wasExpunged).isFalse()
    }

    @Test
    fun `sync with syncRemoteDeletions=true should remove local messages`() {
        addMessageToBackendFolder(uid = 42)
        val syncConfig = defaultSyncConfig.copy(syncRemoteDeletions = true)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).isEmpty()
        verify(syncListener).syncStarted(FOLDER_SERVER_ID)
        verify(syncListener).syncFinished(FOLDER_SERVER_ID)
    }

    @Test
    fun `sync with syncRemoteDeletions=false should not remove local messages`() {
        addMessageToBackendFolder(uid = 23)
        val syncConfig = defaultSyncConfig.copy(syncRemoteDeletions = false)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("23")
        verify(syncListener).syncStarted(FOLDER_SERVER_ID)
        verify(syncListener).syncFinished(FOLDER_SERVER_ID)
    }

    @Test
    fun `sync should remove messages older than earliestPollDate`() {
        addMessageToImapAndBackendFolder(uid = 23, date = "Mon, 03 Jan 2022 10:00:00 +0100")
        addMessageToImapAndBackendFolder(uid = 42, date = "Wed, 05 Jan 2022 20:00:00 +0100")
        val syncConfig = defaultSyncConfig.copy(
            syncRemoteDeletions = true,
            earliestPollDate = "Tue, 04 Jan 2022 12:00:00 +0100".toDate()
        )

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("42")
    }

    @Test
    fun `sync with new messages on server should download messages`() {
        addMessageToImapFolder(uid = 9)
        addMessageToImapFolder(uid = 13)

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("9", "13")
        verify(syncListener).syncNewMessage(FOLDER_SERVER_ID, messageServerId = "9", isOldMessage = false)
        verify(syncListener).syncNewMessage(FOLDER_SERVER_ID, messageServerId = "13", isOldMessage = false)
    }

    @Test
    fun `sync downloading old messages should notify listener with isOldMessage=true`() {
        addMessageToBackendFolder(uid = 42)
        addMessageToImapFolder(uid = 23)
        addMessageToImapFolder(uid = 42)

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("23", "42")
        verify(syncListener).syncNewMessage(FOLDER_SERVER_ID, messageServerId = "23", isOldMessage = true)
    }

    @Test
    fun `determining the highest UID should use numerical ordering`() {
        addMessageToBackendFolder(uid = 9)
        addMessageToBackendFolder(uid = 100)
        // When text ordering is used: "9" > "100" -> highest UID = 9 (when it should be 100)
        // With 80 > 9 the message on the server is considered a new message, but it shouldn't be (80 < 100)
        addMessageToImapFolder(uid = 80)

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        verify(syncListener).syncNewMessage(FOLDER_SERVER_ID, messageServerId = "80", isOldMessage = true)
    }

    @Test
    fun `sync should update flags of existing messages`() {
        addMessageToBackendFolder(uid = 2)
        addMessageToImapFolder(uid = 2, flags = setOf(Flag.SEEN, Flag.ANSWERED))

        imapSync.sync(FOLDER_SERVER_ID, defaultSyncConfig, syncListener)

        assertThat(backendFolder.getMessageFlags(messageServerId = "2")).containsAtLeast(Flag.SEEN, Flag.ANSWERED)
    }

    @Test
    fun `sync with UIDVALIDITY change should clear all messages`() {
        imapFolder.setUidValidity(1)
        addMessageToImapFolder(uid = 300)
        addMessageToImapFolder(uid = 301)
        val syncConfig = defaultSyncConfig.copy(syncRemoteDeletions = false)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("300", "301")

        imapFolder.setUidValidity(9000)
        imapFolder.removeAllMessages()
        addMessageToImapFolder(uid = 1)

        imapSync.sync(FOLDER_SERVER_ID, syncConfig, syncListener)

        assertThat(backendFolder.getMessageServerIds()).containsExactly("1")
        verify(syncListener).syncNewMessage(FOLDER_SERVER_ID, messageServerId = "1", isOldMessage = false)
    }

    private fun addMessageToBackendFolder(uid: Long, date: String = DEFAULT_MESSAGE_DATE) {
        val messageServerId = uid.toString()
        val message = createSimpleMessage(messageServerId, date).apply {
            setUid(messageServerId)
        }
        backendFolder.saveMessage(message, MessageDownloadState.FULL)

        val highestKnownUid = backendFolder.getFolderExtraNumber("imapHighestKnownUid") ?: 0
        if (uid > highestKnownUid) {
            backendFolder.setFolderExtraNumber("imapHighestKnownUid", uid)
        }
    }

    private fun addMessageToImapFolder(uid: Long, flags: Set<Flag> = emptySet(), date: String = DEFAULT_MESSAGE_DATE) {
        val messageServerId = uid.toString()
        val message = createSimpleMessage(messageServerId, date)
        imapFolder.addMessage(uid, message)

        if (flags.isNotEmpty()) {
            val imapMessage = imapFolder.getMessage(messageServerId)
            imapFolder.setFlags(listOf(imapMessage), flags, true)
        }
    }

    private fun addMessageToImapAndBackendFolder(uid: Long, date: String) {
        addMessageToBackendFolder(uid, date)
        addMessageToImapFolder(uid, date = date)
    }

    private fun createBackendStorage(): InMemoryBackendStorage {
        return InMemoryBackendStorage().apply {
            createFolderUpdater().use { updater ->
                val folderInfo = FolderInfo(
                    serverId = FOLDER_SERVER_ID,
                    name = "irrelevant",
                    type = FolderType.REGULAR
                )
                updater.createFolders(listOf(folderInfo))
            }
        }
    }

    private fun createSyncConfig(): SyncConfig {
        return SyncConfig(
            expungePolicy = ExpungePolicy.MANUALLY,
            earliestPollDate = null,
            syncRemoteDeletions = true,
            maximumAutoDownloadMessageSize = MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE,
            defaultVisibleLimit = DEFAULT_VISIBLE_LIMIT,
            syncFlags = setOf(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED)
        )
    }

    private fun createSimpleMessage(uid: String, date: String, text: String = "UID: $uid"): Message {
        return buildMessage {
            header("Subject", "Test Message")
            header("From", "alice@domain.example")
            header("To", "Bob <bob@domain.example>")
            header("Date", date)
            header("Message-ID", "<msg-$uid@domain.example>")

            textBody(text)
        }
    }
}

private fun String.toDate(): Date {
    val dateTimeField = DefaultFieldParser.parse("Date: $this") as DateTimeField
    return dateTimeField.date
}
