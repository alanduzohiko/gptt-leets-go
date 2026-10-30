package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfSwipeNavigationTest {
    @Test
    fun leftSwipeAdvancesAndRightSwipeReturns() {
        assertEquals(8, pdfSwipeTarget(pageIndex = 7, pageCount = 20, horizontalDragPx = -110f))
        assertEquals(6, pdfSwipeTarget(pageIndex = 7, pageCount = 20, horizontalDragPx = 110f))
    }

    @Test
    fun shortGesturesDoNotTurnThePage() {
        assertEquals(null, pdfSwipeTarget(pageIndex = 5, pageCount = 20, horizontalDragPx = -72f))
        assertEquals(null, pdfSwipeTarget(pageIndex = 5, pageCount = 20, horizontalDragPx = 24f))
    }

    @Test
    fun firstAndLastPagesRespectBounds() {
        assertEquals(null, pdfSwipeTarget(pageIndex = 0, pageCount = 10, horizontalDragPx = 150f))
        assertEquals(null, pdfSwipeTarget(pageIndex = 9, pageCount = 10, horizontalDragPx = -150f))
    }

    @Test
    fun invalidInputsAreIgnored() {
        assertEquals(null, pdfSwipeTarget(pageIndex = -1, pageCount = 10, horizontalDragPx = -150f))
        assertEquals(null, pdfSwipeTarget(pageIndex = 0, pageCount = 0, horizontalDragPx = -150f))
        assertEquals(null, pdfSwipeTarget(pageIndex = 2, pageCount = 10, horizontalDragPx = Float.NaN))
    }
}
