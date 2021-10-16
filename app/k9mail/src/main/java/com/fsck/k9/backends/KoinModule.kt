package com.fsck.k9.backends

import app.k9mail.dev.developmentBackends
import app.k9mail.dev.developmentModuleAdditions
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.imap.BackendIdleRefreshManager
import com.fsck.k9.backend.imap.SystemAlarmManager
import com.fsck.k9.mail.store.imap.IdleRefreshManager
import org.koin.dsl.module

val backendsModule = module {
    single {
        BackendManager(
            mapOf(
                "imap" to get<ImapBackendFactory>(),
                "pop3" to get<Pop3BackendFactory>(),
                "webdav" to get<WebDavBackendFactory>()
            ) + developmentBackends()
        )
    }
    single {
        ImapBackendFactory(
            context = get(),
            accountManager = get(),
            powerManager = get(),
            idleRefreshManager = get(),
            backendStorageFactory = get(),
            trustedSocketFactory = get()
        )
    }
    single<SystemAlarmManager> { AndroidAlarmManager(context = get(), alarmManager = get()) }
    single<IdleRefreshManager> { BackendIdleRefreshManager(alarmManager = get()) }
    single { Pop3BackendFactory(get(), get()) }
    single { WebDavBackendFactory(get(), get(), get()) }

    developmentModuleAdditions()
}
