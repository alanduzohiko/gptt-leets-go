package com.mashxall.unified

import com.mashxall.unified.data.Ayah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranMushafSurahSelectionTest {
    private fun ayah(surah: Int, number: Int, page: Int) = Ayah(
        surah = surah,
        number = number,
        arabic = "",
        translation = "",
        page = page
    )

    @Test
    fun opensEachSelectedSurahAtItsSourceMappedFirstAyahPage() {
        val verses = listOf(
            ayah(surah = 1, number = 1, page = 1),
            ayah(surah = 1, number = 2, page = 1),
            ayah(surah = 2, number = 1, page = 2),
            ayah(surah = 2, number = 2, page = 2)
        )
        assertEquals(1, resolveSurahStartPage(verses, 1))
        assertEquals(2, resolveSurahStartPage(verses, 2))
    }

    @Test
    fun refusesMissingFirstVerseOrInvalidMushafPageNumbers() {
        assertNull(resolveSurahStartPage(listOf(ayah(surah = 2, number = 2, page = 2)), 2))
        assertNull(resolveSurahStartPage(listOf(ayah(surah = 2, number = 1, page = 0)), 2))
        assertNull(resolveSurahStartPage(listOf(ayah(surah = 2, number = 1, page = 605)), 2))
        assertNull(resolveSurahStartPage(listOf(ayah(surah = 2, number = 1, page = 2)), 115))
    }

    @Test
    fun firstVerseMustBelongToTheSurahThatWasActuallySelected() {
        val verses = listOf(
            ayah(surah = 1, number = 1, page = 1),
            ayah(surah = 2, number = 2, page = 2)
        )
        assertNull(resolveSurahStartPage(verses, 2))
    }
}
