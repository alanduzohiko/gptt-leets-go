package com.mashxall.unified.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra("prayer_code") ?: return
        val prefs = context.getSharedPreferences(PrayerAlarmScheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PrayerAlarmScheduler.KEY_ENABLED, false)) return
        val language = prefs.getString("language", "ckb") ?: "ckb"
        val label = when (code) {
            "fajr" -> when (language) { "en" -> "Fajr"; "ar" -> "الفجر"; else -> "بەیانی" }
            "dhuhr" -> when (language) { "en" -> "Dhuhr"; "ar" -> "الظهر"; else -> "نیوەڕۆ" }
            "asr" -> when (language) { "en" -> "Asr"; "ar" -> "العصر"; else -> "عەسر" }
            "maghrib" -> when (language) { "en" -> "Maghrib"; "ar" -> "المغرب"; else -> "مەغریب" }
            else -> when (language) { "en" -> "Isha"; "ar" -> "العشاء"; else -> "عیشا" }
        }
        val audioPath = prefs.getString(PrayerAlarmScheduler.KEY_AUDIO, "") ?: ""
        ContextCompat.startForegroundService(
            context,
            Intent(context, AdhanPlaybackService::class.java)
                .putExtra(AdhanPlaybackService.EXTRA_PRAYER_CODE, code)
                .putExtra(AdhanPlaybackService.EXTRA_PRAYER_LABEL, label)
                .putExtra(AdhanPlaybackService.EXTRA_AUDIO_PATH, audioPath)
        )
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerAlarmScheduler.scheduleNextDayForPrayer(context.applicationContext, code)
            } finally {
                pending.finish()
            }
        }
    }
}
