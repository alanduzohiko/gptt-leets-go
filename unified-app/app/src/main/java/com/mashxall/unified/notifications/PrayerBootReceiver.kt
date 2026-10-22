package com.mashxall.unified.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PrayerAlarmScheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PrayerAlarmScheduler.KEY_ENABLED, false)) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler.scheduleUpcomingFromPreferences(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
