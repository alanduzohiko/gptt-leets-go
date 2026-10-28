package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranTextScriptTest {
    @Test
    fun acceptsOnlyACompleteSequentialChapterMatchingHafsVerseNumbers() {
        val rows = listOf(
            QuranScriptVerse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"),
            QuranScriptVerse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"),
            QuranScriptVerse(3, "الرَّحْمَٰنِ الرَّحِيمِ")
        )
        assertEquals(
            mapOf(1 to rows[0].text, 2 to rows[1].text, 3 to rows[2].text),
            validateQuranScriptChapter(1, 1, 3, rows)
        )
    }

    @Test
    fun rejectsAResponseForTheWrongSurah() {
        val rows = listOf(QuranScriptVerse(1, "ayah one"))
        assertNull(validateQuranScriptChapter(2, 1, 1, rows))
    }

    @Test
    fun rejectsMissingOrExtraAyahsRatherThanSilentlyMisaligningText() {
        val rows = listOf(QuranScriptVerse(1, "ayah one"), QuranScriptVerse(2, "ayah two"))
        assertNull(validateQuranScriptChapter(1, 1, 3, rows))
        assertNull(validateQuranScriptChapter(1, 1, 1, rows))
    }

    @Test
    fun rejectsNonSequentialDuplicateAndBlankAyahEntries() {
        assertNull(
            validateQuranScriptChapter(
                1, 1, 2,
                listOf(QuranScriptVerse(1, "first"), QuranScriptVerse(3, "third"))
            )
        )
        assertNull(
            validateQuranScriptChapter(
                1, 1, 2,
                listOf(QuranScriptVerse(1, "first"), QuranScriptVerse(1, "duplicate"))
            )
        )
        assertNull(
            validateQuranScriptChapter(
                1, 1, 2,
                listOf(QuranScriptVerse(1, "first"), QuranScriptVerse(2, " "))
            )
        )
    }

    @Test
    fun supportedAlternatesAreNativeHafsTextScriptsNotPageImageEditions() {
        assertEquals(
            setOf(QuranTextEditions.QPC_HAFS, QuranTextEditions.HAFS_NASTALIQ),
            QuranTextEditions.alternatives.map { it.id }.toSet()
        )
        assertEquals(2, QuranTextEditions.alternatives.size)
        assertEquals("https://api.quranpedia.net/dumps/LICENSE.md", QuranTextEditions.alternatives.first().licenseUrl)
    }
}
