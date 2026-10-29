package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SalawatiOnlyPrayerTimesTest {
    private fun row(source: String, fajr: String = "04:35") = PrayerRow(
        city = "Erbil",
        date = "10-29",
        fajr = fajr,
        sunrise = "06:00",
        dhuhr = "12:00",
        asr = "15:20",
        maghrib = "17:45",
        isha = "19:05",
        source = source,
        latitude = 36.191196,
        longitude = 44.009962
    )

    @Test
    fun acceptsOnlyValidOriginalSalawatiTimetables() {
        assertNotNull(ContentRepository.salawatiOnlyPrayerTimes(row("salawati:Erbil, Iraq")))
        assertNotNull(ContentRepository.salawatiOnlyPrayerTimes(row("salawati-dependent:Harir → Erbil, Iraq")))
    }

    @Test
    fun rejectsNoorAndCalculatedOrUnknownTimeSources() {
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("noor-base:Erbil")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("noor-correction:Erbil (+2 min)")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("astronomical:36.19,44.00")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("unverified")))
    }

    @Test
    fun rejectsIncompleteOrInvalidSourceClockValues() {
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("salawati:Erbil", fajr = "")))
        assertNull(ContentRepository.salawatiOnlyPrayerTimes(row("salawati:Erbil", fajr = "29:75")))
    }

    @Test
    fun directSalawatiScheduleWinsOverDependentAliasAtSameDistance() {
        val direct = row("salawati:Erbil")
        val dependent = row("salawati-dependent:Kalak → Erbil")
        val ordered = ContentRepository.orderPrayerCandidates(listOf(dependent to 0.0, direct to 0.0))
        assertEquals(direct, ordered.first().first)
    }
}
