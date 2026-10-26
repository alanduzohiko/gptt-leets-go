package com.mashxall.unified.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mashxall.unified.data.AsrCalculationSchool
import com.mashxall.unified.data.ContentRepository
import com.mashxall.unified.data.PrayerCalculationMethod
import com.mashxall.unified.data.PrayerRow
import com.mashxall.unified.data.PrayerTimeCalculator
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Alarm scheduling must not silently assume the app's display-default city.
 * A typed/selected city is valid only after the user explicitly picked it.
 * Location coordinates are resolved before this function is needed.
 */
internal fun explicitPrayerCityOrNull(cityName: String?, isExplicit: Boolean): String? {
    if (!isExplicit) return null
    val normalized = cityName?.trim().orEmpty()
    return normalized.takeIf { it.isNotBlank() }
}

object PrayerAlarmScheduler {
    const val PREFS = "mashxall_preferences"
    const val KEY_ENABLED = "adhan_enabled"
    const val KEY_AUDIO = "adhan_audio_asset"
    const val KEY_DAYS = "adhan_days_of_week"
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

    suspend fun scheduleUpcomingFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            cancelAll(context)
            return
        }
        // Cancel alarms from any previously selected city before rebuilding the
        // next occurrence for each prayer and chosen weekday.
        cancelAll(context)
        val now = System.currentTimeMillis()
        val selectedDays = prefs.getStringSet(KEY_DAYS, null)?.toSet()
        val upcomingRows = mutableMapOf<Int, Pair<Date, PrayerRow?>>()
        for (offset in 0..8) {
            val day = startOfDay(Date(), offset)
            val weekday = Calendar.getInstance().apply { time = day }.get(Calendar.DAY_OF_WEEK)
            if (!isPrayerDaySelected(selectedDays, weekday)) continue
            upcomingRows[offset] = day to resolveTimes(context, prefs, day)
        }

        PRAYERS.forEach { code ->
            for (offset in 0..8) {
                val (day, row) = upcomingRows[offset] ?: continue
                val trigger = row?.let { triggerAt(day, prayerTime(it, code)) } ?: continue
                if (trigger > now + 3000L) {
                    schedule(context, code, trigger)
                    break
                }
            }
        }
    }

    suspend fun scheduleNextDayForPrayer(context: Context, prayerCode: String) {
        if (prayerCode !in PRAYERS) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val selectedDays = prefs.getStringSet(KEY_DAYS, null)?.toSet()
        val now = System.currentTimeMillis()
        for (offset in 1..8) {
            val day = startOfDay(Date(), offset)
            val weekday = Calendar.getInstance().apply { time = day }.get(Calendar.DAY_OF_WEEK)
            if (!isPrayerDaySelected(selectedDays, weekday)) continue
            val row = resolveTimes(context, prefs, day) ?: continue
            val trigger = triggerAt(day, prayerTime(row, prayerCode)) ?: continue
            if (trigger > now) {
                schedule(context, prayerCode, trigger)
                return
            }
        }
    }

    private suspend fun resolveTimes(context: Context, prefs: android.content.SharedPreferences, date: Date): PrayerRow? {
        // Source-supplied city timetable rows remain preferred. These fixed
        // settings are only used when no verified source timetable exists.
        val method = PrayerCalculationMethod.fromId("mwl")
        val school = AsrCalculationSchool.fromId("shafi")
        val gpsLatitude = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val gpsLongitude = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        if (gpsLatitude != null && gpsLongitude != null) {
            return ContentRepository.loadPrayerTimesAtLocation(context, gpsLatitude, gpsLongitude, date, method, school)
        }
        val manualLatitude = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val manualLongitude = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        if (manualLatitude != null && manualLongitude != null) {
            return ContentRepository.loadPrayerTimesAtLocation(context, manualLatitude, manualLongitude, date, method, school)
        }
        val cityName = explicitPrayerCityOrNull(
            prefs.getString("selected_city", null),
            prefs.getBoolean("selected_city_explicit", false)
        ) ?: return null
        val countryName = prefs.getString("selected_city_country", "") ?: ""
        ContentRepository.loadPrayerTimes(context, cityName, date, countryName)?.let { return it }
        val city = ContentRepository.findCityByName(context, cityName, countryName) ?: return null
        return PrayerTimeCalculator.calculate(date, city.latitude, city.longitude, method, school)
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
    /** A missing weekday preference means all days for backward compatibility. */
    internal fun isPrayerDaySelected(selectedDays: Set<String>?, calendarDay: Int): Boolean {
        if (calendarDay !in Calendar.SUNDAY..Calendar.SATURDAY) return false
        val effective = selectedDays ?: (Calendar.SUNDAY..Calendar.SATURDAY).map { it.toString() }.toSet()
        return calendarDay.toString() in effective
    }

}
