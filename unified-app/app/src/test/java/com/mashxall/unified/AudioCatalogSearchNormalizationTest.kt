package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCatalogSearchNormalizationTest {
    @Test
    fun searchIgnoresHarakatAndAlefVariants() {
        assertTrue(matchesAudioCatalogSearch("إبراهيم", "إِبْرَاهِيمُ"))
        assertTrue(matchesAudioCatalogSearch("القرآن", "القران"))
    }

    @Test
    fun searchFoldsKurdishAndPersianLetterVariants() {
        assertTrue(matchesAudioCatalogSearch("قاری", "قاري"))
        assertTrue(matchesAudioCatalogSearch("کریم", "كريم"))
    }

    @Test
    fun searchMatchesJoinedAndSeparatedArabicNames() {
        assertTrue(matchesAudioCatalogSearch("عبدالرحمن", "عبد الرحمن"))
        assertTrue(matchesAudioCatalogSearch("عبد الرحمن", "عبدالرحمن"))
    }

    @Test
    fun searchRequiresEveryQueryWord() {
        assertFalse(matchesAudioCatalogSearch("عبدالملك", "عبد الرحمن"))
    }

    @Test
    fun nextAndPreviousMoveExactlyOneAyahClip() {
        assertEquals(4, adjacentAudioSequenceIndex(3, 10, forward = true))
        assertEquals(2, adjacentAudioSequenceIndex(3, 10, forward = false))
    }

    @Test
    fun ayahNavigationStopsAtSequenceBoundaries() {
        assertNull(adjacentAudioSequenceIndex(0, 10, forward = false))
        assertNull(adjacentAudioSequenceIndex(9, 10, forward = true))
        assertNull(adjacentAudioSequenceIndex(0, 0, forward = true))
    }
}
