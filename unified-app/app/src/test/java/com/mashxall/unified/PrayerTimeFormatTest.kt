package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimeFormatTest {
    @Test
    fun twelveHourFormatHandlesMorningAfternoonMidnightAndNoon() {
        assertEquals("4:51 AM", formatPrayerTime("04:51", AppLanguage.EN, use24Hour = false))
        assertEquals("2:49 PM", formatPrayerTime("14:49", AppLanguage.EN, use24Hour = false))
        assertEquals("12:05 AM", formatPrayerTime("00:05", AppLanguage.EN, use24Hour = false))
        assertEquals("12:00 PM", formatPrayerTime("12:00", AppLanguage.EN, use24Hour = false))
    }

    @Test
    fun twentyFourHourFormatIsOptInAndPreservesLeadingZero() {
        assertEquals("04:51", formatPrayerTime("04:51", AppLanguage.EN, use24Hour = true))
        assertEquals("14:49", formatPrayerTime("14:49", AppLanguage.EN, use24Hour = true))
    }

    @Test
    fun preservesUnavailableOrUnrecognizedSourceTimeInsteadOfInventingOne() {
        assertEquals("time unavailable", formatPrayerTime("time unavailable", AppLanguage.EN, use24Hour = false))
        assertEquals("", formatPrayerTime("", AppLanguage.EN, use24Hour = false))
    }
}
