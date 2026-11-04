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
    fun paperAngleStartsFlatAndEndsWithItsPrintedReverseFace() {
        assertEquals(0f, mushafCurlSurfaceAngle(0f, -1f, 0.5f, 0.5f), 0.001f)
        assertEquals(Math.PI.toFloat(), mushafCurlSurfaceAngle(1f, -1f, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, mushafCurlSurfaceAngle(0f, 1f, 0.5f, 0.5f), 0.001f)
        assertEquals(Math.PI.toFloat(), mushafCurlSurfaceAngle(1f, 1f, 0.5f, 0.5f), 0.001f)
    }

    @Test
    fun paperProjectionStaysInPlaceWhenFlatAndMirrorsWhenTurnedOver() {
        assertEquals(250f, mushafCurlProjectedX(1000f, 250f, 0f, -1f), 0.01f)
        assertEquals(750f, mushafCurlProjectedX(1000f, 250f, 1f, -1f), 0.01f)
        assertEquals(250f, mushafCurlProjectedX(1000f, 750f, 1f, 1f), 0.01f)
    }

    @Test
    fun faceBoundaryMovesDiagonallyAroundTheFingerAnchor() {
        val top = mushafCurlFaceBoundaryX(1000f, 1600f, 0.1f, 0.5f, -1f, 0.5f)
        val center = mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 0.5f, -1f, 0.5f)
        val bottom = mushafCurlFaceBoundaryX(1000f, 1600f, 0.9f, 0.5f, -1f, 0.5f)
        assertTrue(top > center)
        assertTrue(bottom < center)
        assertEquals(1000f, mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 0f, -1f), 0.01f)
        assertEquals(0f, mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 1f, -1f), 0.01f)
    }



}
