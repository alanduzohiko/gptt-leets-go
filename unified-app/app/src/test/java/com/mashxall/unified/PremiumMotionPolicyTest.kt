package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumMotionPolicyTest {
    @Test
    fun staggerDelayStartsImmediatelyAndCapsTheCascade() {
        assertEquals(0L, staggerDelayMillis(0))
        assertEquals(28L, staggerDelayMillis(1))
        assertEquals(112L, staggerDelayMillis(4))
        assertEquals(224L, staggerDelayMillis(8))
        assertEquals(224L, staggerDelayMillis(99))
    }

    @Test
    fun negativePositionsNeverAddAnUnexpectedDelay() {
        assertEquals(0L, staggerDelayMillis(-1))
    }
}
