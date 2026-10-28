package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerLocationMathTest {
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
