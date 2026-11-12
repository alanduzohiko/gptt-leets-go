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
    fun oneFileFullSurahHasNoFabricatedPerAyahBoundaries() {
        assertEquals(1, quranPlaybackAyahForTrack(audioType = 1, startingAyah = 1, trackIndex = 0))
        assertEquals(1, quranPlaybackAyahForTrack(audioType = 1, startingAyah = 1, trackIndex = 8))
        assertEquals(listOf(1), quranPlaybackTrackAyahs(audioType = 1, startingAyah = 1, ayahCount = 7))
    }

    @Test
    fun verseSegmentsStartAtTheSelectedAyahAndStopAtTheSurahEnd() {
        assertEquals(listOf(5, 6, 7), quranPlaybackTrackAyahs(audioType = 0, startingAyah = 5, ayahCount = 7))
        assertEquals(emptyList<Int>(), quranPlaybackTrackAyahs(audioType = 0, startingAyah = 1, ayahCount = 0))
    }

    // These mathematical estimates remain for non-Mushaf legacy audio controls.
    // The Mushaf no longer uses them to seek or highlight a verse in one full MP3.
    @Test
    fun everySelectedFullSurahSeekLandsInsideTheSameWeightedAyahInterval() {
        val weights = listOf(10, 20, 30, 40)
        val duration = 100_000
        for (targetAyah in 1..4) {
            val position = quranFullSurahSeekPositionForAyah(
                targetAyah = targetAyah,
                ayahCount = 4,
                durationMs = duration,
                ayahWeights = weights
            )
            assertEquals(
                targetAyah,
                quranPlaybackAyahAtPosition(1, 4, position, duration, weights)
            )
        }
    }

    @Test
    fun legacyFullSurahTimingEstimateUsesAvailableVerseWeights() {
        val weights = listOf(10, 20, 30)
        assertEquals(1, quranPlaybackAyahAtPosition(1, 3, 500, 6_000, weights))
        assertEquals(2, quranPlaybackAyahAtPosition(1, 3, 1_500, 6_000, weights))
        assertEquals(3, quranPlaybackAyahAtPosition(1, 3, 3_500, 6_000, weights))
        val targetPosition = quranPlaybackPositionForAyah(2, 1, 3, 6_000, weights)
        assertEquals(2_000, targetPosition)
        assertEquals(2, quranPlaybackAyahAtPosition(1, 3, targetPosition, 6_000, weights))
    }

    @Test
    fun legacyTimingEstimateClampsToTheAvailableAyahRange() {
        assertEquals(3, quranPlaybackAyahAtPosition(3, 7, 0, 10_000))
        assertEquals(7, quranPlaybackAyahAtPosition(3, 7, 100_000, 10_000))
        assertEquals(4, quranPlaybackPositionForAyah(1, 4, 7, 9_000).let {
            quranPlaybackAyahAtPosition(4, 7, it, 9_000)
        })
        assertEquals(0, quranPlaybackPositionForAyah(4, 4, 7, 0))
    }
}
