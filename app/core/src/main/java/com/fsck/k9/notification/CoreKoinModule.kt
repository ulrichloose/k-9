package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.AccountPreferenceSerializer
import java.util.concurrent.Executors
import org.koin.dsl.module

val coreNotificationModule = module {
    single {
        NotificationController(
            certificateErrorNotificationController = get(),
            authenticationErrorNotificationController = get(),
            syncNotificationController = get(),
            sendFailedNotificationController = get(),
            newMailNotificationController = get()
        )
    }
    single { NotificationManagerCompat.from(get()) }
    single { NotificationHelper(context = get(), notificationManager = get(), channelUtils = get()) }
    single {
        NotificationChannelManager(
            preferences = get(),
            backgroundExecutor = Executors.newSingleThreadExecutor(),
            notificationManager = get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
            resourceProvider = get()
        )
    }
    single {
        AccountPreferenceSerializer(
            storageManager = get(),
            resourceProvider = get(),
            serverSettingsSerializer = get()
        )
    }
    single {
        CertificateErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get()
        )
    }
    single {
        AuthenticationErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get()
        )
    }
    single {
        SyncNotificationController(notificationHelper = get(), actionBuilder = get(), resourceProvider = get())
    }
    single {
        SendFailedNotificationController(notificationHelper = get(), actionBuilder = get(), resourceProvider = get())
    }
    single {
        NewMailNotificationController(
            notificationManager = get(),
            newMailNotificationManager = get(),
            summaryNotificationCreator = get(),
            singleMessageNotificationCreator = get()
        )
    }
    single {
        NewMailNotificationManager(
            contentCreator = get(),
            notificationRepository = get(),
            baseNotificationDataCreator = get(),
            singleMessageNotificationDataCreator = get(),
            summaryNotificationDataCreator = get(),
            clock = get()
        )
    }
    factory { NotificationContentCreator(context = get(), resourceProvider = get()) }
    factory { BaseNotificationDataCreator() }
    factory { SingleMessageNotificationDataCreator() }
    factory { SummaryNotificationDataCreator(singleMessageNotificationDataCreator = get()) }
    factory {
        SingleMessageNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            lockScreenNotificationCreator = get(),
            notificationManager = get()
        )
    }
    factory {
        SummaryNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            lockScreenNotificationCreator = get(),
            singleMessageNotificationCreator = get(),
            resourceProvider = get(),
            notificationManager = get()
        )
    }
    factory { LockScreenNotificationCreator(notificationHelper = get(), resourceProvider = get()) }
    single {
        PushNotificationManager(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
            notificationManager = get()
        )
    }
    single {
        NotificationRepository(
            notificationStoreProvider = get(),
            localStoreProvider = get(),
            messageStoreManager = get(),
            notificationContentCreator = get()
        )
    }
}
