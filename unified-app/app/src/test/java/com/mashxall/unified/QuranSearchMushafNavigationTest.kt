package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranSearchMushafNavigationTest {
    @Test
    fun prefersThePageForTheSelectedMushafEdition() {
        assertEquals(217, resolveQuranSearchMushafPage(217, 216, 215))
    }

    @Test
    fun fallsBackToTheOriginalGraphicMushafPageWhenTajweedHasNoMapping() {
        assertEquals(216, resolveQuranSearchMushafPage(null, 216, 215))
    }

    @Test
    fun usesThePageStoredOnTheSearchResultAsTheLastValidFallback() {
        assertEquals(215, resolveQuranSearchMushafPage(null, null, 215))
    }

    @Test
    fun rejectsOutOfRangePagesInsteadOfOpeningAnotherReader() {
        assertNull(resolveQuranSearchMushafPage(0, 605, null))
        assertNull(resolveQuranSearchMushafPage(-1, 900, 0))
    }
}
