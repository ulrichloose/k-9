package com.fsck.k9.ui.managefolders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val manageFoldersUiModule = module {
    viewModel { ManageFoldersViewModel(folderRepository = get()) }
    viewModel { FolderSettingsViewModel(preferences = get(), folderRepository = get(), messagingController = get()) }
}
