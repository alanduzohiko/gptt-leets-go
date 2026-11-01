package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimelineMarkerTest {
    @Test
    fun markersUseTheirActualPositionBetweenFajrAndIsha() {
        val fajr = 100L
        val isha = 300L
        assertEquals(0f, prayerTimelineMarkerFraction(fajr, fajr, isha), 0.0001f)
        assertEquals(0.25f, prayerTimelineMarkerFraction(150L, fajr, isha), 0.0001f)
        assertEquals(0.75f, prayerTimelineMarkerFraction(250L, fajr, isha), 0.0001f)
        assertEquals(1f, prayerTimelineMarkerFraction(isha, fajr, isha), 0.0001f)
    }

    @Test
    fun markerPositionIsClampedOutsideThePrayerWindow() {
        assertEquals(0f, prayerTimelineMarkerFraction(50L, 100L, 300L), 0.0001f)
        assertEquals(1f, prayerTimelineMarkerFraction(350L, 100L, 300L), 0.0001f)
    }

    @Test
    fun invalidOrReversedPrayerWindowDoesNotCreateProgress() {
        assertEquals(0f, prayerTimelineMarkerFraction(150L, 300L, 100L), 0.0001f)
        assertEquals(0f, prayerTimelineMarkerFraction(150L, 100L, 100L), 0.0001f)
    }
}
