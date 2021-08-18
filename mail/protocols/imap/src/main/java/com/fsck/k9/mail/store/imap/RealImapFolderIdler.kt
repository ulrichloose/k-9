package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.power.WakeLock
import java.io.IOException
import timber.log.Timber

private const val SOCKET_EXTRA_TIMEOUT_MS = 2 * 60 * 1000L

internal class RealImapFolderIdler(
    private val idleRefreshManager: IdleRefreshManager,
    private val wakeLock: WakeLock,
    private val imapStore: ImapStore,
    private val connectionProvider: ImapConnectionProvider,
    private val folderServerId: String,
    private val idleRefreshTimeoutProvider: IdleRefreshTimeoutProvider
) : ImapFolderIdler {
    private val logTag = "ImapFolderIdler[$folderServerId]"

    private var folder: ImapFolder? = null

    @get:Synchronized
    @set:Synchronized
    private var idleRefreshTimer: IdleRefreshTimer? = null

    @Volatile
    private var stopIdle = false

    private var idleSent = false
    private var doneSent = false

    override fun idle(): IdleResult {
        Timber.v("%s.idle()", logTag)

        val folder = imapStore.getFolder(folderServerId).also { this.folder = it }
        folder.open(OpenMode.READ_ONLY)

        try {
            return folder.idle().also { idleResult ->
                Timber.v("%s.idle(): result=%s", logTag, idleResult)
            }
        } finally {
            folder.close()
        }
    }

    @Synchronized
    override fun refresh() {
        Timber.v("%s.refresh()", logTag)
        endIdle()
    }

    @Synchronized
    override fun stop() {
        Timber.v("%s.stop()", logTag)
        stopIdle = true
        endIdle()
    }

    private fun endIdle() {
        if (idleSent && !doneSent) {
            idleRefreshTimer?.cancel()

            try {
                sendDone()
            } catch (e: IOException) {
                Timber.v(e, "%s: IOException while sending DONE", logTag)
            }
        }
    }

    private fun ImapFolder.idle(): IdleResult {
        var result = IdleResult.STOPPED

        val connection = connectionProvider.getConnection(this)!!
        if (!connection.isIdleCapable) {
            Timber.w("%s: IDLE not supported by server", logTag)
            return IdleResult.NOT_SUPPORTED
        }

        stopIdle = false
        do {
            synchronized(this) {
                idleSent = false
                doneSent = false
            }

            val tag = connection.sendCommand("IDLE", false)

            synchronized(this) {
                idleSent = true
            }

            var receivedRelevantResponse = false
            do {
                val response = connection.readResponse()
                if (response.tag == tag) {
                    Timber.w("%s.idle(): IDLE command completed without a continuation request response", logTag)
                    return IdleResult.NOT_SUPPORTED
                } else if (response.isRelevant) {
                    receivedRelevantResponse = true
                }
            } while (!response.isContinuationRequested)

            if (receivedRelevantResponse) {
                Timber.v("%s.idle(): Received a relevant untagged response right after sending IDLE command", logTag)
                result = IdleResult.SYNC
                stopIdle = true
                sendDone()
            } else {
                connection.setSocketIdleReadTimeout()
            }

            var response: ImapResponse
            do {
                idleRefreshTimer = idleRefreshManager.startTimer(
                    timeout = idleRefreshTimeoutProvider.idleRefreshTimeoutMs,
                    callback = ::idleRefresh
                )

                wakeLock.release()

                try {
                    response = connection.readResponse()
                } finally {
                    wakeLock.acquire()
                    idleRefreshTimer?.cancel()
                }

                if (response.isRelevant && !stopIdle) {
                    Timber.v("%s.idle(): Received a relevant untagged response during IDLE", logTag)
                    result = IdleResult.SYNC
                    stopIdle = true
                    sendDone()
                } else if (!response.isTagged) {
                    Timber.v("%s.idle(): Ignoring untagged response", logTag)
                }
            } while (response.tag != tag)

            if (!response.isOk) {
                throw MessagingException("Received non-OK response to IDLE command")
            }
        } while (!stopIdle)

        return result
    }

    @Synchronized
    private fun idleRefresh() {
        Timber.v("%s.idleRefresh()", logTag)

        if (!idleSent || doneSent) {
            Timber.v("%s: Connection is not in a state where it can be refreshed.", logTag)
            return
        }

        try {
            sendDone()
        } catch (e: IOException) {
            Timber.v(e, "%s: IOException while sending DONE", logTag)
        }
    }

    @Synchronized
    private fun sendDone() {
        val folder = folder ?: return
        val connection = connectionProvider.getConnection(folder) ?: return

        synchronized(connection) {
            if (connection.isConnected) {
                doneSent = true
                connection.setSocketDefaultReadTimeout()
                connection.sendContinuation("DONE")
            }
        }
    }

    private fun ImapConnection.setSocketIdleReadTimeout() {
        setSocketReadTimeout((idleRefreshTimeoutProvider.idleRefreshTimeoutMs + SOCKET_EXTRA_TIMEOUT_MS).toInt())
    }

    private val ImapResponse.isRelevant: Boolean
        get() {
            return if (!isTagged && size >= 2) {
                ImapResponseParser.equalsIgnoreCase(get(1), "EXISTS") ||
                    ImapResponseParser.equalsIgnoreCase(get(1), "EXPUNGE") ||
                    ImapResponseParser.equalsIgnoreCase(get(1), "FETCH")
            } else {
                false
            }
        }

    private val ImapResponse.isOk: Boolean
        get() = isTagged && size >= 1 && ImapResponseParser.equalsIgnoreCase(get(0), Responses.OK)
}
