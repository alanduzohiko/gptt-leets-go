package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafPanBoundsTest {
    @Test
    fun panRemainsWithinTheScaledPageEdges() {
        assertEquals(120f, clampMushafPanOffset(100f, 80f, 640f, 400f), 0.001f)
        assertEquals(-120f, clampMushafPanOffset(-100f, -80f, 640f, 400f), 0.001f)
    }

    @Test
    fun panCanMoveFreelyInsideTheAllowedRange() {
        assertEquals(35f, clampMushafPanOffset(10f, 25f, 700f, 400f), 0.001f)
        assertEquals(-40f, clampMushafPanOffset(-15f, -25f, 700f, 400f), 0.001f)
    }

    @Test
    fun smallerContentAndInvalidInputsNeverCreateAnOffset() {
        assertEquals(0f, clampMushafPanOffset(0f, 120f, 280f, 400f), 0.001f)
        assertEquals(0f, clampMushafPanOffset(Float.NaN, 20f, 600f, 400f), 0.001f)
        assertEquals(0f, clampMushafPanOffset(0f, 20f, 600f, 0f), 0.001f)
    }
}
