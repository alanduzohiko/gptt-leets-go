package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaHeadingTest {
    @Test
    fun positiveDeclinationConvertsMagneticNorthToTrueNorth() {
        // The magnetic heading is 90° and local declination is +10° east.
        // Its true-north heading is 100°, exactly on a 100° Qibla bearing.
        assertEquals(0f, qiblaRelativeHeading(100f, 90f, 10f), 0.001f)
    }

    @Test
    fun negativeDeclinationIsAppliedInTheOppositeDirection() {
        // 20° magnetic heading with -15° declination is a 5° true heading.
        assertEquals(5f, qiblaRelativeHeading(10f, 20f, -15f), 0.001f)
    }

    @Test
    fun shortestTurnAcrossNorthIsPositiveRatherThanAlmostAFullCircle() {
        // 350° magnetic + 15° declination = 5° true heading; target is 10°.
        assertEquals(5f, qiblaRelativeHeading(10f, 350f, 15f), 0.001f)
    }

    @Test
    fun shortestTurnAcrossZeroCanAlsoBeNegative() {
        assertEquals(-5f, qiblaRelativeHeading(350f, 5f, 0f), 0.001f)
    }
}
