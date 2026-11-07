package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranSurahOriginFilterTest {
    @Test
    fun allFilterKeepsKnownAndUnknownOrigins() {
        assertTrue(matchesSurahOriginFilter("Meccan", "all"))
        assertTrue(matchesSurahOriginFilter("Medinan", "all"))
        assertTrue(matchesSurahOriginFilter("Unknown", "all"))
    }

    @Test
    fun meccanFilterRecognizesSourceAliases() {
        assertTrue(matchesSurahOriginFilter("Meccan", "meccan"))
        assertTrue(matchesSurahOriginFilter("Makki", "meccan"))
        assertTrue(matchesSurahOriginFilter("Makkah", "meccan"))
        assertFalse(matchesSurahOriginFilter("Medinan", "meccan"))
    }

    @Test
    fun medinanFilterRecognizesSourceAliases() {
        assertTrue(matchesSurahOriginFilter("Medinan", "medinan"))
        assertTrue(matchesSurahOriginFilter("Madani", "medinan"))
        assertTrue(matchesSurahOriginFilter("Madinah", "medinan"))
        assertFalse(matchesSurahOriginFilter("Meccan", "medinan"))
    }

    @Test
    fun unknownOriginIsNotMisclassified() {
        assertFalse(matchesSurahOriginFilter("Unknown", "meccan"))
        assertFalse(matchesSurahOriginFilter("", "medinan"))
    }
}
