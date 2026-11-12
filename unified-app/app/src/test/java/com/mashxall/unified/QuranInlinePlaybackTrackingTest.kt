package com.mashxall.unified

import com.mashxall.unified.data.MushafPageVerse
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

    @Test
    fun oneFileFullSurahCanOnlyStartAtItsFirstAyah() {
        val first = quranPlaybackStartPlan(audioType = 1, requestedAyah = 1, ayahCount = 7)
        assertEquals(true, first.isSupported)
        assertEquals(1, first.startingAyah)
        assertEquals(false, first.usesAyahClips)

        val later = quranPlaybackStartPlan(audioType = 1, requestedAyah = 5, ayahCount = 7)
        assertEquals(false, later.isSupported)
        assertEquals(1, later.startingAyah)
    }

    @Test
    fun ayahClipPlaylistStartsExactlyAtTheRequestedAyah() {
        val plan = quranPlaybackStartPlan(audioType = 0, requestedAyah = 5, ayahCount = 7)
        assertEquals(true, plan.isSupported)
        assertEquals(true, plan.usesAyahClips)
        assertEquals(5, plan.startingAyah)
    }

    @Test
    fun fullSurahSourcesUseOnlyVerifiedSameReciterAyahEditions() {
        assertEquals(4, quranAyahSegmentReciterId(2))
        assertEquals(1, quranAyahSegmentReciterId(3))
        assertEquals(76, quranAyahSegmentReciterId(18))
        assertEquals(58, quranAyahSegmentReciterId(26))
        assertEquals(28, quranAyahSegmentReciterId(73))
        assertEquals(17, quranAyahSegmentReciterId(1_000_102, "102-102"))
        assertEquals(25, quranAyahSegmentReciterId(1_000_112, "112-113"))
        assertEquals(19, quranAyahSegmentReciterId(1_000_118, "118-119"))
        assertEquals(null, quranAyahSegmentReciterId(1_000_1, "1-1"))
        assertEquals(null, quranAyahSegmentReciterId(37))
    }

    @Test
    fun verseHighlightUsesOnlyTheTrackConfirmedReadyByThePlayer() {
        assertEquals(0, quranPlaybackCommittedTrackIndex(0, 6))
        assertEquals(3, quranPlaybackCommittedTrackIndex(3, 6))
        assertEquals(-1, quranPlaybackCommittedTrackIndex(-1, 6))
        assertEquals(-1, quranPlaybackCommittedTrackIndex(6, 6))
        assertEquals(-1, quranPlaybackCommittedTrackIndex(1, 0))
    }

    @Test
    fun pagePlaybackBeginsAtTheFirstVerseOnTheVisibleMushafPage() {
        assertEquals(
            2 to 35,
            quranMushafPagePlaybackTarget(
                listOf(MushafPageVerse(2, 35), MushafPageVerse(2, 36), MushafPageVerse(2, 37))
            )
        )
        assertEquals(
            18 to 1,
            quranMushafPagePlaybackTarget(
                listOf(MushafPageVerse(18, 1), MushafPageVerse(18, 2))
            )
        )
        assertEquals(null, quranMushafPagePlaybackTarget(emptyList()))
    }

    @Test
    fun invalidSurahCountCannotProduceAPlaybackPlan() {
        val plan = quranPlaybackStartPlan(audioType = 0, requestedAyah = 3, ayahCount = 0)
        assertEquals(false, plan.isSupported)
    }
}
