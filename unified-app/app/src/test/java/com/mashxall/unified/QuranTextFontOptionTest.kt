package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTextFontOptionTest {
    @Test
    fun offersSixVerifiedSourceFontAssets() {
        assertEquals(6, quranTextFontOptions.size)
        assertEquals(
            setOf("hafs_regular", "hafs_otf", "amiri_quran", "me_quran", "al_qalam", "uthmanic_hafs"),
            quranTextFontOptions.map { it.id }.toSet()
        )
        quranTextFontOptions.forEach { option ->
            assertTrue(option.assetPath.startsWith("content/"))
            assertTrue(option.sizeBytes > 50_000L)
            assertTrue(option.sha256.matches(Regex("^[a-f0-9]{64}$")))
        }
    }

    @Test
    fun defaultsToHafsAndFallsBackSafelyForUnknownSavedValue() {
        assertEquals("hafs_regular", quranTextFontOption("missing").id)
        assertEquals("hafs_regular", quranTextFontOption("hafs_regular").id)
        assertNotEquals("hafs_regular", quranTextFontOption("uthmanic_hafs").id)
    }

    @Test
    fun providesLocalizedLabelsForTheFontPicker() {
        val option = quranTextFontOption("amiri_quran")
        assertFalse(quranTextFontLabel(option, AppLanguage.CKB).isBlank())
        assertFalse(quranTextFontLabel(option, AppLanguage.AR).isBlank())
        assertEquals("Amiri Quran", quranTextFontLabel(option, AppLanguage.EN))
    }
}
