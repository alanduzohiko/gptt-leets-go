package com.mashxall.unified.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mashxall.unified.data.ContentRepository
import com.mashxall.unified.AppLanguage
import com.mashxall.unified.data.PrayerRow
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PrayerAlarmScheduleResult(
    val sourceTimetableAvailable: Boolean,
    val scheduledCount: Int
)

internal fun prayerAlarmScheduleMessage(result: PrayerAlarmScheduleResult, language: AppLanguage): String = when {
    !result.sourceTimetableAvailable -> when (language) {
        AppLanguage.EN -> "No valid Salawati timetable was found for this location. Choose a city or refresh GPS, then enable adhan alerts again."
        AppLanguage.AR -> "لم يُعثر على جدول صلواتي صالح لهذا الموقع. اختر مدينة أو حدّث GPS، ثم فعّل تنبيهات الأذان مجددًا."
        AppLanguage.CKB -> "خشتەیەکی دروستی صەڵەواتی بۆ ئەم شوێنە نەدۆزرایەوە؛ شارێک هەڵبژێرە یان GPS نوێ بکەرەوە، پاشان ئاگادارکردنەوەی بانگ چالاک بکەوە."
    }
    result.scheduledCount == 0 -> when (language) {
        AppLanguage.EN -> "The timetable loaded, but no future adhan alarm could be scheduled. Check today's and tomorrow's source rows."
        AppLanguage.AR -> "تم تحميل الجدول، لكن لم يُجدول أي تنبيه أذان قادم. تحقّق من سجلي اليوم والغد في المصدر."
        AppLanguage.CKB -> "خشتەکە بارکرا، بەڵام هیچ ئاگادارکردنەوەیەکی بانگی داهاتوو دانەنرا؛ خشتەی ئەمڕۆ و سبەی لە سەرچاوەکە بپشکنە."
    }
    result.scheduledCount < 5 -> when (language) {
        AppLanguage.EN -> "Only ${result.scheduledCount} of 5 upcoming prayer alerts could be scheduled from the source timetable."
        AppLanguage.AR -> "تمت جدولة ${result.scheduledCount} فقط من أصل 5 تنبيهات صلاة قادمة من جدول المصدر."
        AppLanguage.CKB -> "تەنها ${result.scheduledCount} لە ٥ ئاگادارکردنەوەی نوێژی داهاتوو لە خشتەی سەرچاوەکە دانرا."
    }
    else -> when (language) {
        AppLanguage.EN -> "The next 5 prayer alerts were scheduled from the verified Salawati timetable."
        AppLanguage.AR -> "جرت جدولة تنبيهات الصلوات الخمسة القادمة من جدول صلواتي المتحقق منه."
        AppLanguage.CKB -> "٥ ئاگادارکردنەوەی نوێژی داهاتوو لە خشتەی پشتڕاستکراوی صەڵەواتی دانران."
    }
}

internal fun prayerAlarmScheduleFailureMessage(language: AppLanguage): String = when (language) {
    AppLanguage.EN -> "Android could not schedule adhan alerts. Check alarm and notification permissions in system settings."
    AppLanguage.AR -> "تعذّر على Android جدولة تنبيهات الأذان. تحقّق من أذونات المنبّه والإشعارات في إعدادات النظام."
    AppLanguage.CKB -> "ئەندرۆید نەتوانی ئاگادارکردنەوەی بانگ دابنێت؛ مۆڵەتی کاتژمێر و ئاگادارکردنەوە لە ڕێکخستنەکانی سیستەم بپشکنە."
}

internal fun usableSavedPrayerCityName(cityName: String?, hasManualCoordinates: Boolean): String? {
    val name = cityName?.trim().orEmpty()
    if (name.isBlank()) return null
    // Erbil was once a UI placeholder; treat it as real only when the user
    // explicitly selected it and the app saved its manual coordinates.
    if (name.equals("Erbil", ignoreCase = true) && !hasManualCoordinates) return null
    return name
}

object PrayerAlarmScheduler {
    const val PREFS = "mashxall_preferences"
    const val KEY_ENABLED = "adhan_enabled"
    const val KEY_AUDIO = "adhan_audio_asset"
    private const val ACTION_ALARM = "com.mashxall.unified.PRAYER_ALARM"
    private const val BASE_REQUEST_CODE = 7400
    private val PRAYERS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        PRAYERS.forEach { code ->
            val pending = pendingIntent(context, code)
            manager.cancel(pending)
            pending.cancel()
        }
    }

    suspend fun scheduleUpcomingFromPreferences(context: Context): PrayerAlarmScheduleResult {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            cancelAll(context)
            return PrayerAlarmScheduleResult(sourceTimetableAvailable = false, scheduledCount = 0)
        }
        // Always retire the previous locations' pending alarms before creating
        // a fresh set. If the new city has no valid timetable, old-city alarms
        // must not remain armed.
        cancelAll(context)
        val today = startOfDay(Date(), 0)
        val tomorrow = startOfDay(Date(), 1)
        val todayTimes = resolveTimes(context, prefs, today)
        val tomorrowTimes = resolveTimes(context, prefs, tomorrow)
        if (todayTimes == null && tomorrowTimes == null) {
            return PrayerAlarmScheduleResult(sourceTimetableAvailable = false, scheduledCount = 0)
        }
        val now = System.currentTimeMillis()
        var scheduledCount = 0
        PRAYERS.forEach { code ->
            val todayTrigger = todayTimes?.let { triggerAt(today, prayerTime(it, code)) }
            if (todayTrigger != null && todayTrigger > now + 3000L) {
                schedule(context, code, todayTrigger)
                scheduledCount++
            } else {
                val tomorrowTrigger = tomorrowTimes?.let { triggerAt(tomorrow, prayerTime(it, code)) }
                if (tomorrowTrigger != null && tomorrowTrigger > now) {
                    schedule(context, code, tomorrowTrigger)
                    scheduledCount++
                }
            }
        }
        return PrayerAlarmScheduleResult(sourceTimetableAvailable = todayTimes != null || tomorrowTimes != null, scheduledCount = scheduledCount)
    }

    suspend fun scheduleNextDayForPrayer(context: Context, prayerCode: String) {
        if (prayerCode !in PRAYERS) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val tomorrow = startOfDay(Date(), 1)
        val row = resolveTimes(context, prefs, tomorrow) ?: return
        triggerAt(tomorrow, prayerTime(row, prayerCode))?.let { schedule(context, prayerCode, it) }
    }

    private suspend fun resolveTimes(context: Context, prefs: android.content.SharedPreferences, date: Date): PrayerRow? {
        val gpsLatitude = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val gpsLongitude = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        if (gpsLatitude != null && gpsLongitude != null) {
            return ContentRepository.salawatiOnlyPrayerTimes(
                ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude, gpsLongitude, date)
            )
        }
        val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        val hasManualCoordinates = manualLatitude != null && manualLongitude != null
        val cityName = usableSavedPrayerCityName(prefs.getString("selected_city", ""), hasManualCoordinates) ?: return null
        val countryName = prefs.getString("selected_city_country", "").orEmpty().trim()

        if (hasManualCoordinates) {
            ContentRepository.salawatiOnlyPrayerTimes(
                ContentRepository.loadPrayerTimes(context, cityName, date, countryName)
            )?.let { return it }
            return ContentRepository.salawatiOnlyPrayerTimes(
                ContentRepository.loadPrayerTimesAtLocation(context, manualLatitude!!, manualLongitude!!, date)
            )
        }
        ContentRepository.salawatiOnlyPrayerTimes(
            ContentRepository.loadPrayerTimes(context, cityName, date, countryName)
        )?.let { return it }
        val city = ContentRepository.findCityByName(context, cityName, countryName) ?: return null
        return ContentRepository.salawatiOnlyPrayerTimes(
            ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, date)
        )
    }

    private fun schedule(context: Context, code: String, whenMillis: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, code)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
            } catch (_: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
            }
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        }
    }

    private fun pendingIntent(context: Context, code: String): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("prayer_code", code)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + PRAYERS.indexOf(code), intent, flags)
    }

    private fun startOfDay(date: Date, addDays: Int): Date =
        Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_YEAR, addDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    private fun triggerAt(day: Date, time: String): Long? {
        val pieces = time.split(":")
        val hour = pieces.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = pieces.getOrNull(1)?.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return Calendar.getInstance().apply {
            this.time = day
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun prayerTime(row: PrayerRow, code: String): String = when (code) {
        "fajr" -> row.fajr
        "dhuhr" -> row.dhuhr
        "asr" -> row.asr
        "maghrib" -> row.maghrib
        else -> row.isha
    }
}
