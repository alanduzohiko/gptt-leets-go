package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafSwipeNavigationTest {
    @Test
    fun rightSwipeAdvancesAndLeftSwipeReturnsInReadingOrder() {
        // From Al-Fatiha, the reader moves a finger to the right to reveal page 2.
        assertEquals(3, mushafSwipeTarget(page = 2, horizontalDragPx = 120f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = -120f))
        assertEquals(2, mushafSwipeTarget(page = 1, horizontalDragPx = 120f))
        assertEquals(3, mushafSwipeTarget(page = 2, horizontalDragPx = 150f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = -150f))
    }

    @Test
    fun shortGesturesDoNotTurnThePage() {
        assertEquals(null, mushafSwipeTarget(page = 24, horizontalDragPx = 72f))
        assertEquals(null, mushafSwipeTarget(page = 24, horizontalDragPx = -35f))
    }

    @Test
    fun swipeThresholdScalesWithViewportAndKeepsASafeMinimum() {
        assertEquals(72f, mushafSwipeThresholdPx(320f), 0.001f)
        assertEquals(160f, mushafSwipeThresholdPx(1000f), 0.001f)
        assertEquals(72f, mushafSwipeThresholdPx(Float.NaN), 0.001f)
        assertEquals(72f, mushafSwipeThresholdPx(0f), 0.001f)
    }

    @Test
    fun pageBoundariesAndInvalidInputsAreSafe() {
        assertEquals(null, mushafSwipeTarget(page = 1, horizontalDragPx = -160f))
        assertEquals(null, mushafSwipeTarget(page = 604, horizontalDragPx = 160f))
        assertEquals(null, mushafSwipeTarget(page = 0, horizontalDragPx = 160f))
        assertEquals(null, mushafSwipeTarget(page = 20, horizontalDragPx = Float.NaN))
    }

    @Test
    fun dragProgressTracksFingerDistanceInEitherDirection() {
        assertEquals(0f, mushafCurlDragProgress(0f, 700f), 0.001f)
        assertEquals(0.5f, mushafCurlDragProgress(350f, 700f), 0.001f)
        assertEquals(0.5f, mushafCurlDragProgress(-350f, 700f), 0.001f)
        assertEquals(0.985f, mushafCurlDragProgress(900f, 700f), 0.001f)
        assertEquals(0f, mushafCurlDragProgress(Float.NaN, 700f), 0.001f)
    }

    @Test
    fun rightSwipeAdvanceUsesRightwardPeelAndLeftSwipeBackUsesLeftwardPeel() {
        // Pointer delta: positive means the finger physically travelled right.
        assertEquals(2, mushafSwipeTarget(page = 1, horizontalDragPx = 140f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = -140f))
        // Advancing peels from the left edge toward the right; returning peels right-to-left.
        assertEquals(-1f, mushafCurlDirectionForPageTransition(1, 2), 0.001f)
        assertEquals(1f, mushafCurlDirectionForPageTransition(2, 1), 0.001f)
        assertEquals(0f, mushafCurlDirectionForPageTransition(2, 2), 0.001f)
    }

    @Test
    fun forwardCurlMovesLeftToRightAndCreatesADiagonalCrease() {
        assertEquals(0f, mushafCurlSeamX(1000f, 0.5f, -1f, 0f), 0.001f)
        assertEquals(1000f, mushafCurlSeamX(1000f, 0.5f, -1f, 1f), 0.001f)
        assertEquals(1000f, mushafCurlBoundaryX(1000f, 0.5f, -1f, 1f), 0.001f)
        assertTrue(mushafCurlSeamX(1000f, 0.1f, -1f, 0.5f) <
            mushafCurlSeamX(1000f, 0.9f, -1f, 0.5f))
    }

    @Test
    fun backwardCurlMovesRightToLeftInTheOppositeDirection() {
        assertEquals(1000f, mushafCurlSeamX(1000f, 0.5f, 1f, 0f), 0.001f)
        assertEquals(0f, mushafCurlSeamX(1000f, 0.5f, 1f, 1f), 0.001f)
        assertEquals(0f, mushafCurlBoundaryX(1000f, 0.5f, 1f, 1f), 0.001f)
        assertTrue(mushafCurlSeamX(1000f, 0.1f, 1f, 0.5f) >
            mushafCurlSeamX(1000f, 0.9f, 1f, 0.5f))
    }

    @Test
    fun curlBandGrowsInTheMiddleAndCollapsesAtTheEnds() {
        assertEquals(0f, mushafCurlBandWidth(1000f, 0f), 0.001f)
        assertEquals(280f, mushafCurlBandWidth(1000f, 0.5f), 0.001f)
        assertEquals(0f, mushafCurlBandWidth(1000f, 1f), 0.001f)
    }

    @Test
    fun printedPageWarpsOnlyInsideTheMovingCurlBand() {
        assertEquals(250f, mushafCurlWarpX(250f, 300f, 100f, 1f), 0.001f)
        assertEquals(326f, mushafCurlWarpX(350f, 300f, 100f, 1f), 0.01f)
        assertEquals(400f, mushafCurlWarpX(400f, 300f, 100f, 1f), 0.001f)
        assertEquals(374f, mushafCurlWarpX(350f, 400f, 100f, -1f), 0.01f)
    }

}
