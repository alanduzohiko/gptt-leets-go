package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class MashxallTypographyTest {
    @Test
    fun raisesCompactDefaultSystemTextToReadableBaseline() {
        assertEquals(1.15f, mashxallReadableFontScale(1.0f), 0.0001f)
        assertEquals(1.15f, mashxallReadableFontScale(0.85f), 0.0001f)
    }

    @Test
    fun preservesLargerAndroidAccessibilityFontScale() {
        assertEquals(1.35f, mashxallReadableFontScale(1.35f), 0.0001f)
    }

    @Test
    fun invalidSystemScaleFallsBackToReadableBaseline() {
        assertEquals(1.15f, mashxallReadableFontScale(Float.NaN), 0.0001f)
        assertEquals(1.15f, mashxallReadableFontScale(0f), 0.0001f)
    }
}
