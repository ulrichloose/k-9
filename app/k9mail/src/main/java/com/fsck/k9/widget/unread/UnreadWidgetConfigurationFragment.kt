package com.fsck.k9.widget.unread

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.ChooseAccount
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.choosefolder.ChooseFolderActivity
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.koin.android.ext.android.inject

class UnreadWidgetConfigurationFragment : PreferenceFragmentCompat() {
    private val preferences: Preferences by inject()
    private val repository: UnreadWidgetRepository by inject()
    private val unreadWidgetUpdater: UnreadWidgetUpdater by inject()

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var unreadAccount: Preference
    private lateinit var unreadFolderEnabled: CheckBoxPreference
    private lateinit var unreadFolder: Preference

    private var selectedAccountUuid: String? = null
    private var selectedFolderId: Long? = null
    private var selectedFolderDisplayName: String? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setHasOptionsMenu(true)
        setPreferencesFromResource(R.xml.unread_widget_configuration, rootKey)

        appWidgetId = arguments?.getInt(ARGUMENT_APP_WIDGET_ID) ?: error("Missing argument '$ARGUMENT_APP_WIDGET_ID'")

        unreadAccount = findPreference(PREFERENCE_UNREAD_ACCOUNT)!!
        unreadAccount.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(requireContext(), ChooseAccount::class.java)
            startActivityForResult(intent, REQUEST_CHOOSE_ACCOUNT)
            false
        }

        unreadFolderEnabled = findPreference(PREFERENCE_UNREAD_FOLDER_ENABLED)!!
        unreadFolderEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
            selectedFolderId = null
            selectedFolderDisplayName = null
            true
        }

        unreadFolder = findPreference(PREFERENCE_UNREAD_FOLDER)!!
        unreadFolder.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = ChooseFolderActivity.buildLaunchIntent(
                context = requireContext(),
                action = ChooseFolderActivity.Action.CHOOSE,
                accountUuid = selectedAccountUuid!!,
                showDisplayableOnly = true
            )
            startActivityForResult(intent, REQUEST_CHOOSE_FOLDER)
            false
        }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_ACCOUNT_UUID, selectedAccountUuid)
        outState.putLongIfPresent(STATE_SELECTED_FOLDER_ID, selectedFolderId)
        outState.putString(STATE_SELECTED_FOLDER_DISPLAY_NAME, selectedFolderDisplayName)
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        val accountUuid = savedInstanceState.getString(STATE_SELECTED_ACCOUNT_UUID)
        if (accountUuid != null) {
            handleChooseAccount(accountUuid)
            val folderId = savedInstanceState.getLongOrNull(STATE_SELECTED_FOLDER_ID)
            val folderSummary = savedInstanceState.getString(STATE_SELECTED_FOLDER_DISPLAY_NAME)
            if (folderId != null && folderSummary != null) {
                handleChooseFolder(folderId, folderSummary)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CHOOSE_ACCOUNT -> {
                    val accountUuid = data.getStringExtra(ChooseAccount.EXTRA_ACCOUNT_UUID)!!
                    handleChooseAccount(accountUuid)
                }
                REQUEST_CHOOSE_FOLDER -> {
                    val folderId = data.getLongExtra(ChooseFolderActivity.RESULT_SELECTED_FOLDER_ID, -1L)
                    val folderDisplayName = data.getStringExtra(ChooseFolderActivity.RESULT_FOLDER_DISPLAY_NAME)!!
                    handleChooseFolder(folderId, folderDisplayName)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleChooseAccount(accountUuid: String) {
        val userSelectedSameAccount = accountUuid == selectedAccountUuid
        if (userSelectedSameAccount) {
            return
        }

        selectedAccountUuid = accountUuid
        selectedFolderId = null
        selectedFolderDisplayName = null
        unreadFolder.summary = getString(R.string.unread_widget_folder_summary)
        if (SearchAccount.UNIFIED_INBOX == selectedAccountUuid) {
            handleSearchAccount()
        } else {
            handleRegularAccount()
        }
    }

    private fun handleSearchAccount() {
        if (SearchAccount.UNIFIED_INBOX == selectedAccountUuid) {
            unreadAccount.setSummary(R.string.unread_widget_unified_inbox_account_summary)
        }
        unreadFolderEnabled.isEnabled = false
        unreadFolderEnabled.isChecked = false
        unreadFolder.isEnabled = false
        selectedFolderId = null
        selectedFolderDisplayName = null
    }

    private fun handleRegularAccount() {
        val selectedAccount = preferences.getAccount(selectedAccountUuid!!)
            ?: error("Account $selectedAccountUuid not found")

        val accountDescription: String? = selectedAccount.description
        val summary = if (accountDescription.isNullOrEmpty()) selectedAccount.email else accountDescription

        unreadAccount.summary = summary
        unreadFolderEnabled.isEnabled = true
        unreadFolder.isEnabled = true
    }

    private fun handleChooseFolder(folderId: Long, folderDisplayName: String) {
        selectedFolderId = folderId
        selectedFolderDisplayName = folderDisplayName
        unreadFolder.summary = folderDisplayName
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.unread_widget_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                if (validateWidget()) {
                    updateWidgetAndExit()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun validateWidget(): Boolean {
        if (selectedAccountUuid == null) {
            Toast.makeText(requireContext(), R.string.unread_widget_account_not_selected, Toast.LENGTH_LONG).show()
            return false
        } else if (unreadFolderEnabled.isChecked && selectedFolderId == null) {
            Toast.makeText(requireContext(), R.string.unread_widget_folder_not_selected, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun updateWidgetAndExit() {
        val configuration = UnreadWidgetConfiguration(appWidgetId, selectedAccountUuid!!, selectedFolderId)
        repository.saveWidgetConfiguration(configuration)

        unreadWidgetUpdater.update(appWidgetId)

        // Let the caller know that the configuration was successful
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val activity = requireActivity()
        activity.setResult(Activity.RESULT_OK, resultValue)
        activity.finish()
    }

    private fun Bundle.putLongIfPresent(key: String, value: Long?) {
        if (value != null) {
            putLong(key, value)
        }
    }

    private fun Bundle.getLongOrNull(key: String): Long? {
        return if (containsKey(key)) getLong(key) else null
    }

    companion object {
        private const val ARGUMENT_APP_WIDGET_ID = "app_widget_id"

        private const val PREFERENCE_UNREAD_ACCOUNT = "unread_account"
        private const val PREFERENCE_UNREAD_FOLDER_ENABLED = "unread_folder_enabled"
        private const val PREFERENCE_UNREAD_FOLDER = "unread_folder"

        private const val REQUEST_CHOOSE_ACCOUNT = 1
        private const val REQUEST_CHOOSE_FOLDER = 2

        private const val STATE_SELECTED_ACCOUNT_UUID = "com.fsck.k9.widget.unread.selectedAccountUuid"
        private const val STATE_SELECTED_FOLDER_ID = "com.fsck.k9.widget.unread.selectedFolderId"
        private const val STATE_SELECTED_FOLDER_DISPLAY_NAME = "com.fsck.k9.widget.unread.selectedFolderDisplayName"

        fun create(appWidgetId: Int): UnreadWidgetConfigurationFragment {
            return UnreadWidgetConfigurationFragment().apply {
                arguments = bundleOf(ARGUMENT_APP_WIDGET_ID to appWidgetId)
            }
        }
    }
}
