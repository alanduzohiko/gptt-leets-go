package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class AzkarRepetitionTest {
    @Test
    fun incrementsOnceForEachTap() {
        assertEquals(1, nextAzkarRepetition(0, 33))
        assertEquals(2, nextAzkarRepetition(1, 33))
        assertEquals(17, nextAzkarRepetition(16, 33))
    }

    @Test
    fun capsAtTheSourceDefinedTarget() {
        assertEquals(33, nextAzkarRepetition(32, 33))
        assertEquals(33, nextAzkarRepetition(33, 33))
        assertEquals(33, nextAzkarRepetition(40, 33))
    }

    @Test
    fun normalizesInvalidCurrentAndTargetCountsSafely() {
        assertEquals(1, nextAzkarRepetition(-5, 5))
        assertEquals(0, nextAzkarRepetition(-3, 0))
        assertEquals(0, nextAzkarRepetition(-3, -1))
    }
}
