package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranSurahProgressTest {
    @Test
    fun returnsNoProgressBeforeTheFirstAyahIsRead() {
        assertNull(quranSurahProgressFraction(0, 7))
    }

    @Test
    fun calculatesPartialAndCompleteSurahProgress() {
        assertEquals(0.5f, quranSurahProgressFraction(5, 10)!!, 0.0001f)
        assertEquals(1f, quranSurahProgressFraction(10, 10)!!, 0.0001f)
    }

    @Test
    fun clampsOverlongProgressAndRejectsInvalidTotals() {
        assertEquals(1f, quranSurahProgressFraction(25, 10)!!, 0.0001f)
        assertNull(quranSurahProgressFraction(3, 0))
        assertNull(quranSurahProgressFraction(-1, 10))
    }
}
