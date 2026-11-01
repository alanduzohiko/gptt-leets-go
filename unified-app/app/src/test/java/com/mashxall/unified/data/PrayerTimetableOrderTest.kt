package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimetableOrderTest {
    private fun row(city: String, source: String) = PrayerRow(
        city = city,
        date = "01-01",
        fajr = "05:00",
        sunrise = "06:30",
        dhuhr = "12:00",
        asr = "15:00",
        maghrib = "17:30",
        isha = "19:00",
        source = source
    )

    @Test
    fun nearestTimetableWinsEvenIfTheFartherRowIsDirect() {
        val near = row("Sulaymaniyah", "salawati:Sulaymaniyah, Iraq") to 0.4
        val far = row("Erbil", "salawati:Erbil, Iraq") to 3.5
        assertEquals("Sulaymaniyah", ContentRepository.orderPrayerCandidates(listOf(far, near)).first().first.city)
    }

    @Test
    fun directlyScheduledRowWinsWhenAdependentCityHasTheSameDistance() {
        val dependent = row("Locality", "salawati-dependent:Locality → Base city, Iraq") to 0.0
        val direct = row("Base city", "salawati:Base city, Iraq") to 0.0
        assertEquals("salawati:Base city, Iraq", ContentRepository.orderPrayerCandidates(listOf(dependent, direct)).first().first.source)
    }
}
