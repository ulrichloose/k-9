package com.fsck.k9.notification

import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.K9
import com.fsck.k9.TestClock
import com.fsck.k9.controller.MessageReference
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private val TIMESTAMP = 0L

class SummaryNotificationDataCreatorTest {
    private val account = createAccount()
    private val notificationDataCreator = SummaryNotificationDataCreator(SingleMessageNotificationDataCreator())

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<Clock> { TestClock() }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        setQuietTime(false)
    }

    @Test
    fun `single new message`() {
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        assertThat(result).isInstanceOf(SummarySingleNotificationData::class.java)
    }

    @Test
    fun `single notification during quiet time`() {
        setQuietTime(true)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isTrue()
    }

    @Test
    fun `single notification with quiet time disabled`() {
        setQuietTime(false)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style notification during quiet time`() {
        setQuietTime(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isTrue()
    }

    @Test
    fun `inbox-style notification with quiet time disabled`() {
        setQuietTime(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style base properties`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.notificationId).isEqualTo(
            NotificationIds.getNewMailSummaryNotificationId(account)
        )
        assertThat(summaryNotificationData.isSilent).isTrue()
        assertThat(summaryNotificationData.timestamp).isEqualTo(TIMESTAMP)
    }

    @Test
    fun `default actions`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.MarkAsRead)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.MarkAsRead)
    }

    @Test
    fun `always show delete action without confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `always show delete action with confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
        setConfirmDeleteFromNotification(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `show delete action for single notification without confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).doesNotContain(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `never show delete action`() {
        setDeleteAction(K9.NotificationQuickDelete.NEVER)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).doesNotContain(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `archive action with archive folder`() {
        account.archiveFolderId = 1
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        account.archiveFolderId = null
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Archive)
    }

    private fun setQuietTime(quietTime: Boolean) {
        K9.isQuietTimeEnabled = quietTime
        if (quietTime) {
            K9.quietTimeStarts = "0:00"
            K9.quietTimeEnds = "23:59"
        }
    }

    private fun setDeleteAction(mode: K9.NotificationQuickDelete) {
        K9.notificationQuickDeleteBehaviour = mode
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        K9.isConfirmDeleteFromNotification = confirm
    }

    private fun createAccount(): Account {
        return Account("00000000-0000-0000-0000-000000000000").apply {
            accountNumber = 42
        }
    }

    private fun createNotificationContent() = NotificationContent(
        messageReference = MessageReference("irrelevant", 1, "irrelevant"),
        sender = "irrelevant",
        subject = "irrelevant",
        preview = "irrelevant",
        summary = "irrelevant"
    )

    private fun createNotificationData(
        contentList: List<NotificationContent> = listOf(createNotificationContent())
    ): NotificationData {
        val activeNotifications = contentList.mapIndexed { index, content ->
            NotificationHolder(notificationId = index, TIMESTAMP, content)
        }

        return NotificationData(account, activeNotifications, inactiveNotifications = emptyList())
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createNotificationDataWithMultipleMessages(times: Int = 2): NotificationData {
        val contentList = buildList {
            repeat(times) {
                add(createNotificationContent())
            }
        }
        return createNotificationData(contentList)
    }
}
