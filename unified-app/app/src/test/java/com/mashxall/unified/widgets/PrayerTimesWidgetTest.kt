package com.mashxall.unified.widgets

import com.mashxall.unified.data.PrayerRow
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
        assertEquals("05:03", widgetPrayerTimeValue("05:03"))
        assertEquals("--:--", widgetPrayerTimeValue(null))
        assertEquals("--:--", widgetPrayerTimeValue(""))
        assertEquals("--:--", widgetPrayerTimeValue("  "))
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
}
