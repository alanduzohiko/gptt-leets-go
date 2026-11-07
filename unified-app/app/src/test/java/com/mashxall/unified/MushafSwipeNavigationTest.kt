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
    fun curlPreviewUsesOnlyTheAdjacentPageAndNeverSkipsAhead() {
        assertEquals(2, mushafCurlAdjacentPage(page = 1, forward = true))
        assertEquals(1, mushafCurlAdjacentPage(page = 2, forward = false))
        assertEquals(604, mushafCurlAdjacentPage(page = 603, forward = true))
        assertEquals(null, mushafCurlAdjacentPage(page = 604, forward = true))
        assertEquals(null, mushafCurlAdjacentPage(page = 1, forward = false))
        assertEquals(null, mushafCurlAdjacentPage(page = 0, forward = true))
    }

    @Test
    fun rightSwipeAdvanceUsesRightwardPeelAndLeftSwipeBackUsesLeftwardPeel() {
        // Pointer delta: positive means the finger physically travelled right.
        assertEquals(2, mushafSwipeTarget(page = 1, horizontalDragPx = 140f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = -140f))
        // Advancing peels from the left edge toward the right; returning peels right-to-left.
        assertEquals(1f, mushafCurlDirectionForPageTransition(1, 2), 0.001f)
        assertEquals(-1f, mushafCurlDirectionForPageTransition(2, 1), 0.001f)
        assertEquals(0f, mushafCurlDirectionForPageTransition(2, 2), 0.001f)
    }

    @Test
    fun paperAngleStartsFlatAndEndsWithItsPrintedReverseFace() {
        // Sample the outer portion of each leaf: the wider curl radius keeps
        // the page centre inside the smooth bend until the turn is complete.
        assertEquals(0f, mushafCurlSurfaceAngle(0f, -1f, 0.8f, 0.5f), 0.001f)
        assertEquals(Math.PI.toFloat(), mushafCurlSurfaceAngle(1f, -1f, 0.8f, 0.5f), 0.001f)
        assertEquals(0f, mushafCurlSurfaceAngle(0f, 1f, 0.2f, 0.5f), 0.001f)
        assertEquals(Math.PI.toFloat(), mushafCurlSurfaceAngle(1f, 1f, 0.2f, 0.5f), 0.001f)
    }

    @Test
    fun paperProjectionFollowsTheSameDirectionAsTheFinger() {
        // A rightward advance leaves the left edge flat first, then carries
        // the turned sheet off the right edge. Returning mirrors that path.
        assertEquals(250f, mushafCurlProjectedX(1000f, 250f, 0f, 1f), 0.01f)
        assertEquals(750f, mushafCurlProjectedX(1000f, 750f, 0f, -1f), 0.01f)

        val foldStart = mushafCurlFoldPositionFraction(0f, 1f, 0.5f)
        val foldEnd = mushafCurlFoldPositionFraction(1f, 1f, 0.5f)
        assertEquals(0f, foldStart, 0.001f)
        assertEquals(1f, foldEnd, 0.001f)

        val curlMiddle = mushafCurlProjectedX(1000f, 250f, 0.5f, 1f)
        assertTrue(curlMiddle > 550f)

        val turnedRight = mushafCurlProjectedX(1000f, 400f, 1f, 1f)
        val turnedLeft = mushafCurlProjectedX(1000f, 600f, 1f, -1f)
        assertTrue(turnedRight > 1000f)
        assertTrue(turnedLeft < 0f)
    }

    @Test
    fun faceBoundaryMovesDiagonallyButDoesNotTwistAcrossTheWholePage() {
        val top = mushafCurlFaceBoundaryX(1000f, 1600f, 0.1f, 0.5f, -1f, 0.5f)
        val center = mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 0.5f, -1f, 0.5f)
        val bottom = mushafCurlFaceBoundaryX(1000f, 1600f, 0.9f, 0.5f, -1f, 0.5f)
        assertTrue(top > center)
        assertTrue(bottom < center)
        assertTrue(top - bottom < 300f)
        assertEquals(1000f, mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 0f, -1f), 0.01f)
        val finalFold = mushafCurlFaceBoundaryX(1000f, 1600f, 0.5f, 1f, -1f)
        assertTrue(finalFold in 370f..415f)
    }

    @Test
    fun curledLeafHasAVisibleBroadSurfaceRatherThanAThinEdgeOnStripe() {
        val angle = mushafCurlSurfaceAngle(
            progress = 0.5f,
            direction = -1f,
            xFraction = 0.75f,
            yFraction = 0.5f
        )
        assertEquals(1f, angle, 0.01f)
    }

    @Test
    fun paperRadiusRespondsToTheFingerAnchorAndStaysBounded() {
        val nearFinger = mushafCurlRadiusFraction(yFraction = 0.5f, anchorYFraction = 0.5f)
        val farEdge = mushafCurlRadiusFraction(yFraction = 0f, anchorYFraction = 0.5f)
        assertTrue(nearFinger > farEdge)
        assertTrue(nearFinger in 0.24f..0.25f)
        assertTrue(farEdge in 0.20f..0.22f)
        assertEquals(0.21f, mushafCurlRadiusFraction(Float.NaN), 0.001f)
    }

    @Test
    fun cylinderAngleIsLocalToTheMovingFoldNotTheWholeFlatPage() {
        assertEquals(0f, mushafCurlSurfaceAngle(0f, -1f, 0.5f, 0.5f), 0.001f)
        assertEquals(0f, mushafCurlSurfaceAngle(0f, 1f, 0.5f, 0.5f), 0.001f)

        val flatSide = mushafCurlSurfaceAngle(0.5f, -1f, 0.1f, 0.5f)
        val curledSide = mushafCurlSurfaceAngle(0.5f, -1f, 0.7f, 0.5f)
        assertEquals(0f, flatSide, 0.001f)
        assertTrue(curledSide > 0f)
        assertTrue(curledSide <= Math.PI.toFloat())

        assertEquals(
            0.5f,
            mushafCurlFoldPositionFraction(0.5f, -1f, 0.5f, 0.5f),
            0.001f
        )
    }



}
