package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TasbihTapBehaviorTest {
    @Test
    fun advancesRoundAndTotalTogetherBeforeTarget() {
        val result = nextTasbihTap(roundCount = 7, totalCount = 107, targetCount = 33)
        assertEquals(8, result.roundCount)
        assertEquals(108, result.totalCount)
        assertTrue(result.accepted)
    }

    @Test
    fun completesExactlyAtSourceTarget() {
        val result = nextTasbihTap(roundCount = 32, totalCount = 132, targetCount = 33)
        assertEquals(33, result.roundCount)
        assertEquals(133, result.totalCount)
        assertTrue(result.accepted)
    }

    @Test
    fun ignoresFurtherTapsAfterTargetWithoutChangingTheAllTimeTotal() {
        val result = nextTasbihTap(roundCount = 33, totalCount = 133, targetCount = 33)
        assertEquals(33, result.roundCount)
        assertEquals(133, result.totalCount)
        assertFalse(result.accepted)
    }

    @Test
    fun anUnspecifiedTargetAllowsCountingWithoutAFalseCap() {
        val result = nextTasbihTap(roundCount = 50, totalCount = 500, targetCount = 0)
        assertEquals(51, result.roundCount)
        assertEquals(501, result.totalCount)
        assertTrue(result.accepted)
    }

    @Test
    fun invalidStoredCountsAreNormalizedAndIntegerOverflowIsBlocked() {
        val normalized = nextTasbihTap(roundCount = -3, totalCount = -9, targetCount = 5)
        assertEquals(1, normalized.roundCount)
        assertEquals(1, normalized.totalCount)
        assertFalse(nextTasbihTap(Int.MAX_VALUE, Int.MAX_VALUE, 0).accepted)
    }
}
