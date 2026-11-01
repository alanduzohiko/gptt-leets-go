package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranResumeTargetTest {
    @Test
    fun restoresTheLastMushafPageWhenTheSavedReaderModeIsMushaf() {
        val target = resolveQuranResumeTarget(
            savedSurah = 18,
            savedAyah = 12,
            savedPage = 295,
            savedReaderMode = "mushaf",
            hasSavedPage = true
        )
        assertEquals(18, target.surah)
        assertEquals(12, target.ayah)
        assertEquals(295, target.mushafPage)
        assertTrue(target.openMushaf)
    }

    @Test
    fun preservesVerseReaderModeEvenWhenAnOlderMushafPageExists() {
        val target = resolveQuranResumeTarget(
            savedSurah = 2,
            savedAyah = 255,
            savedPage = 42,
            savedReaderMode = "verse",
            hasSavedPage = true
        )
        assertEquals(2, target.surah)
        assertEquals(255, target.ayah)
        assertEquals(42, target.mushafPage)
        assertFalse(target.openMushaf)
    }

    @Test
    fun clampsPersistedPositionBeforeOpeningTheReader() {
        val target = resolveQuranResumeTarget(
            savedSurah = 115,
            savedAyah = 0,
            savedPage = 605,
            savedReaderMode = "mushaf",
            hasSavedPage = true
        )
        assertEquals(114, target.surah)
        assertEquals(1, target.ayah)
        assertEquals(604, target.mushafPage)
        assertTrue(target.openMushaf)
    }

    @Test
    fun doesNotRestoreADeletedMushafPage() {
        val target = resolveQuranResumeTarget(
            savedSurah = 1,
            savedAyah = 1,
            savedPage = 1,
            savedReaderMode = "mushaf",
            hasSavedPage = false
        )
        assertFalse(target.openMushaf)
    }

    @Test
    fun legacyPageWithoutModeKeepsBackwardCompatibleMushafResume() {
        val target = resolveQuranResumeTarget(
            savedSurah = 5,
            savedAyah = 4,
            savedPage = 90,
            savedReaderMode = null,
            hasSavedPage = true
        )
        assertTrue(target.openMushaf)
    }
}
