package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranProgressDiscoveryTest {
    @Test
    fun summarizesOnlyValidSourceSurahsAndClampsSavedProgress() {
        val summary = quranReadingProgressSummary(
            surahTotals = mapOf(1 to 7, 2 to 286, 115 to 10, 3 to 0),
            progressBySurah = mapOf(1 to 7, 2 to 300, 115 to 9)
        )
        assertEquals(293, summary.totalAyahs)
        assertEquals(293, summary.readAyahs)
        assertEquals(2, summary.completedSurahs)
        assertEquals(0, summary.inProgressSurahs)
        assertEquals(1f, summary.fraction, 0.0001f)
    }

    @Test
    fun reportsPartialReadingWithoutMarkingItComplete() {
        val summary = quranReadingProgressSummary(
            surahTotals = mapOf(1 to 7, 2 to 286, 3 to 200),
            progressBySurah = mapOf(1 to 4, 2 to 0, 3 to 200)
        )
        assertEquals(493, summary.totalAyahs)
        assertEquals(204, summary.readAyahs)
        assertEquals(1, summary.completedSurahs)
        assertEquals(1, summary.inProgressSurahs)
        assertTrue(summary.fraction in 0f..1f)
    }

    @Test
    fun handlesAnEmptySurahCatalogueSafely() {
        val summary = quranReadingProgressSummary(emptyMap(), emptyMap())
        assertEquals(0, summary.totalAyahs)
        assertEquals(0, summary.readAyahs)
        assertEquals(0f, summary.fraction, 0f)
    }

    @Test
    fun progressFiltersClassifyUnreadPartialAndCompletedSurahs() {
        assertTrue(matchesSurahProgressFilter(0, 7, "not_started"))
        assertTrue(matchesSurahProgressFilter(3, 7, "in_progress"))
        assertTrue(matchesSurahProgressFilter(7, 7, "completed"))
        assertFalse(matchesSurahProgressFilter(0, 7, "in_progress"))
        assertFalse(matchesSurahProgressFilter(3, 7, "completed"))
        assertFalse(matchesSurahProgressFilter(7, 7, "not_started"))
    }

    @Test
    fun progressFiltersClampCorruptValuesAndRejectUnknownTotals() {
        assertTrue(matchesSurahProgressFilter(99, 7, "completed"))
        assertTrue(matchesSurahProgressFilter(-5, 7, "not_started"))
        assertFalse(matchesSurahProgressFilter(1, 0, "in_progress"))
        assertFalse(matchesSurahProgressFilter(1, -7, "completed"))
        assertTrue(matchesSurahProgressFilter(1, 0, "all"))
    }
}
