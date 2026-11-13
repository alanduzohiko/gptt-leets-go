package com.mashxall.unified

import com.mashxall.unified.data.Reciter
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
    fun fullSurahSearchDoesNotHideReadersWithPartialCatalogCoverage() {
        val partialFullSurah = Reciter(
            id = 700,
            nameArabic = "مشاري العفاسي",
            nameEnglish = "Mishary Alafasy",
            audioType = 1,
            baseUrl = "https://server.example/reciter/",
            order = 1,
            surahTotal = 42
        )
        val verseByVerse = Reciter(
            id = 701,
            nameArabic = "مشاري العفاسي آيات",
            nameEnglish = "Mishary Alafasy Ayah",
            audioType = 0,
            baseUrl = "https://server.example/ayah/",
            order = 2
        )

        assertEquals(
            listOf(partialFullSurah),
            filterRecitersForPlaybackMode(listOf(partialFullSurah, verseByVerse), "full", "مشاري")
        )
        assertEquals(
            listOf(verseByVerse),
            filterRecitersForPlaybackMode(listOf(partialFullSurah, verseByVerse), "verse", "مشاري")
        )
    }

    @Test
    fun verifiedAhmadAlNufaisRecordingIsAvailableFromTheSelectedAyahMode() {
        val ahmadAlNufais = Reciter(
            id = 1_000_259,
            nameArabic = "أحمد النفيس",
            nameEnglish = "Ahmad Al Nufais",
            audioType = 1,
            baseUrl = "https://server16.mp3quran.net/nufais/Rewayat-Hafs-A-n-Assem/",
            order = 10_259,
            sourceId = "259-259"
        )
        val unverifiedLookalike = ahmadAlNufais.copy(id = 1_000_258, sourceId = "259-258")

        assertTrue(quranHasVerifiedMushafTimingRead(ahmadAlNufais))
        assertFalse(quranHasVerifiedMushafTimingRead(unverifiedLookalike))
        assertTrue(reciterMatchesPlaybackMode("verse", ahmadAlNufais))
        assertFalse(reciterMatchesPlaybackMode("verse", unverifiedLookalike))
        assertEquals(
            listOf(ahmadAlNufais),
            filterRecitersForPlaybackMode(listOf(ahmadAlNufais, unverifiedLookalike), "verse", "النفيس")
        )
        assertEquals(ahmadAlNufais.id, defaultReciterId("verse", listOf(ahmadAlNufais)))
        assertEquals(
            ahmadAlNufais.id,
            resolveCompatibleReciterId("verse", ahmadAlNufais.id, listOf(ahmadAlNufais))
        )
    }

    @Test
    fun nextUsesTheClipThatHasActuallyStartedAfterGaplessTransition() {
        assertEquals(5, resolveActiveAudioSequenceIndex(4, 5, preloadedHasStarted = true))
        assertEquals(4, resolveActiveAudioSequenceIndex(4, 5, preloadedHasStarted = false))
        assertEquals(4, resolveActiveAudioSequenceIndex(4, -1, preloadedHasStarted = true))
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
