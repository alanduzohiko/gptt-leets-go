package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranSurahSwipeTest {
    @Test
    fun physicalLeftSwipeAdvancesToNextSurahInRightToLeftReadingOrder() {
        assertEquals(2, quranSurahSwipeTarget(1, -110f))
        assertEquals(114, quranSurahSwipeTarget(113, -110f))
    }

    @Test
    fun physicalRightSwipeReturnsToPreviousSurah() {
        assertEquals(1, quranSurahSwipeTarget(2, 110f))
        assertEquals(113, quranSurahSwipeTarget(114, 110f))
    }

    @Test
    fun resumesTheLastReaderModeInsteadOfAlwaysForcingPageMushaf() {
        assertEquals(true, shouldRestorePageMushaf(null, true))
        assertEquals(true, shouldRestorePageMushaf("mushaf", true))
        assertEquals(false, shouldRestorePageMushaf("verse", true))
        assertEquals(false, shouldRestorePageMushaf("verse", false))
        assertEquals(false, shouldRestorePageMushaf("mushaf", false))
    }

    @Test
    fun ignoresShortGesturesAndSurahBoundaries() {
        assertNull(quranSurahSwipeTarget(1, 110f))
        assertNull(quranSurahSwipeTarget(114, -110f))
        assertNull(quranSurahSwipeTarget(10, -25f))
        assertNull(quranSurahSwipeTarget(0, -120f))
        assertNull(quranSurahSwipeTarget(12, Float.NaN))
    }
}
