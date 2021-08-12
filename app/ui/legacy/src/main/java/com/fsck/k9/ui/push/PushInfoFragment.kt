package com.fsck.k9.ui.push

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.fsck.k9.controller.push.PushController
import com.fsck.k9.notification.NotificationChannelManager
import com.fsck.k9.ui.R
import org.koin.android.ext.android.inject

private const val LEARN_MORE_URL = "https://k9mail.app/go/push-info"

class PushInfoFragment : Fragment() {
    private val notificationChannelManager: NotificationChannelManager by inject()
    private val pushController: PushController by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_push_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeNotificationSection(view)
        initializeDisablePushSection(view)
    }

    private fun initializeNotificationSection(view: View) {
        val notificationTextView = view.findViewById<TextView>(R.id.notificationText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configureNotificationText = getString(R.string.push_info_configure_notification_text)
            notificationTextView.text = getString(
                R.string.push_info_notification_explanation_text,
                configureNotificationText
            )
            val configureNotificationButton = view.findViewById<Button>(R.id.configureNotificationButton)
            configureNotificationButton.isVisible = true
            configureNotificationButton.setOnClickListener {
                launchNotificationSettings()
            }
        } else {
            notificationTextView.text = getString(R.string.push_info_notification_explanation_text, "")
        }

        view.findViewById<Button>(R.id.learnMoreButton).setOnClickListener {
            launchLearnMoreAction()
        }
    }

    private fun initializeDisablePushSection(view: View) {
        val disablePushButton = view.findViewById<Button>(R.id.disablePushButton)
        disablePushButton.setOnClickListener(::disablePush)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun launchNotificationSettings() {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, this@PushInfoFragment.requireContext().packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelManager.pushChannelId)
        }
        startActivity(intent)
    }

    private fun launchLearnMoreAction() {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(LEARN_MORE_URL))
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.error_activity_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun disablePush(disablePushButton: View) {
        disablePushButton.isEnabled = false
        pushController.disablePush()
    }
}
