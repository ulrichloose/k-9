package com.fsck.k9.ui.settings.account

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import com.takisoft.preferencex.PreferenceActivityResultListener
import com.takisoft.preferencex.PreferenceFragmentCompat

private const val REQUEST_CODE_RINGTONE = 1

@SuppressLint("RestrictedApi")
class NotificationSoundPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = TypedArrayUtils.getAttr(
        context,
        androidx.preference.R.attr.preferenceStyle,
        android.R.attr.preferenceStyle
    ),
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes), PreferenceActivityResultListener {

    override fun onPreferenceClick(fragment: PreferenceFragmentCompat, preference: Preference) {
        launchRingtonePicker(fragment, onRestoreRingtone())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_RINGTONE || resultCode != Activity.RESULT_OK) return

        val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

        if (callChangeListener(uri?.toString().orEmpty())) {
            onSaveRingtone(uri)
        }
    }

    @Suppress("DEPRECATION")
    private fun launchRingtonePicker(fragment: PreferenceFragmentCompat, selectedRingtone: Uri?) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            .putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtone)

        fragment.startActivityForResult(intent, REQUEST_CODE_RINGTONE)
    }

    private fun onRestoreRingtone(): Uri? {
        val uriString = getPersistedString(null)?.takeIf { it.isNotEmpty() }
        return uriString?.let { Uri.parse(it) }
    }

    private fun onSaveRingtone(ringtoneUri: Uri?) {
        persistString(ringtoneUri?.toString().orEmpty())
    }
}
