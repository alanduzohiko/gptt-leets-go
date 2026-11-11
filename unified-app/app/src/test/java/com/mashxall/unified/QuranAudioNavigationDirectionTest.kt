package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranAudioNavigationDirectionTest {
    @Test
    fun previousAyahArrowPointsRightForRtlReading() {
        assertEquals(0f, quranAudioNavigationArrowRotation(isNext = false), 0f)
    }

    @Test
    fun nextAyahArrowPointsLeftForRtlReading() {
        assertEquals(180f, quranAudioNavigationArrowRotation(isNext = true), 0f)
    }
}