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
    fun invalidSalawatiClockIsRejectedInsteadOfSubstitutingAnotherSource() {
        val invalidSalawati = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "25:61", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "salawati:Sulaymaniyah, Iraq"
        )
        assertEquals(null, ContentRepository.salawatiOnlyPrayerTimes(invalidSalawati))
    }

    @Test
    fun onlyValidSalawatiRowsAreAccepted() {
        val salawati = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "04:51", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "salawati:Sulaymaniyah, Iraq"
        )
        val otherSource = salawati.copy(source = "noor:Sulaymaniyah")
        assertEquals(salawati, ContentRepository.salawatiOnlyPrayerTimes(salawati))
        assertEquals(null, ContentRepository.salawatiOnlyPrayerTimes(otherSource))
        assertEquals(null, ContentRepository.salawatiOnlyPrayerTimes(null))
    }

    @Test
    fun gpsCandidateOrderingUsesNearestSalawatiLocation() {
        val farther = PrayerRow(
            city = "Sulaymaniyah", date = "10-23",
            fajr = "04:51", sunrise = "06:12", dhuhr = "11:49",
            asr = "14:49", maghrib = "17:21", isha = "18:36",
            source = "salawati:Sulaymaniyah, Iraq"
        )
        val nearer = farther.copy(city = "Tasluja", source = "salawati:Tasluja, Iraq")
        val candidates = listOf(farther to 3.4, nearer to 0.8)
        assertEquals(nearer, ContentRepository.orderPrayerCandidates(candidates).first().first)
    }
}
