package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlaybackModeTest {
    @Test
    fun defaultsToFullSurahButHonoursExplicitVerseMode() {
        assertEquals("full", ReaderPlaybackSettings.normalize(null))
        assertEquals("full", ReaderPlaybackSettings.normalize("full"))
        assertEquals("full", ReaderPlaybackSettings.normalize("unknown"))
        assertEquals("verse", ReaderPlaybackSettings.normalize("verse"))
    }

    @Test
    fun firstUseReciterMatchesTheSelectedPlaybackMode() {
        val verseReciter = com.mashxall.unified.data.Reciter(1, "قارئ آية", "Verse Reciter", 0, "https://example.invalid/verse/", 1)
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        assertEquals(2, defaultReciterId("full", listOf(verseReciter, fullReciter)))
        assertEquals(1, defaultReciterId("verse", listOf(verseReciter, fullReciter)))
        assertEquals(2, defaultReciterId("full", listOf(fullReciter)))
        assertEquals(1, defaultReciterId("verse", listOf(verseReciter)))
    }

    @Test
    fun fullSurahModeOnlyShowsEditionsDeclaredToCoverAll114Surahs() {
        val complete = com.mashxall.unified.data.Reciter(
            10, "قارئ كامل", "Complete edition", 1, "https://example.invalid/complete/", 1,
            surahTotal = 114, sourceId = "complete-114"
        )
        val partial = com.mashxall.unified.data.Reciter(
            11, "قارئ جزئي", "Partial edition", 1, "https://example.invalid/partial/", 2,
            surahTotal = 38, sourceId = "partial-38"
        )
        val bundledVerseSource = com.mashxall.unified.data.Reciter(
            12, "قارئ آية", "Bundled verse source", 0, "https://example.invalid/ayah/", 3,
            surahTotal = 114
        )

        assertEquals(true, ReaderPlaybackSettings.supportsAllSurahs(complete))
        assertEquals(false, ReaderPlaybackSettings.supportsAllSurahs(partial))
        assertEquals(true, ReaderPlaybackSettings.supportsAllSurahs(bundledVerseSource))
    }

    @Test
    fun fullSurahAlwaysStartsAtTheFirstAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 17, 286))
        assertEquals(1, resolveReciterStartAyah("full", 1, 7))
    }

    @Test
    fun selectedAyahModeStartsAtTheRequestedAyah() {
        assertEquals(17, resolveReciterStartAyah("verse", 17, 286))
        assertEquals(1, resolveReciterStartAyah("verse", 0, 7))
        assertEquals(7, resolveReciterStartAyah("verse", 12, 7))
    }

    @Test
    fun protectsAgainstMissingAyahCounts() {
        assertEquals(1, resolveReciterStartAyah("verse", 4, 0))
        assertEquals(1, resolveReciterStartAyah("verse", -1, -10))
    }

    @Test
    fun selectedAyahModeSwitchesAwayFromFullSurahOnlyReciter() {
        val verseReciter = com.mashxall.unified.data.Reciter(1, "قارئ آية", "Verse Reciter", 0, "https://example.invalid/verse/", 1)
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        assertEquals(1, resolveCompatibleReciterId("verse", 2, listOf(verseReciter, fullReciter)))
    }

    @Test
    fun fullSurahModeKeepsTheCurrentCompatibleReciter() {
        val verseReciter = com.mashxall.unified.data.Reciter(1, "قارئ آية", "Verse Reciter", 0, "https://example.invalid/verse/", 1)
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        assertEquals(2, resolveCompatibleReciterId("full", 2, listOf(verseReciter, fullReciter)))
    }

    @Test
    fun verseModeDoesNotPretendAFullSurahOnlySourceCanStartAtAnAyah() {
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        org.junit.Assert.assertNull(resolveCompatibleReciterId("verse", 2, listOf(fullReciter)))
    }
}
