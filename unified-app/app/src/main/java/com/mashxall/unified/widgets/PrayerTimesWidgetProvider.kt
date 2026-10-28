package com.mashxall.unified.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.mashxall.unified.MainActivity
import com.mashxall.unified.R
import com.mashxall.unified.data.ContentRepository
import com.mashxall.unified.data.PrayerRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Home-screen prayer widget for quick at-a-glance access without opening Mashxall.
 * Uses the same stored GPS/manual city choices and the same content repository as
 * the full prayer screen; it does not invent a default coordinate.
 */
class PrayerTimesWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_REFRESH || action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: manager.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
            val pendingResult = goAsync()
            scope.launch {
                try {
                    widgetIds.forEach { updateOne(context.applicationContext, manager, it) }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    private suspend fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
        manager.updateAppWidget(widgetId, render(context, widgetId, null, Companion.locationFallback("ckb"), ""))
        val loaded = runCatching { loadTodayAndTomorrow(context) }.getOrNull()
        val language = context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
            .getString("language", "ckb") ?: "ckb"
        val views = render(
            context,
            widgetId,
            loaded?.today,
            loaded?.locationLabel ?: Companion.locationFallback(language),
            loaded?.nextPrayerLabel(language).orEmpty()
        )
        manager.updateAppWidget(widgetId, views)
    }

    private fun render(context: Context, widgetId: Int, row: PrayerRow?, locationLabel: String, nextPrayer: String): RemoteViews {
        val language = context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
            .getString("language", "ckb") ?: "ckb"
        val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
        views.setTextViewText(
            R.id.widget_prayer_title,
            when (language) {
                "en" -> "Mashxall • Prayer times"
                "ar" -> "مەشخەڵ • مواقيت الصلاة"
                else -> "مەشخەڵ • کاتی نوێژ"
            }
        )
        views.setTextViewText(R.id.widget_prayer_location, locationLabel)
        views.setTextViewText(
            R.id.widget_prayer_date,
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        views.setTextViewText(R.id.widget_prayer_next, nextPrayer.ifBlank {
            when (language) {
                "en" -> "Prayer times unavailable"
                "ar" -> "مواقيت الصلاة غير متاحة"
                else -> "کاتەکانی نوێژ بەردەست نییە"
            }
        })

        val entries = listOf(
            Triple(R.id.widget_time_fajr, prayerLabel(language, "fajr"), row?.fajr),
            Triple(R.id.widget_time_sunrise, prayerLabel(language, "sunrise"), row?.sunrise),
            Triple(R.id.widget_time_dhuhr, prayerLabel(language, "dhuhr"), row?.dhuhr),
            Triple(R.id.widget_time_asr, prayerLabel(language, "asr"), row?.asr),
            Triple(R.id.widget_time_maghrib, prayerLabel(language, "maghrib"), row?.maghrib),
            Triple(R.id.widget_time_isha, prayerLabel(language, "isha"), row?.isha)
        )
        entries.forEach { (viewId, label, time) ->
            views.setTextViewText(viewId, "$label\n${time?.takeIf { it.isNotBlank() } ?: "--:--"}")
        }
        val openPrayer = Intent(context, MainActivity::class.java).putExtra(
            MainActivity.EXTRA_OPEN_SCREEN, "prayer"
        )
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getActivity(context, widgetId + 8430, openPrayer, flags)
        views.setOnClickPendingIntent(R.id.widget_prayer_root, pending)
        return views
    }

    private suspend fun loadTodayAndTomorrow(context: Context): WidgetPrayerData? {
        val prefs = context.getSharedPreferences("mashxall_preferences", Context.MODE_PRIVATE)
        val gpsLat = prefs.getString("gps_latitude", null)?.toDoubleOrNull()
        val gpsLon = prefs.getString("gps_longitude", null)?.toDoubleOrNull()
        val manualLat = prefs.getString("manual_city_latitude", null)?.toDoubleOrNull()
        val manualLon = prefs.getString("manual_city_longitude", null)?.toDoubleOrNull()
        val hasManualCoordinates = manualLat != null && manualLon != null
        val cityName = com.mashxall.unified.notifications.usableSavedPrayerCityName(
            prefs.getString("selected_city", ""), hasManualCoordinates
        ).orEmpty()
        val countryName = prefs.getString("selected_city_country", "").orEmpty().trim()

        suspend fun rowFor(date: Date): PrayerRow? {
            if (gpsLat != null && gpsLon != null) {
                return ContentRepository.loadPrayerTimesAtLocation(context, gpsLat, gpsLon, date)
            }
            if (cityName.isBlank()) return null
            if (hasManualCoordinates) {
                ContentRepository.loadPrayerTimes(context, cityName, date, countryName)?.let { return it }
                return ContentRepository.loadPrayerTimesAtLocation(context, manualLat!!, manualLon!!, date)
            }
            ContentRepository.loadPrayerTimes(context, cityName, date, countryName)?.let { return it }
            val city = ContentRepository.findCityByName(context, cityName, countryName) ?: return null
            return ContentRepository.loadPrayerTimesAtLocation(context, city.latitude, city.longitude, date)
        }

        val today = Date()
        val todayRow = rowFor(today) ?: return null
        val tomorrowCalendar = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowRow = rowFor(tomorrowCalendar.time)
        val locationLabel = when {
            gpsLat != null && gpsLon != null -> String.format(Locale.US, "%.3f, %.3f", gpsLat, gpsLon)
            manualLat != null && manualLon != null -> String.format(Locale.US, "%.3f, %.3f", manualLat, manualLon)
            else -> listOf(cityName, countryName).filter { it.isNotBlank() }.joinToString(", ")
        }
        return WidgetPrayerData(todayRow, tomorrowRow, locationLabel)
    }

    private data class WidgetPrayerData(
        val today: PrayerRow,
        val tomorrow: PrayerRow?,
        val locationLabel: String
    ) {
        fun nextPrayerLabel(language: String): String {
            val prayers = listOf(
                "fajr" to today.fajr,
                "dhuhr" to today.dhuhr,
                "asr" to today.asr,
                "maghrib" to today.maghrib,
                "isha" to today.isha
            )
            val now = LocalTime.now()
            val next = prayers.firstOrNull { (_, time) -> parseTime(time)?.isAfter(now) == true }
            val chosen = next ?: (tomorrow?.let { "fajr" to it.fajr } ?: ("fajr" to today.fajr))
            val name = prayerLabel(language, chosen.first)
            val whenText = if (next == null) {
                when (language) {
                    "en" -> "Tomorrow"
                    "ar" -> "غدًا"
                    else -> "سبەی"
                }
            } else {
                when (language) {
                    "en" -> "Next"
                    "ar" -> "القادم"
                    else -> "داهاتوو"
                }
            }
            return "$whenText: $name • ${chosen.second}"
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.mashxall.unified.REFRESH_PRAYER_WIDGET"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun requestRefresh(context: Context) {
            context.sendBroadcast(
                Intent(context, PrayerTimesWidgetProvider::class.java).setAction(ACTION_REFRESH)
            )
        }

        private fun locationFallback(language: String) = when (language) {
            "en" -> "Select a city in Mashxall"
            "ar" -> "اختر مدينة في مەشخەڵ"
            else -> "شارێک لە مەشخەڵ هەڵبژێرە"
        }
    }
}

private fun prayerLabel(language: String, key: String): String = when (key) {
    "fajr" -> when (language) { "en" -> "Fajr"; "ar" -> "الفجر"; else -> "بەیانی" }
    "sunrise" -> when (language) { "en" -> "Sunrise"; "ar" -> "الشروق"; else -> "هەڵاتنی خۆر" }
    "dhuhr" -> when (language) { "en" -> "Dhuhr"; "ar" -> "الظهر"; else -> "نیوەڕۆ" }
    "asr" -> when (language) { "en" -> "Asr"; "ar" -> "العصر"; else -> "عەسر" }
    "maghrib" -> when (language) { "en" -> "Maghrib"; "ar" -> "المغرب"; else -> "مەغریب" }
    else -> when (language) { "en" -> "Isha"; "ar" -> "العشاء"; else -> "عیشا" }
}

private fun parseTime(value: String): LocalTime? {
    val parts = value.trim().split(":")
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].take(2).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return runCatching { LocalTime.of(hour, minute) }.getOrNull()
}
