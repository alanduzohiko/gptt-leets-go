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

    // One-file recordings have no per-ayah timestamps, so mapping is explicitly approximate.
    @Test
    fun fullSurahPlaybackEstimateAdvancesAsTheSingleTrackProgresses() {
        assertEquals(1, quranPlaybackAyahAtPosition(1, 7, 0, 7_000))
        assertEquals(4, quranPlaybackAyahAtPosition(1, 7, 3_500, 7_000))
        assertEquals(7, quranPlaybackAyahAtPosition(1, 7, 7_000, 7_000))
    }

    @Test
    fun fullSurahSeekTargetsTheApproximateTimeSliceForTheRequestedAyah() {
        val position = quranPlaybackPositionForAyah(
            targetAyah = 3,
            startingAyah = 1,
            ayahCount = 7,
            durationMs = 7_000
        )
        assertEquals(2_500, position)
        assertEquals(3, quranPlaybackAyahAtPosition(1, 7, position, 7_000))
    }

    @Test
    fun fullSurahWeightsFollowArabicVerseLengthWhenAvailable() {
        val weights = listOf(10, 20, 30)
        assertEquals(1, quranPlaybackAyahAtPosition(1, 3, 500, 6_000, weights))
        assertEquals(2, quranPlaybackAyahAtPosition(1, 3, 1_500, 6_000, weights))
        assertEquals(3, quranPlaybackAyahAtPosition(1, 3, 3_500, 6_000, weights))
        val targetPosition = quranPlaybackPositionForAyah(2, 1, 3, 6_000, weights)
        assertEquals(2_000, targetPosition)
        assertEquals(2, quranPlaybackAyahAtPosition(1, 3, targetPosition, 6_000, weights))
    }

    @Test
    fun fullSurahProgressAndSeekClampToTheAvailableAyahRange() {
        assertEquals(3, quranPlaybackAyahAtPosition(3, 7, 0, 10_000))
        assertEquals(7, quranPlaybackAyahAtPosition(3, 7, 100_000, 10_000))
        assertEquals(4, quranPlaybackPositionForAyah(1, 4, 7, 9_000).let {
            quranPlaybackAyahAtPosition(4, 7, it, 9_000)
        })
        assertEquals(0, quranPlaybackPositionForAyah(4, 4, 7, 0))
    }
}
