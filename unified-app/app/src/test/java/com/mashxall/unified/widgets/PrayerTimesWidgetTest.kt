package com.mashxall.unified.widgets

import com.mashxall.unified.data.PrayerRow
import com.mashxall.unified.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesWidgetTest {
    private fun completeRow() = PrayerRow(
        city = "Sulaymaniyah",
        date = "2026-11-07",
        fajr = "05:03",
        sunrise = "06:25",
        dhuhr = "11:47",
        asr = "14:32",
        maghrib = "17:02",
        isha = "18:21"
    )

    @Test
    fun keepsAllSixSourceTimesAsRequiredWidgetData() {
        val row = completeRow()
        assertTrue(isCompleteWidgetPrayerRow(row))
        assertEquals(
            listOf("05:03", "06:25", "11:47", "14:32", "17:02", "18:21"),
            listOf(row.fajr, row.sunrise, row.dhuhr, row.asr, row.maghrib, row.isha)
        )
    }

    @Test
    fun incompleteRefreshDoesNotReplaceTheLastGoodWidgetRow() {
        val row = completeRow().copy(asr = "", maghrib = " ", isha = "")
        assertFalse(isCompleteWidgetPrayerRow(row))
        assertFalse(isCompleteWidgetPrayerRow(null))
    }

    @Test
    fun emptyTimeUsesVisiblePlaceholderRatherThanAnEmptyCell() {
        assertEquals("5:03 AM", widgetPrayerTimeValue("05:03", AppLanguage.EN, false))
        assertEquals("--:--", widgetPrayerTimeValue(null, AppLanguage.EN, false))
        assertEquals("--:--", widgetPrayerTimeValue("", AppLanguage.EN, false))
        assertEquals("--:--", widgetPrayerTimeValue("  ", AppLanguage.EN, false))
    }

    @Test
    fun savedCoordinatesRemainUsableWithoutAGpsFreshnessGate() {
        val saved = widgetGpsCoordinates(35.327, 45.685)
        assertNotNull(saved)
        assertEquals(35.327, saved!!.first, 0.000001)
        assertEquals(45.685, saved.second, 0.000001)
    }

    @Test
    fun rejectsMissingOrInvalidCoordinates() {
        assertNull(widgetGpsCoordinates(null, 45.685))
        assertNull(widgetGpsCoordinates(35.327, null))
        assertNull(widgetGpsCoordinates(Double.NaN, 45.685))
        assertNull(widgetGpsCoordinates(95.0, 45.685))
        assertNull(widgetGpsCoordinates(35.327, 181.0))
    }
    @Test
    fun widgetUsesTheSameTwelveHourFormattingAsThePrayerScreen() {
        assertEquals("2:49 PM", widgetPrayerTimeValue("14:49", AppLanguage.EN, false))
        assertEquals("12:05 AM", widgetPrayerTimeValue("00:05", AppLanguage.EN, false))
        assertEquals("12:00 PM", widgetPrayerTimeValue("12:00", AppLanguage.EN, false))
        assertEquals("2:49 مساءً", widgetPrayerTimeValue("14:49", AppLanguage.AR, false))
        assertEquals("2:49 د.ن", widgetPrayerTimeValue("14:49", AppLanguage.CKB, false))
    }

    @Test
    fun widgetRespectsTheAppsExplicitTwentyFourHourPreference() {
        assertEquals("14:49", widgetPrayerTimeValue("14:49", AppLanguage.EN, true))
    }

}
