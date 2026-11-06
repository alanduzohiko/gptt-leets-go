package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyAyahHadithSwipeTest {
    @Test
    fun rightSwipeFromDailyAyahOpensHadith() {
        assertEquals(true, dailyAyahHadithSwipeTarget(showingHadith = false, horizontalDragPx = 110f))
    }

    @Test
    fun leftSwipeFromDailyHadithReturnsToAyah() {
        assertEquals(false, dailyAyahHadithSwipeTarget(showingHadith = true, horizontalDragPx = -110f))
    }

    @Test
    fun oppositeDirectionsDoNotChangeTheCurrentCard() {
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = false, horizontalDragPx = -110f))
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = true, horizontalDragPx = 110f))
    }

    @Test
    fun shortAndInvalidGesturesAreIgnored() {
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = false, horizontalDragPx = 72f))
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = true, horizontalDragPx = -72f))
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = false, horizontalDragPx = Float.NaN))
        assertNull(dailyAyahHadithSwipeTarget(showingHadith = false, horizontalDragPx = Float.POSITIVE_INFINITY))
    }
}
