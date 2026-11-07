package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarProgressSummaryTest {
    @Test
    fun summarizesRepetitionsAndCompletedEntriesFromSavedCounters() {
        val summary = summarizeAzkarProgress(
            targetsByEntry = mapOf(1 to 3, 2 to 5, 3 to 2),
            progressByEntry = mapOf(1 to 3, 2 to 2, 3 to 2)
        )
        assertEquals(10, summary.totalRepetitions)
        assertEquals(7, summary.completedRepetitions)
        assertEquals(3, summary.totalEntries)
        assertEquals(2, summary.completedEntries)
        assertEquals(1, summary.inProgressEntries)
        assertEquals(0.7f, summary.fraction, 0.0001f)
    }

    @Test
    fun clampsPersistedCountersAndIgnoresInvalidEntryIdsOrTargets() {
        val summary = summarizeAzkarProgress(
            targetsByEntry = mapOf(1 to 3, 2 to 0, 3 to 4, 0 to 8, -4 to 2),
            progressByEntry = mapOf(1 to -5, 2 to 5, 3 to 99, 0 to 8)
        )
        assertEquals(7, summary.totalRepetitions)
        assertEquals(4, summary.completedRepetitions)
        assertEquals(2, summary.totalEntries)
        assertEquals(1, summary.completedEntries)
        assertEquals(0, summary.inProgressEntries)
        assertTrue(summary.fraction in 0f..1f)
    }

    @Test
    fun partialProgressIsNotCountedAsCompleted() {
        val summary = summarizeAzkarProgress(
            targetsByEntry = mapOf(11 to 5, 12 to 4, 13 to 6),
            progressByEntry = mapOf(11 to 2, 12 to 0, 13 to 0)
        )
        assertEquals(15, summary.totalRepetitions)
        assertEquals(2, summary.completedRepetitions)
        assertEquals(0, summary.completedEntries)
        assertEquals(1, summary.inProgressEntries)
    }

    @Test
    fun emptyProgressHasNoPhantomCompletion() {
        val summary = summarizeAzkarProgress(emptyMap(), emptyMap())
        assertEquals(0, summary.totalRepetitions)
        assertEquals(0, summary.completedRepetitions)
        assertEquals(0, summary.totalEntries)
        assertEquals(0, summary.completedEntries)
        assertEquals(0, summary.inProgressEntries)
        assertFalse(summary.fraction > 0f)
    }
}
