package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun unifiedQuranPickerShowsBothFullSurahAndVerseByVerseRecitersTogether() {
        val full = com.mashxall.unified.data.Reciter(
            80, "قارئ السورة", "Full Surah Reader", 1, "https://example.invalid/full/", 1
        )
        val verse = com.mashxall.unified.data.Reciter(
            81, "قارئ الآيات", "Ayah Reader", 0, "https://example.invalid/verse/", 2
        )
        assertEquals(
            listOf(full, verse),
            filterRecitersForUnifiedPicker(listOf(full, verse), "", 2)
        )
        assertEquals(listOf(verse), filterRecitersForUnifiedPicker(listOf(full, verse), "Ayah", 2))
        assertEquals(emptyList<com.mashxall.unified.data.Reciter>(), filterRecitersForUnifiedPicker(listOf(full, verse), "", 3))
    }

    @Test
    fun onlyVerseByVerseReadersShowPreviousAndNextAyahButtons() {
        assertEquals(true, quranReciterShowsAyahNavigation(0))
        assertEquals(false, quranReciterShowsAyahNavigation(1))
        assertEquals(false, quranReciterShowsAyahNavigation(2))
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
        assertNull(resolveCompatibleReciterId("verse", 2, listOf(verseReciter, fullReciter)))
    }

    @Test
    fun fullSurahModeKeepsTheCurrentCompatibleReciter() {
        val verseReciter = com.mashxall.unified.data.Reciter(1, "قارئ آية", "Verse Reciter", 0, "https://example.invalid/verse/", 1)
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        assertEquals(2, resolveCompatibleReciterId("full", 2, listOf(verseReciter, fullReciter)))
    }

    @Test
    fun firstUseFullSurahNeverChoosesAnEditionDeclaredPartial() {
        val partial = com.mashxall.unified.data.Reciter(
            5, "قارئ جزئي", "Partial first in source", 1, "https://example.invalid/partial/", 0,
            surahTotal = 40, sourceId = "partial-40"
        )
        val complete = com.mashxall.unified.data.Reciter(
            6, "قارئ كامل", "Full coverage", 1, "https://example.invalid/full/", 1,
            surahTotal = 114, sourceId = "complete-114"
        )
        // First-use default still prefers a complete edition, but an explicitly
        // selected partial edition remains a valid full-surah source for its coverage.
        assertEquals(6, defaultReciterId("full", listOf(partial, complete)))
        assertEquals(5, resolveCompatibleReciterId("full", 5, listOf(partial, complete)))
        assertNull(resolveCompatibleReciterId("full", -1, listOf(partial, complete)))
        assertNull(defaultReciterId("full", listOf(partial)))
    }

    @Test
    fun fullSurahLibraryShowsAvailablePartialAndCompleteEditionsAndVerseModeShowsVerseSources() {
        val partial = com.mashxall.unified.data.Reciter(
            5, "قارئ جزئي", "Partial edition", 1, "https://example.invalid/partial/", 0,
            surahTotal = 40, sourceId = "partial-40"
        )
        val complete = com.mashxall.unified.data.Reciter(
            6, "قارئ كامل", "Complete edition", 1, "https://example.invalid/full/", 1,
            surahTotal = 114, sourceId = "complete-114"
        )
        val verse = com.mashxall.unified.data.Reciter(
            7, "قارئ آية", "Verse source", 0, "https://example.invalid/ayah/", 2,
            surahTotal = 114
        )
        assertEquals(listOf(partial, complete), filterRecitersForPlaybackMode(listOf(partial, complete, verse), "full", ""))
        assertEquals(listOf(verse), filterRecitersForPlaybackMode(listOf(partial, complete, verse), "verse", ""))
        assertEquals(listOf(partial), filterRecitersForPlaybackMode(listOf(partial), "full", ""))
    }

    @Test
    fun verseModeDoesNotPretendAFullSurahOnlySourceCanStartAtAnAyah() {
        val fullReciter = com.mashxall.unified.data.Reciter(2, "قارئ سورة", "Full Reciter", 1, "https://example.invalid/full/", 2)
        org.junit.Assert.assertNull(resolveCompatibleReciterId("verse", 2, listOf(fullReciter)))
    }
}
