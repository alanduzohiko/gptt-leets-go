package com.mashxall.unified

import com.mashxall.unified.data.MushafPageRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MushafVerseTapSelectionTest {
    @Test
    fun centerTapMapsThroughTheSameFitScaleAsTheRenderedPage() {
        val point = mushafPageTouchPointFromViewport(
            touchX = 180f,
            touchY = 300f,
            viewportWidth = 360f,
            viewportHeight = 600f
        )

        requireNotNull(point)
        assertEquals(574f, point.x, 0.5f)
        assertEquals(820.5f, point.y, 1f)
    }

    @Test
    fun tapsInTheLetterboxedMarginDoNotResolveToPageCoordinates() {
        assertNull(
            mushafPageTouchPointFromViewport(
                touchX = 180f,
                touchY = 10f,
                viewportWidth = 360f,
                viewportHeight = 600f
            )
        )
    }

    @Test
    fun invalidViewportAndTouchCoordinatesAreRejected() {
        assertNull(mushafPageTouchPointFromViewport(Float.NaN, 10f, 360f, 600f))
        assertNull(mushafPageTouchPointFromViewport(10f, 10f, 0f, 600f))
    }

    @Test
    fun verseGeometryBecomesOneTightBandForEachTextLine() {
        val bands = mushafVerseHighlightBands(
            listOf(
                MushafPageRect(1, 2, 100f, 120f, 200f, 220f),
                MushafPageRect(2, 2, 125f, 150f, 201f, 221f),
                MushafPageRect(3, 3, 400f, 430f, 260f, 285f),
                MushafPageRect(4, 3, 434f, 450f, 261f, 284f),
                MushafPageRect(5, 4, 1100f, 1200f, 300f, 340f),
                MushafPageRect(6, 4, 500f, 520f, 400f, 200f)
            )
        )

        assertEquals(2, bands.size)
        assertEquals(2, bands[0].lineNumber)
        assertEquals(100f, bands[0].minX, 0.001f)
        assertEquals(150f, bands[0].maxX, 0.001f)
        assertEquals(200f, bands[0].minY, 0.001f)
        assertEquals(221f, bands[0].maxY, 0.001f)
        assertEquals(3, bands[1].lineNumber)
        assertEquals(400f, bands[1].minX, 0.001f)
        assertEquals(450f, bands[1].maxX, 0.001f)
    }
}
