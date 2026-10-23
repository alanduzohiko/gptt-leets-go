package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimeCorrectionTest {
    @Test
    fun appliesPositiveMinuteOffset() {
        assertEquals("05:12", ContentRepository.addMinutesToPrayerTime("05:10", 2))
    }

    @Test
    fun appliesNegativeMinuteOffset() {
        assertEquals("05:08", ContentRepository.addMinutesToPrayerTime("05:10", -2))
    }

    @Test
    fun wrapsForwardAcrossMidnight() {
        assertEquals("00:01", ContentRepository.addMinutesToPrayerTime("23:59", 2))
    }

    @Test
    fun wrapsBackwardAcrossMidnight() {
        assertEquals("23:59", ContentRepository.addMinutesToPrayerTime("00:01", -2))
    }

    @Test
    fun leavesSourceStringUntouchedForZeroOrInvalidValues() {
        assertEquals("05:10", ContentRepository.addMinutesToPrayerTime("05:10", 0))
        assertEquals("time unavailable", ContentRepository.addMinutesToPrayerTime("time unavailable", 2))
    }
}
