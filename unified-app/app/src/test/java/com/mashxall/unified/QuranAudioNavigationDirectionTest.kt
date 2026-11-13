package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun timedContinuousRecitationShowsAyahNavigationButUntimedFullSurahDoesNot() {
        assertTrue(quranReciterShowsAyahNavigation(audioType = 0))
        assertTrue(quranReciterShowsAyahNavigation(audioType = 1, hasVerifiedAyahTimings = true))
        assertFalse(quranReciterShowsAyahNavigation(audioType = 1))
    }
}