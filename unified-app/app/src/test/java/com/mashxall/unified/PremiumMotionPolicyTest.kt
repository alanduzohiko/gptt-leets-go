package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumMotionPolicyTest {
    @Test
    fun staggerDelayStartsImmediatelyAndCapsTheCascade() {
        assertEquals(0L, staggerDelayMillis(0))
        assertEquals(40L, staggerDelayMillis(1))
        assertEquals(160L, staggerDelayMillis(4))
        assertEquals(320L, staggerDelayMillis(8))
        assertEquals(320L, staggerDelayMillis(99))
    }

    @Test
    fun negativePositionsNeverAddAnUnexpectedDelay() {
        assertEquals(0L, staggerDelayMillis(-1))
    }
}
