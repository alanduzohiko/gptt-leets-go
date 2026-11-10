package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranInlinePlaybackTrackingTest {
    @Test
    fun verseByVerseFilesAdvanceTheActiveMushafAyahWithEachStartedTrack() {
        assertEquals(3, quranPlaybackAyahForTrack(audioType = 0, startingAyah = 3, trackIndex = 0))
        assertEquals(4, quranPlaybackAyahForTrack(audioType = 0, startingAyah = 3, trackIndex = 1))
        assertEquals(7, quranPlaybackAyahForTrack(audioType = 0, startingAyah = 3, trackIndex = 4))
    }

    @Test
    fun fullSurahFileDoesNotPretendToHaveIndividualAyahBoundaries() {
        assertEquals(1, quranPlaybackAyahForTrack(audioType = 1, startingAyah = 1, trackIndex = 0))
        assertEquals(1, quranPlaybackAyahForTrack(audioType = 1, startingAyah = 1, trackIndex = 8))
        assertEquals(listOf(1), quranPlaybackTrackAyahs(audioType = 1, startingAyah = 1, ayahCount = 7))
    }

    @Test
    fun verseSegmentsStartAtTheSelectedAyahAndStopAtTheSurahEnd() {
        assertEquals(listOf(5, 6, 7), quranPlaybackTrackAyahs(audioType = 0, startingAyah = 5, ayahCount = 7))
        assertEquals(emptyList<Int>(), quranPlaybackTrackAyahs(audioType = 0, startingAyah = 1, ayahCount = 0))
    }
}
