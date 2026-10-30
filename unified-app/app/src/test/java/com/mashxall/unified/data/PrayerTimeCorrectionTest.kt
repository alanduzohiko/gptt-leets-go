package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SalawatiPrayerSelectionTest {
    private fun row(city: String, source: String, fajr: String = "04:51") = PrayerRow(
        city = city,
        date = "10-23",
        fajr = fajr,
        sunrise = "06:12",
        dhuhr = "11:49",
        asr = "14:49",
        maghrib = "17:21",
        isha = "18:36",
        source = source
    )

    @Test
    fun onlyValidSalawatiRowsAreAccepted() {
        assertNotNull(ContentRepository.salawatiOnlyPrayerTimes(row("Sulaymaniyah", "salawati:Sulaymaniyah, Iraq")))
        assertNotNull(ContentRepository.salawatiOnlyPrayerTimes(row("Tasluja", "salawati-dependent:Tasluja → Sulaymaniyah, Iraq")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(null))
    }

    @Test
    fun rejectsNoorCorrectionAstronomicalAndUnknownSources() {
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Erbil", "noor-base:Erbil")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Erbil", "noor-correction:Erbil (+2 min)")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Erbil", "astronomical:36.19,44.00")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Erbil", "unknown")))
    }

    @Test
    fun rejectsIncompleteOrInvalidSalawatiClockValues() {
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Sulaymaniyah", "salawati:Sulaymaniyah", fajr = "25:61")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("Sulaymaniyah", "salawati:Sulaymaniyah", fajr = "")))
    }

    @Test
    fun gpsCandidatesPreferTheNearestValidSalawatiLocation() {
        val farther = row("Sulaymaniyah", "salawati:Sulaymaniyah, Iraq")
        val nearer = row("Tasluja", "salawati:Tasluja, Iraq")
        assertEquals(nearer, ContentRepository.orderPrayerCandidates(listOf(farther to 3.4, nearer to 0.8)).first().first)
    }

    @Test
    fun directTimetableWinsOverDependentAliasAtEqualDistance() {
        val direct = row("Erbil", "salawati:Erbil, Iraq")
        val dependent = row("Kalak", "salawati-dependent:Kalak → Erbil, Iraq")
        assertEquals(direct, ContentRepository.orderPrayerCandidates(listOf(dependent to 0.0, direct to 0.0)).first().first)
    }
}
