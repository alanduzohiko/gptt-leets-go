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

    @Test
    fun prefersTheSelectedSalawatiCityScheduleOverNoorWhenBothExist() {
        val salawati = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "04:51", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "salawati:Sulaymaniyah, Iraq"
        )
        val noor = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "04:51", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "noor:Sulaymaniyah"
        )
        assertEquals(salawati, ContentRepository.preferredSourcePrayerTimes(salawati, noor))
    }

    @Test
    fun fallsBackToNoorWhenSalawatiHasNoMatchingSchedule() {
        val noor = PrayerRow(
            city = "Halabja", date = "10-23",
            fajr = "04:46", sunrise = "06:16", dhuhr = "11:46",
            asr = "14:50", maghrib = "17:17", isha = "18:25",
            source = "noor:Halabja"
        )
        assertEquals(noor, ContentRepository.preferredSourcePrayerTimes(null, noor))
        assertEquals(null, ContentRepository.preferredSourcePrayerTimes(null, null))
    }

    @Test
    fun gpsTimetableUsesNearestRowButPrefersSalawatiOnAnExactDistanceTie() {
        val salawati = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "04:51", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "salawati:Sulaymaniyah, Iraq"
        )
        val noor = salawati.copy(source = "noor:Sulaymaniyah")
        val candidates = listOf(noor to 0.0, salawati to 0.0)
        assertEquals(salawati, ContentRepository.orderPrayerCandidates(candidates).first().first)
        assertEquals(noor, ContentRepository.orderPrayerCandidates(listOf(noor to 0.1, salawati to 0.3)).first().first)
    }
}
