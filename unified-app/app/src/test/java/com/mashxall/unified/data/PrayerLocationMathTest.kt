package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerLocationMathTest {
    private fun prayerRow(city: String, source: String) = PrayerRow(
        city = city,
        date = "10-29",
        fajr = "04:40",
        sunrise = "06:05",
        dhuhr = "11:55",
        asr = "15:10",
        maghrib = "17:32",
        isha = "18:58",
        source = source,
        latitude = 35.56,
        longitude = 45.43
    )

    @Test
    fun gpsFallbackUsesNearestSalawatiSourceAndRejectsNearerUntrustedRow() {
        val valid = prayerRow("Sulaymaniyah", "salawati:Sulaymaniyah, Iraq")
        val untrusted = prayerRow("Other", "noor-base:Other")
        val result = ContentRepository.nearestValidSalawatiCandidate(
            listOf(valid to 18.4, untrusted to 1.1), maxDistanceKm = 30.0
        )
        assertEquals(valid, result?.first)
        assertEquals(18.4, result?.second ?: -1.0, 0.0001)
    }

    @Test
    fun gpsFallbackDoesNotUseSchedulesBeyondConfiguredRadius() {
        val far = prayerRow("Erbil", "salawati:Erbil, Iraq")
        assertNull(ContentRepository.nearestValidSalawatiCandidate(listOf(far to 42.0), 30.0))
        assertNull(ContentRepository.nearestValidSalawatiCandidate(listOf(far to 8.0), 7.0))
    }

    @Test
    fun gpsStillChoosesTheClosestValidSourceInsideThePreciseRadius() {
        val near = prayerRow("Tasluja", "salawati:Tasluja, Iraq")
        val farther = prayerRow("Sulaymaniyah", "salawati:Sulaymaniyah, Iraq")
        val result = ContentRepository.nearestValidSalawatiCandidate(
            listOf(farther to 4.2, near to 1.3), maxDistanceKm = 7.0
        )
        assertEquals(near, result?.first)
    }

    @Test
    fun distanceToSameCoordinateIsZero() {
        assertEquals(0.0, PrayerLocationMath.distanceKm(35.56, 45.43, 35.56, 45.43), 0.000001)
    }

    @Test
    fun oneDegreeOfLongitudeAtTheEquatorUsesGreatCircleDistance() {
        assertEquals(111.195, PrayerLocationMath.distanceKm(0.0, 0.0, 0.0, 1.0), 0.01)
    }

    @Test
    fun distanceIsSymmetric() {
        val forward = PrayerLocationMath.distanceKm(35.56, 45.43, 36.19, 44.01)
        val reverse = PrayerLocationMath.distanceKm(36.19, 44.01, 35.56, 45.43)
        assertTrue(forward > 0.0)
        assertEquals(forward, reverse, 0.000001)
    }
}
