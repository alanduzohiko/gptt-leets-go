package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlaybackModeTest {
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
