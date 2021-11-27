package com.fsck.k9.storage

import android.app.Application
import com.fsck.k9.AppConfig
import com.fsck.k9.Core
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.DI
import com.fsck.k9.K9
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.coreModules
import com.fsck.k9.crypto.EncryptionExtractor
import com.fsck.k9.preferences.K9StoragePersister
import com.fsck.k9.preferences.StoragePersister
import org.koin.dsl.module
import org.mockito.kotlin.mock

class TestApp : Application() {
    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()
        DI.start(this, coreModules + storageModule + testModule)

        K9.init(this)
        Core.init(this)
    }
}

val testModule = module {
    single { AppConfig(emptyList()) }
    single { mock<CoreResourceProvider>() }
    single { mock<EncryptionExtractor>() }
    single<StoragePersister> { K9StoragePersister(get()) }
    single { mock<BackendManager>() }
}
