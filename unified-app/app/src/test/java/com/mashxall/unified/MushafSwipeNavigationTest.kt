package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafSwipeNavigationTest {
    @Test
    fun rightSwipeAdvancesAndLeftSwipeReturnsInReadingOrder() {
        // From Al-Fatiha, the reader moves a finger to the right to reveal page 2.
        assertEquals(3, mushafSwipeTarget(page = 2, horizontalDragPx = 120f))
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = -120f))
        assertEquals(2, mushafSwipeTarget(page = 1, horizontalDragPx = 120f))
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
    fun oneSheetCurlUsesTheCorrectHingeAndDirection() {
        assertEquals(0f, mushafPageCurlRotationDegrees(1f, 0f), 0.001f)
        assertEquals(84f, mushafPageCurlRotationDegrees(1f, 0.5f), 0.001f)
        assertEquals(168f, mushafPageCurlRotationDegrees(1f, 1f), 0.001f)
        assertEquals(-84f, mushafPageCurlRotationDegrees(-1f, 0.5f), 0.001f)
        assertEquals(0f, mushafPageCurlRotationDegrees(1f, Float.NaN), 0.001f)
        assertEquals(0f, mushafPageCurlRotationDegrees(0f, 0.8f), 0.001f)
        assertEquals(0.94f, mushafPageCurlPivotFraction(1f), 0.001f)
        assertEquals(0.06f, mushafPageCurlPivotFraction(-1f), 0.001f)
        assertEquals(0.5f, mushafPageCurlPivotFraction(0f), 0.001f)
    }

    @Test
    fun sheetFrontFadesBeforeQuranTextCanAppearMirrored() {
        assertEquals(1f, mushafPageCurlFrontFaceAlpha(1f, 0.45f), 0.001f)
        assertEquals(0.75f, mushafPageCurlFrontFaceAlpha(1f, 0.5f), 0.001f)
        assertEquals(0f, mushafPageCurlFrontFaceAlpha(1f, 0.54f), 0.001f)
        assertEquals(0.75f, mushafPageCurlFrontFaceAlpha(-1f, 0.5f), 0.001f)
    }
}
