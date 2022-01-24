package com.fsck.k9.preferences.migrations

import android.database.sqlite.SQLiteDatabase

internal object StorageMigrations {
    @JvmStatic
    fun upgradeDatabase(db: SQLiteDatabase, migrationsHelper: StorageMigrationsHelper) {
        val oldVersion = db.version

        if (oldVersion < 2) StorageMigrationTo2.urlEncodeUserNameAndPassword(db, migrationsHelper)
        if (oldVersion < 3) StorageMigrationTo3(db, migrationsHelper).rewriteFolderNone()
        if (oldVersion < 4) StorageMigrationTo4(db, migrationsHelper).insertSpecialFolderSelectionValues()
        if (oldVersion < 5) StorageMigrationTo5(db, migrationsHelper).fixMailCheckFrequencies()
        if (oldVersion < 6) StorageMigrationTo6(db, migrationsHelper).performLegacyMigrations()
        if (oldVersion < 7) StorageMigrationTo7(db, migrationsHelper).rewriteEnumOrdinalsToNames()
        if (oldVersion < 8) StorageMigrationTo8(db, migrationsHelper).rewriteTheme()
        // 9: "Temporarily disable Push" is no longer necessary
        if (oldVersion < 10) StorageMigrationTo10(db, migrationsHelper).removeSavedFolderSettings()
        if (oldVersion < 11) StorageMigrationTo11(db, migrationsHelper).upgradeMessageViewContentFontSize()
        if (oldVersion < 12) StorageMigrationTo12(db, migrationsHelper).removeStoreAndTransportUri()
        if (oldVersion < 13) StorageMigrationTo13(db, migrationsHelper).renameHideSpecialAccounts()
        if (oldVersion < 14) StorageMigrationTo14(db, migrationsHelper).disablePushFoldersForNonImapAccounts()
        if (oldVersion < 15) StorageMigrationTo15(db, migrationsHelper).rewriteIdleRefreshInterval()
        if (oldVersion < 16) StorageMigrationTo16(db, migrationsHelper).changeDefaultRegisteredNameColor()
    }
}
