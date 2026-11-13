package com.mashxall.unified

import com.mashxall.unified.data.MushafPageVerse
import com.mashxall.unified.data.Reciter
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
    fun oneFileFullSurahAlwaysPlaysAndUsesOnlyVerifiedTimingsForMidSurahSeek() {
        val first = quranPlaybackStartPlan(audioType = 1, requestedAyah = 1, ayahCount = 7)
        assertEquals(true, first.isSupported)
        assertEquals(1, first.startingAyah)
        assertEquals(false, first.usesAyahClips)

        val laterWithoutTimings = quranPlaybackStartPlan(audioType = 1, requestedAyah = 5, ayahCount = 7)
        assertEquals(true, laterWithoutTimings.isSupported)
        assertEquals(1, laterWithoutTimings.startingAyah)

        val laterWithTimings = quranPlaybackStartPlan(
            audioType = 1,
            requestedAyah = 5,
            ayahCount = 7,
            hasAyahTimings = true
        )
        assertEquals(true, laterWithTimings.isSupported)
        assertEquals(5, laterWithTimings.startingAyah)
        assertEquals(false, laterWithTimings.usesAyahClips)
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
        assertEquals(8, quranAyahSegmentReciterId(1_000_4, "4-4"))
        assertEquals(50, quranAyahSegmentReciterId(1_000_5, "5-5"))
        assertEquals(28, quranAyahSegmentReciterId(1_000_30, "30-30"))
        assertEquals(31, quranAyahSegmentReciterId(1_000_31, "31-31"))
        assertEquals(5, quranAyahSegmentReciterId(1_000_54, "54-54"))
        assertEquals(7, quranAyahSegmentReciterId(1_000_60, "60-60"))
        assertEquals(12, quranAyahSegmentReciterId(1_000_74, "74-74"))
        assertEquals(15, quranAyahSegmentReciterId(1_000_89, "89-89"))
        assertEquals(23, quranAyahSegmentReciterId(1_000_106, "106-106"))
        assertEquals(21, quranAyahSegmentReciterId(1_000_109, "109-109"))
        assertEquals(22, quranAyahSegmentReciterId(1_000_111, "111-111"))
        assertEquals(20, quranAyahSegmentReciterId(1_000_123, "123-123"))
        assertEquals(null, quranAyahSegmentReciterId(1_000_1, "1-1"))
        assertEquals(null, quranAyahSegmentReciterId(37))
    }

    @Test
    fun nextAndPreviousUseTheLastConfirmedClipEvenIfMedia3HasAdvancedWhileBuffering() {
        // While the UI still confirms index 2, Media3 may already report index 3
        // as current during buffering. Navigation from index 2 must still target 3.
        assertEquals(3, quranPlaybackSequenceTargetIndex(2, 8, next = true))
        assertEquals(1, quranPlaybackSequenceTargetIndex(2, 8, next = false))
        assertEquals(0, quranPlaybackSequenceTargetIndex(1, 8, next = false))
        assertEquals(null, quranPlaybackSequenceTargetIndex(0, 8, next = false))
        assertEquals(null, quranPlaybackSequenceTargetIndex(7, 8, next = true))
        assertEquals(null, quranPlaybackSequenceTargetIndex(-1, 8, next = true))
        assertEquals(null, quranPlaybackSequenceTargetIndex(0, 0, next = true))
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
    fun pagePlaybackUsesTheVerseSelectedByTheReaderWhenItBelongsToThatPage() {
        val page = listOf(
            MushafPageVerse(2, 35),
            MushafPageVerse(2, 36),
            MushafPageVerse(2, 37)
        )
        assertEquals(2 to 36, quranMushafPagePlaybackTarget(page, page[1]))
        // A stale selection from a previous page must not override this page.
        assertEquals(2 to 35, quranMushafPagePlaybackTarget(page, MushafPageVerse(2, 99)))
        assertEquals(2 to 35, quranMushafPagePlaybackTarget(page))
    }

    @Test
    fun invalidSurahCountCannotProduceAPlaybackPlan() {
        val plan = quranPlaybackStartPlan(audioType = 0, requestedAyah = 3, ayahCount = 0)
        assertEquals(false, plan.isSupported)
    }
    @Test
    fun fullSurahAndVerseByVerseModesNeverMixRecordingTypes() {
        assertEquals(true, reciterMatchesPlaybackMode("full", 1))
        assertEquals(true, reciterMatchesPlaybackMode("full", 2))
        assertEquals(false, reciterMatchesPlaybackMode("full", 0))
        assertEquals(true, reciterMatchesPlaybackMode("verse", 0))
        assertEquals(false, reciterMatchesPlaybackMode("verse", 1))
    }

    @Test
    fun playbackModeControlsTheRequestedStartingAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 35, 100))
        assertEquals(35, resolveReciterStartAyah("verse", 35, 100))
    }

    @Test
    fun mushafKeepsTheTappedAyahWhenUsingAFullSurahRecording() {
        assertEquals(35, resolveMushafInlineStartAyah(35, 100))
        assertEquals(1, resolveMushafInlineStartAyah(0, 100))
        assertEquals(100, resolveMushafInlineStartAyah(120, 100))
        val timedFullSurah = quranPlaybackStartPlan(
            audioType = 1,
            requestedAyah = resolveMushafInlineStartAyah(35, 100),
            ayahCount = 100,
            hasAyahTimings = true
        )
        assertEquals(true, timedFullSurah.isSupported)
        assertEquals(false, timedFullSurah.usesAyahClips)
        assertEquals(35, timedFullSurah.startingAyah)
    }

    @Test
    fun defaultAndSavedReciterNeverFallBackAcrossPlaybackModes() {
        val verseReciter = Reciter(
            id = 1, nameArabic = "قارئ اختبار", nameEnglish = "Test reader",
            audioType = 0, baseUrl = "https://example.invalid/verse/", order = 1
        )
        val fullReciter = Reciter(
            id = 2, nameArabic = "قارئ اختبار 2", nameEnglish = "Test reader 2",
            audioType = 1, baseUrl = "https://example.invalid/full/", order = 2
        )
        assertEquals(1, defaultReciterId("verse", listOf(verseReciter)))
        assertEquals(null, defaultReciterId("full", listOf(verseReciter)))
        assertEquals(2, defaultReciterId("full", listOf(verseReciter, fullReciter)))
        assertEquals(null, resolveCompatibleReciterId("full", 1, listOf(verseReciter)))
        assertEquals(null, resolveCompatibleReciterId("verse", 2, listOf(fullReciter)))
        assertEquals(2, resolveCompatibleReciterId("full", 2, listOf(fullReciter)))
    }
}
