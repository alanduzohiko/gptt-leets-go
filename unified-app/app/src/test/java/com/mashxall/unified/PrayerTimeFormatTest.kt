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
        assertEquals("00:05", formatPrayerTime("00:05", AppLanguage.EN, use24Hour = true))
        assertEquals("04:51", formatPrayerTime("04:51", AppLanguage.EN, use24Hour = true))
        assertEquals("14:49", formatPrayerTime("14:49", AppLanguage.EN, use24Hour = true))
    }

    @Test
    fun supportsExistingAmPmSuffixesAndLocalizesTheMarker() {
        assertEquals("00:05", formatPrayerTime("12:05 AM", AppLanguage.EN, use24Hour = true))
        assertEquals("12:05 PM", formatPrayerTime("12:05 PM", AppLanguage.EN, use24Hour = false))
        assertEquals("2:49 مساءً", formatPrayerTime("14:49", AppLanguage.AR, use24Hour = false))
        assertEquals("2:49 د.ن", formatPrayerTime("14:49", AppLanguage.CKB, use24Hour = false))
        assertEquals("2:49 پ.ن", formatPrayerTime("02:49", AppLanguage.CKB, use24Hour = false))
    }

    @Test
    fun preservesUnavailableOrInvalidSourceTimeInsteadOfInventingOne() {
        assertEquals("time unavailable", formatPrayerTime("time unavailable", AppLanguage.EN, use24Hour = false))
        assertEquals("", formatPrayerTime("", AppLanguage.EN, use24Hour = false))
        assertEquals("25:80", formatPrayerTime("25:80", AppLanguage.EN, use24Hour = false))
        assertEquals("13:20 PM", formatPrayerTime("13:20 PM", AppLanguage.EN, use24Hour = false))
    }
}
