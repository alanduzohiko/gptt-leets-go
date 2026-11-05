package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MushafSvgVerseGeometryTest {
    private fun word(
        surah: Int,
        ayah: Int,
        line: Int,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) = MushafWordBounds(surah, ayah, line, left, right, top, bottom)

    @Test
    fun wordsOfOneAyahOnOnePrintedLineBecomeOneTightRegion() {
        val regions = buildMushafPageVerseGeometry(
            listOf(
                word(1, 2, 8, 430f, 500f, 910f, 950f),
                word(1, 2, 8, 505f, 560f, 912f, 949f),
                word(1, 2, 8, 565f, 690f, 909f, 951f)
            )
        )

        assertEquals(1, regions.size)
        assertEquals(MushafPageVerse(1, 2), regions.single().verse)
        assertEquals(1, regions.single().rects.size)
        val rect = regions.single().rects.single()
        assertEquals(430f, rect.minX, 0.001f)
        assertEquals(690f, rect.maxX, 0.001f)
        assertEquals(909f, rect.minY, 0.001f)
        assertEquals(951f, rect.maxY, 0.001f)
    }

    @Test
    fun neighbouringAyahsOnTheSamePageAreNeverMerged() {
        val regions = buildMushafPageVerseGeometry(
            listOf(
                word(1, 1, 7, 350f, 620f, 820f, 890f),
                word(1, 2, 8, 430f, 700f, 910f, 980f)
            )
        )

        assertEquals(2, regions.size)
        assertEquals(MushafPageVerse(1, 1), regions[0].verse)
        assertEquals(MushafPageVerse(1, 2), regions[1].verse)
    }

    @Test
    fun verseWrappingAcrossLinesKeepsSeparateHighlights() {
        val regions = buildMushafPageVerseGeometry(
            listOf(
                word(2, 4, 10, 300f, 460f, 1000f, 1060f),
                word(2, 4, 11, 250f, 420f, 1080f, 1140f)
            )
        )

        assertEquals(1, regions.size)
        assertEquals(2, regions.single().rects.size)
    }

    @Test
    fun tapOnAlhamduRegionSelectsAyahTwoNotThePreviousAyah() {
        val regions = buildMushafPageVerseGeometry(
            listOf(
                word(1, 1, 7, 350f, 620f, 820f, 890f),
                word(1, 2, 8, 430f, 700f, 910f, 980f)
            )
        )

        assertEquals(
            MushafPageVerse(1, 2),
            findMushafPageVerseAtPoint(regions, x = 560f, y = 945f)
        )
        assertEquals(
            MushafPageVerse(1, 1),
            findMushafPageVerseAtPoint(regions, x = 480f, y = 850f)
        )
    }

    @Test
    fun blankSpaceAwayFromVerseTextDoesNotSelectAnAyah() {
        val regions = buildMushafPageVerseGeometry(
            listOf(word(1, 2, 8, 430f, 700f, 910f, 980f))
        )

        assertNull(findMushafPageVerseAtPoint(regions, x = 560f, y = 700f))
    }

    @Test
    fun malformedOrOutOfPageWordBoundsAreDiscarded() {
        val regions = buildMushafPageVerseGeometry(
            listOf(
                word(1, 2, 8, 430f, 700f, 910f, 980f),
                word(1, 3, 8, -200f, 800f, 900f, 980f),
                word(1, 4, 8, 100f, 120f, 900f, 1000f),
                word(1, 5, 8, 100f, 900f, 100f, 1000f)
            )
        )

        assertEquals(1, regions.size)
        assertEquals(MushafPageVerse(1, 2), regions.single().verse)
    }
}
