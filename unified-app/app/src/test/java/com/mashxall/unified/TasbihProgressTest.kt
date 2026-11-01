package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class TasbihProgressTest {
    @Test
    fun progressTracksTheSelectedSourceTarget() {
        assertEquals(0f, tasbihProgressFraction(0, 33), 0.0001f)
        assertEquals(0.5f, tasbihProgressFraction(33, 66), 0.0001f)
        assertEquals(1f, tasbihProgressFraction(33, 33), 0.0001f)
    }

    @Test
    fun progressIsBoundedEvenWhenCounterContinuesPastTheTarget() {
        assertEquals(1f, tasbihProgressFraction(40, 33), 0.0001f)
        assertEquals(0f, tasbihProgressFraction(-2, 33), 0.0001f)
    }

    @Test
    fun missingOrNonPositiveTargetDoesNotProduceInvalidDrawingValues() {
        assertEquals(0f, tasbihProgressFraction(5, 0), 0.0001f)
        assertEquals(0f, tasbihProgressFraction(5, -1), 0.0001f)
    }
}
