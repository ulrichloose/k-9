package com.fsck.k9.backends

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import com.fsck.k9.backend.imap.SystemAlarmManager
import com.fsck.k9.helper.AlarmManagerCompat
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

private const val ALARM_ACTION = "com.fsck.k9.backends.ALARM"
private const val REQUEST_CODE = 1

private typealias Callback = () -> Unit

class AndroidAlarmManager(
    private val context: Context,
    private val alarmManager: AlarmManagerCompat,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SystemAlarmManager {
    private val coroutineScope = CoroutineScope(backgroundDispatcher)

    private val pendingIntent: PendingIntent = run {
        val intent = Intent(ALARM_ACTION).apply {
            setPackage(context.packageName)
        }
        val flags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private val callback = AtomicReference<Callback?>(null)

    init {
        val intentFilter = IntentFilter(ALARM_ACTION)
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val callback = callback.getAndSet(null)
                    if (callback == null) {
                        Timber.w("Alarm triggered but 'callback' was null")
                    } else {
                        coroutineScope.launch {
                            callback.invoke()
                        }
                    }
                }
            },
            intentFilter
        )
    }

    override fun setAlarm(triggerTime: Long, callback: Callback) {
        this.callback.set(callback)
        alarmManager.scheduleAlarm(triggerTime, pendingIntent)
    }

    override fun cancelAlarm() {
        callback.set(null)
        alarmManager.cancelAlarm(pendingIntent)
    }

    override fun now(): Long = SystemClock.elapsedRealtime()
}
