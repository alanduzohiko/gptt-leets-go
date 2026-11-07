package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerJourneyProgressTest {
    @Test
    fun progressIsZeroAtFajr() {
        assertEquals(0f, prayerJourneyProgressFraction(100L, 100L, 200L), 0.0001f)
    }

    @Test
    fun progressIsHalfwayBetweenFajrAndIsha() {
        assertEquals(0.5f, prayerJourneyProgressFraction(150L, 100L, 200L), 0.0001f)
    }

    @Test
    fun progressIsClampedBeforeFajrAndAfterIsha() {
        assertEquals(0f, prayerJourneyProgressFraction(50L, 100L, 200L), 0.0001f)
        assertEquals(1f, prayerJourneyProgressFraction(250L, 100L, 200L), 0.0001f)
    }

    @Test
    fun invalidPrayerWindowDoesNotCreateProgress() {
        assertEquals(0f, prayerJourneyProgressFraction(150L, 200L, 100L), 0.0001f)
        assertEquals(0f, prayerJourneyProgressFraction(150L, 100L, 100L), 0.0001f)
    }
}
