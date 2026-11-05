package com.mashxall.unified

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
}
