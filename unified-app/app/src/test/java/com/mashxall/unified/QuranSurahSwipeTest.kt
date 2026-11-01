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


class TafsirSwipeNavigationTest {
    private val previous = 2 to 286
    private val current = 2 to 287
    private val next = 2 to 288

    @Test
    fun physicalLeftSwipeAdvancesToNextAyah() {
        assertEquals(next, tafsirSwipeTarget(-110f, previousVerse = previous, nextVerse = next, currentVerse = current))
    }

    @Test
    fun physicalRightSwipeReturnsToPreviousAyah() {
        assertEquals(previous, tafsirSwipeTarget(110f, previousVerse = previous, nextVerse = next, currentVerse = current))
    }

    @Test
    fun shortGesturesAndInvalidThresholdsAreIgnored() {
        assertNull(tafsirSwipeTarget(-24f, previousVerse = previous, nextVerse = next, currentVerse = current))
        assertNull(tafsirSwipeTarget(24f, previousVerse = previous, nextVerse = next, currentVerse = current))
        assertNull(tafsirSwipeTarget(-120f, thresholdPx = 0f, previousVerse = previous, nextVerse = next, currentVerse = current))
        assertNull(tafsirSwipeTarget(Float.NaN, previousVerse = previous, nextVerse = next, currentVerse = current))
    }

    @Test
    fun firstAndLastVerseBoundariesDoNotNavigatePastQuran() {
        assertNull(tafsirSwipeTarget(120f, previousVerse = current, nextVerse = next, currentVerse = current))
        assertNull(tafsirSwipeTarget(-120f, previousVerse = previous, nextVerse = current, currentVerse = current))
    }
}

