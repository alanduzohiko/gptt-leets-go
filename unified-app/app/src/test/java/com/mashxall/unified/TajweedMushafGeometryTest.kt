package com.mashxall.unified

import com.mashxall.unified.data.MushafWordBounds
import com.mashxall.unified.data.findMushafPageVerseAtPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TajweedMushafGeometryTest {
    @Test
    fun sourceGlyphCoordinatesAreScaledIntoTheDisplayedPage() {
        val firstLineWord = tajweedGlyphBoundsToPageSpace(
            surah = 2,
            ayah = 6,
            lineNumber = 1,
            minX = 42f,
            maxX = 175f,
            minY = 3f,
            maxY = 121f
        )

        assertEquals(42f * (1148f / 1340f), firstLineWord.minX, 0.1f)
        assertEquals(121f * (1148f / 1340f) + (1641f - 1890f * (1148f / 1340f)) / 2f, firstLineWord.maxY, 0.2f)
    }

    @Test
    fun tallerOriginalGlyphBoxesRemainSelectableWithoutChangingTheExistingSvgRules() {
        val word = tajweedGlyphBoundsToPageSpace(
            surah = 2,
            ayah = 6,
            lineNumber = 1,
            minX = 42f,
            maxX = 175f,
            minY = 3f,
            maxY = 121f
        )
        assertEquals(1, buildTajweedPageVerseGeometry(listOf(word)).size)
        assertEquals(0, buildTajweedPageVerseGeometry(emptyList()).size)
    }

    @Test
    fun tappingWithinThePrintedWordSelectsItsExactSurahAndAyah() {
        val bounds = tajweedGlyphBoundsToPageSpace(
            surah = 2,
            ayah = 6,
            lineNumber = 1,
            minX = 42f,
            maxX = 175f,
            minY = 3f,
            maxY = 121f
        )
        val geometry = buildTajweedPageVerseGeometry(listOf(bounds))
        val centerX = (bounds.minX + bounds.maxX) / 2f
        val centerY = (bounds.minY + bounds.maxY) / 2f

        val selected = findMushafPageVerseAtPoint(geometry, centerX, centerY, tolerancePx = 0f)
        assertNotNull(selected)
        assertEquals(2, selected?.surah)
        assertEquals(6, selected?.ayah)
        assertNull(findMushafPageVerseAtPoint(geometry, 0f, 0f, tolerancePx = 0f))
    }

    @Test
    fun differentAyahsRemainSeparateSelectionTargets() {
        val words = listOf(
            MushafWordBounds(2, 6, 1, 40f, 95f, 20f, 110f),
            MushafWordBounds(2, 7, 1, 100f, 170f, 20f, 110f)
        )
        val geometry = buildTajweedPageVerseGeometry(words)

        assertEquals(2, geometry.size)
        assertEquals(listOf(6, 7), geometry.map { it.verse.ayah })
    }
    @Test
    fun tajweedPagesShowTheirArabicIndicPrintedPageNumber() {
        assertEquals("١", tajweedPageNumberLabel(1))
        assertEquals("٣", tajweedPageNumberLabel(3))
        assertEquals("٦٠٤", tajweedPageNumberLabel(604))
        assertEquals("", tajweedPageNumberLabel(0))
        assertEquals("", tajweedPageNumberLabel(605))
    }

}
