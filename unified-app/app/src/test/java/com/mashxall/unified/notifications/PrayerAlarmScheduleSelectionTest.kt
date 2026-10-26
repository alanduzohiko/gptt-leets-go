package com.mashxall.unified.notifications

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerAlarmScheduleSelectionTest {
    @Test
    fun missingOrImplicitCityIsNeverUsedForPrayerAlarms() {
        assertFalse(explicitPrayerCityOrNull("Erbil", false) != null)
        assertFalse(explicitPrayerCityOrNull(null, true) != null)
        assertFalse(explicitPrayerCityOrNull("   ", true) != null)
    }

    @Test
    fun explicitlyChosenCityIsTrimmedAndReturnedForPrayerAlarms() {
        assertTrue(explicitPrayerCityOrNull("  Sulaymaniyah  ", true) == "Sulaymaniyah")
    }

    @Test
    fun absentWeekdayPreferenceMeansEveryDayForExistingUsers() {
        (Calendar.SUNDAY..Calendar.SATURDAY).forEach { day ->
            assertTrue(PrayerAlarmScheduler.isPrayerDaySelected(null, day))
        }
    }

    @Test
    fun onlySelectedWeekdaysAreScheduled() {
        val weekdays = setOf(Calendar.MONDAY.toString(), Calendar.WEDNESDAY.toString(), Calendar.FRIDAY.toString())
        assertTrue(PrayerAlarmScheduler.isPrayerDaySelected(weekdays, Calendar.MONDAY))
        assertTrue(PrayerAlarmScheduler.isPrayerDaySelected(weekdays, Calendar.WEDNESDAY))
        assertTrue(PrayerAlarmScheduler.isPrayerDaySelected(weekdays, Calendar.FRIDAY))
        assertFalse(PrayerAlarmScheduler.isPrayerDaySelected(weekdays, Calendar.TUESDAY))
        assertFalse(PrayerAlarmScheduler.isPrayerDaySelected(weekdays, Calendar.SUNDAY))
    }

    @Test
    fun emptyWeekdaySelectionSchedulesNoPrayerAlerts() {
        (Calendar.SUNDAY..Calendar.SATURDAY).forEach { day ->
            assertFalse(PrayerAlarmScheduler.isPrayerDaySelected(emptySet(), day))
        }
    }

    @Test
    fun invalidCalendarDayIsRejected() {
        assertFalse(PrayerAlarmScheduler.isPrayerDaySelected(null, 0))
        assertFalse(PrayerAlarmScheduler.isPrayerDaySelected(null, 8))
    }
}
