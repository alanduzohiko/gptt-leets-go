package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranSurahNavigationTest {
    @Test
    fun movesOnlyToAnAdjacentSurah() {
        assertEquals(17, quranAdjacentSurahTarget(18, -1))
        assertEquals(19, quranAdjacentSurahTarget(18, 1))
    }

    @Test
    fun respectsTheFirstAndLastSurahBoundaries() {
        assertNull(quranAdjacentSurahTarget(1, -1))
        assertNull(quranAdjacentSurahTarget(114, 1))
    }

    @Test
    fun rejectsInvalidSurahNumbersAndDirections() {
        assertNull(quranAdjacentSurahTarget(0, 1))
        assertNull(quranAdjacentSurahTarget(115, -1))
        assertNull(quranAdjacentSurahTarget(5, 0))
        assertNull(quranAdjacentSurahTarget(5, 2))
    }
}
