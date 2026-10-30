package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafSwipeNavigationTest {
    @Test
    fun leftSwipeAdvancesAndRightSwipeReturnsInReadingOrder() {
        assertEquals(3, mushafSwipeTarget(page = 2, horizontalDragPx = -120f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = 120f))
    }

    @Test
    fun shortGesturesDoNotTurnThePage() {
        assertEquals(null, mushafSwipeTarget(page = 24, horizontalDragPx = -72f))
        assertEquals(null, mushafSwipeTarget(page = 24, horizontalDragPx = 35f))
    }

    @Test
    fun pageBoundariesAndInvalidInputsAreSafe() {
        assertEquals(null, mushafSwipeTarget(page = 1, horizontalDragPx = 160f))
        assertEquals(null, mushafSwipeTarget(page = 604, horizontalDragPx = -160f))
        assertEquals(null, mushafSwipeTarget(page = 0, horizontalDragPx = -160f))
        assertEquals(null, mushafSwipeTarget(page = 20, horizontalDragPx = Float.NaN))
    }
}
