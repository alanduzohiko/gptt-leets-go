package com.mashxall.unified.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.mashxall.unified.MainActivity
import com.mashxall.unified.data.ContentRepository
import com.mashxall.unified.AppLanguage
import com.mashxall.unified.isValidPersistedGpsFix
import com.mashxall.unified.resolvePrayerLocationMode
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
        AppLanguage.EN -> "The timetable loaded, but no enabled upcoming prayer alarm was scheduled. Check the prayer toggles and source times."
        AppLanguage.AR -> "تم تحميل الجدول، لكن لم تُجدول تنبيهات قادمة. تحقّق من مفاتيح الصلوات ومواقيت المصدر."
        AppLanguage.CKB -> "خشتەکە بارکرا، بەڵام هیچ ئاگادارکردنەوەیەکی چالاکی داهاتوو دانەنرا؛ کلیلەکانی نوێژ و کاتەکانی سەرچاوە بپشکنە."
    }
    else -> when (language) {
        AppLanguage.EN -> "${result.scheduledCount} enabled prayer alerts were scheduled from the verified Salawati timetable."
        AppLanguage.AR -> "تمت جدولة ${result.scheduledCount} من تنبيهات الصلاة المفعّلة من جدول صلواتي الموثوق."
        AppLanguage.CKB -> "${result.scheduledCount} ئاگادارکردنەوەی چالاککراوی نوێژ لە خشتەی پشتڕاستکراوی صەڵەواتی دانرا."
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
    const val KEY_AUDIO_TITLE = "adhan_audio_title"
    const val KEY_SCHEDULED_ALARM_IDS = "adhan_scheduled_alarm_ids"
    const val EXTRA_DAY_TOKEN = "prayer_alarm_day_token"
    private const val ACTION_ALARM = "com.mashxall.unified.PRAYER_ALARM"
    private const val ACTION_SHOW_ALARM = "com.mashxall.unified.SHOW_PRAYER_ALARM"
    private const val BASE_REQUEST_CODE = 7400
    private const val BASE_SHOW_REQUEST_CODE = 7600
    val PRAYER_CODES = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")
    val ALERT_CODES = PRAYER_CODES + "jumuah"
    private const val JUMUAH_CODE = "jumuah"

    fun enabledPreferenceKey(code: String): String = "adhan_enabled_$code"
    fun audioPreferenceKey(code: String): String = "adhan_audio_asset_$code"
    fun audioTitlePreferenceKey(code: String): String = "adhan_audio_title_$code"

    fun isPrayerAlertEnabled(prefs: android.content.SharedPreferences, code: String): Boolean =
        code in ALERT_CODES && prefs.getBoolean(enabledPreferenceKey(code), code != JUMUAH_CODE)

    internal fun effectivePrayerAlertEnabled(masterEnabled: Boolean, prayerEnabled: Boolean): Boolean =
        masterEnabled && prayerEnabled

    fun isPrayerAlertActive(prefs: android.content.SharedPreferences, code: String): Boolean =
        effectivePrayerAlertEnabled(
            prefs.getBoolean(KEY_ENABLED, false),
            isPrayerAlertEnabled(prefs, code)
        )

    internal fun prayerAlertCodesForDay(enabledCodes: Set<String>, isFriday: Boolean): List<String> = buildList {
        if ("fajr" in enabledCodes) add("fajr")
        if (isFriday && JUMUAH_CODE in enabledCodes) add(JUMUAH_CODE)
        else if ("dhuhr" in enabledCodes) add("dhuhr")
        if ("asr" in enabledCodes) add("asr")
        if ("maghrib" in enabledCodes) add("maghrib")
        if ("isha" in enabledCodes) add("isha")
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Each prayer may have more than one future occurrence. Cancel every
        // date-specific token so tomorrow can never replace today's alarm.
        val scheduledIds = prefs.getStringSet(KEY_SCHEDULED_ALARM_IDS, emptySet()).orEmpty().toList()
        scheduledIds.forEach { identity ->
            val separator = identity.indexOf('|')
            if (separator <= 0 || separator >= identity.lastIndex) return@forEach
            val code = identity.substring(0, separator)
            val dayToken = identity.substring(separator + 1)
            if (code !in ALERT_CODES || dayToken.isBlank()) return@forEach
            val pending = pendingIntent(context, code, dayToken)
            runCatching { manager.cancel(pending) }
            pending.cancel()
        }
        // Retire the single-token PendingIntents created by earlier app versions.
        ALERT_CODES.forEach { code ->
            val legacy = pendingIntent(context, code, null)
            runCatching { manager.cancel(legacy) }
            legacy.cancel()
        }
        prefs.edit().remove(KEY_SCHEDULED_ALARM_IDS).apply()
    }

    fun markAlarmDelivered(context: Context, code: String, dayToken: String?) {
        if (code !in ALERT_CODES || dayToken.isNullOrBlank()) return
        val pending = pendingIntent(context, code, dayToken)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { manager.cancel(pending) }
        pending.cancel()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val remaining = prefs.getStringSet(KEY_SCHEDULED_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
        remaining.remove(alarmIdentity(code, dayToken))
        prefs.edit().putStringSet(KEY_SCHEDULED_ALARM_IDS, remaining).apply()
    }

    internal fun prayerAlarmDayToken(triggerAtMillis: Long): String =
        Calendar.getInstance().apply { timeInMillis = triggerAtMillis }.let { calendar ->
            "%04d-%03d".format(Locale.US, calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR))
        }

    internal fun alarmIdentity(code: String, dayToken: String): String = "$code|$dayToken"

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
        val enabledCodes = ALERT_CODES.filter { isPrayerAlertActive(prefs, it) }.toSet()
        if (enabledCodes.isEmpty()) {
            cancelAll(context)
            return PrayerAlarmScheduleResult(sourceTimetableAvailable = true, scheduledCount = 0)
        }
        val today = startOfDay(Date(), 0)
        val tomorrow = startOfDay(Date(), 1)
        val todayTimes = resolveTimes(context, prefs, today)
        val tomorrowTimes = resolveTimes(context, prefs, tomorrow)
        if (todayTimes == null && tomorrowTimes == null) {
            return PrayerAlarmScheduleResult(sourceTimetableAvailable = false, scheduledCount = 0)
        }
        val now = System.currentTimeMillis()
        var scheduledCount = 0

        fun scheduleDate(date: Date, row: PrayerRow?) {
            if (row == null) return
            prayerAlertCodesForDay(enabledCodes, isFriday(date)).forEach { code ->
                val trigger = triggerAt(date, prayerTime(row, code))
                if (trigger != null && trigger > now + 3000L) {
                    if (schedule(context, code, trigger)) scheduledCount++
                }
            }
        }
        scheduleDate(today, todayTimes)
        scheduleDate(tomorrow, tomorrowTimes)

        if (JUMUAH_CODE in enabledCodes && !isFriday(tomorrow)) {
            val nextFriday = nextFridayStart(Date())
            val fridayTimes = resolveTimes(context, prefs, nextFriday)
            if (fridayTimes != null) {
                val trigger = triggerAt(nextFriday, fridayTimes.dhuhr)
                if (trigger != null && trigger > now + 3000L) {
                    if (schedule(context, JUMUAH_CODE, trigger)) scheduledCount++
                }
            }
        }
        return PrayerAlarmScheduleResult(
            sourceTimetableAvailable = todayTimes != null || tomorrowTimes != null,
            scheduledCount = scheduledCount
        )
    }

    suspend fun scheduleNextDayForPrayer(context: Context, prayerCode: String) {
        if (prayerCode !in ALERT_CODES) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!isPrayerAlertActive(prefs, prayerCode)) return

        val targetDay = if (prayerCode == JUMUAH_CODE) nextFridayStart(Date()) else startOfDay(Date(), 1)
        if (prayerCode == "dhuhr" && isFriday(targetDay) && isPrayerAlertEnabled(prefs, JUMUAH_CODE)) return
        val row = resolveTimes(context, prefs, targetDay) ?: return
        triggerAt(targetDay, prayerTime(row, prayerCode))?.let { schedule(context, prayerCode, it) }
    }

    private suspend fun resolveTimes(context: Context, prefs: android.content.SharedPreferences, date: Date): PrayerRow? {
        val gpsLatitude = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val gpsLongitude = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        val hasManualCoordinates = manualLatitude != null && manualLongitude != null
        val savedCityName = usableSavedPrayerCityName(prefs.getString("selected_city", ""), hasManualCoordinates)
        val locationMode = resolvePrayerLocationMode(
            prefs.getString("prayer_location_mode", null),
            gpsLatitude != null && gpsLongitude != null,
            hasManualCoordinates,
            hasUsableCitySelection = savedCityName != null
        )
        if (locationMode == "gps") {
            val gpsFixTime = prefs.getLong("gps_location_timestamp", 0L)
            // Saved GPS coordinates are the user's persistent prayer location. Do not
            // cancel alarms because the phone has been stationary for more than 48 hours.
            if (!isValidPersistedGpsFix(gpsLatitude, gpsLongitude, gpsFixTime)) return null
            return ContentRepository.salawatiOnlyPrayerTimes(
                ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude!!, gpsLongitude!!, date)
            )
        }
        val cityName = savedCityName ?: return null
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

    private fun schedule(context: Context, code: String, whenMillis: Long): Boolean {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val dayToken = prayerAlarmDayToken(whenMillis)
        val pending = pendingIntent(context, code, dayToken)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val prayerIndex = ALERT_CODES.indexOf(code).coerceAtLeast(0)
        val showIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHOW_ALARM
            data = Uri.parse("mashxall://show-prayer-alarm/" + code)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val showPending = PendingIntent.getActivity(
            context,
            BASE_SHOW_REQUEST_CODE + prayerIndex,
            showIntent,
            flags
        )
        try {
            // AlarmClockInfo gives the system a visible wake-up alarm and is more
            // reliable than an ordinary idle alarm on OEM battery managers.
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(whenMillis, showPending), pending)
        } catch (error: Exception) {
            Log.w("MashxallAdhanAlarm", "AlarmClock scheduling failed for " + code + "; trying exact fallback.", error)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms())
                ) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                }
            } catch (exactError: Exception) {
                Log.e("MashxallAdhanAlarm", "Exact alarm fallback failed for " + code + "; trying idle alarm.", exactError)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                    } else {
                        manager.set(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                    }
                } catch (fallbackError: Exception) {
                    Log.e("MashxallAdhanAlarm", "Could not schedule prayer alarm for " + code, fallbackError)
                    pending.cancel()
                    return false
                }
            }
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val identities = prefs.getStringSet(KEY_SCHEDULED_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
        identities.add(alarmIdentity(code, dayToken))
        prefs.edit().putStringSet(KEY_SCHEDULED_ALARM_IDS, identities).apply()
        Log.i("MashxallAdhanAlarm", "Scheduled prayer alarm: code=" + code + " day=" + dayToken + " at=" + whenMillis)
        return true
    }

    private fun pendingIntent(context: Context, code: String, dayToken: String?): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("prayer_code", code)
            if (!dayToken.isNullOrBlank()) {
                putExtra(EXTRA_DAY_TOKEN, dayToken)
                // PendingIntent identity includes the data URI: each date has a
                // distinct token for the same prayer code.
                data = Uri.parse("mashxall://prayer-alarm/" + code + "/" + dayToken)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, BASE_REQUEST_CODE + ALERT_CODES.indexOf(code), intent, flags)
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

    private fun isFriday(date: Date): Boolean =
        Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

    private fun nextFridayStart(from: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = from
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val offset = ((Calendar.FRIDAY - currentDay + 7) % 7).let { if (it == 0) 7 else it }
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return calendar.time
    }

    internal fun parsePrayerTimeInMinutes(value: String): Int? {
        val match = Regex("""^(\d{1,2}):(\d{2})(?:\s*([AaPp][Mm]))?$""")
            .matchEntire(value.trim()) ?: return null
        val rawHour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        val suffix = match.groupValues[3].uppercase(Locale.US)
        if (minute !in 0..59) return null
        val hour = when (suffix) {
            "AM" -> {
                if (rawHour !in 1..12) return null
                rawHour % 12
            }
            "PM" -> {
                if (rawHour !in 1..12) return null
                (rawHour % 12) + 12
            }
            else -> rawHour.takeIf { it in 0..23 } ?: return null
        }
        return hour * 60 + minute
    }

    private fun triggerAt(day: Date, time: String): Long? {
        val minutes = parsePrayerTimeInMinutes(time) ?: return null
        val hour = minutes / 60
        val minute = minutes % 60
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
        "jumuah" -> row.dhuhr
        else -> row.isha
    }
}
