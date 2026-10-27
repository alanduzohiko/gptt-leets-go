package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPlaybackModeTest {
    @Test
    fun fullSurahAlwaysStartsAtTheFirstAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 17, 286))
        assertEquals(1, resolveReciterStartAyah("full", 1, 7))
    }

    @Test
    fun selectedAyahModeStartsAtTheRequestedAyah() {
        assertEquals(17, resolveReciterStartAyah("verse", 17, 286))
        assertEquals(1, resolveReciterStartAyah("verse", 0, 7))
        assertEquals(7, resolveReciterStartAyah("verse", 12, 7))
    }

    @Test
    fun protectsAgainstMissingAyahCounts() {
        assertEquals(1, resolveReciterStartAyah("verse", 4, 0))
        assertEquals(1, resolveReciterStartAyah("verse", -1, -10))
    }
}
