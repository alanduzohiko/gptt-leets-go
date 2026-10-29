package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafEdgeTapActionTest {
    @Test
    fun physicalLeftSwipeAdvancesToTheNextPage() {
        assertEquals(2, mushafSwipeTarget(page = 1, horizontalDragPx = -100f))
        assertEquals(81, mushafSwipeTarget(page = 80, horizontalDragPx = -100f))
    }

    @Test
    fun physicalRightSwipeReturnsToThePreviousPage() {
        assertEquals(1, mushafSwipeTarget(page = 2, horizontalDragPx = 100f))
        assertEquals(79, mushafSwipeTarget(page = 80, horizontalDragPx = 100f))
    }

    @Test
    fun smallSwipesAndOutOfBoundsPageTurnsAreIgnored() {
        assertEquals(null, mushafSwipeTarget(page = 10, horizontalDragPx = -40f))
        assertEquals(null, mushafSwipeTarget(page = 1, horizontalDragPx = 100f))
        assertEquals(null, mushafSwipeTarget(page = 604, horizontalDragPx = -100f))
        assertEquals(null, mushafSwipeTarget(page = 0, horizontalDragPx = -100f))
        assertEquals(null, mushafSwipeTarget(page = 80, horizontalDragPx = Float.NaN))
    }

    @Test
    fun leftPageMarginAdvancesToTheNextPage() {
        assertEquals(
            MushafEdgeTapAction.NEXT_PAGE,
            mushafEdgeTapAction(xFraction = 0.03f, page = 80, scale = 1f, verseWasHit = false)
        )
    }

    @Test
    fun rightPageMarginReturnsToThePreviousPage() {
        assertEquals(
            MushafEdgeTapAction.PREVIOUS_PAGE,
            mushafEdgeTapAction(xFraction = 0.97f, page = 80, scale = 1f, verseWasHit = false)
        )
    }

    @Test
    fun centerTapRemainsAvailableForPageContent() {
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 0.50f, page = 80, scale = 1f, verseWasHit = false)
        )
    }

    @Test
    fun edgeTapNeverOverridesARealAyahHit() {
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 0.02f, page = 80, scale = 1f, verseWasHit = true)
        )
    }

    @Test
    fun edgeTapIsDisabledWhileZoomed() {
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 0.02f, page = 80, scale = 2f, verseWasHit = false)
        )
    }

    @Test
    fun edgeTapCannotGoBeyondTheFirstOrLastMushafPage() {
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 0.98f, page = 1, scale = 1f, verseWasHit = false)
        )
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 0.02f, page = 604, scale = 1f, verseWasHit = false)
        )
    }

    @Test
    fun invalidFractionsAreIgnored() {
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = Float.NaN, page = 80, scale = 1f, verseWasHit = false)
        )
        assertEquals(
            MushafEdgeTapAction.NONE,
            mushafEdgeTapAction(xFraction = 1.5f, page = 80, scale = 1f, verseWasHit = false)
        )
    }
}
