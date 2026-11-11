package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranPlaybackTimingTest {
    @Test
    fun verseWeightsIncludeWordLengthAndQuranicPauseMarks() {
        val shortVerse = quranPlaybackAyahWeight("قُلْ هُوَ اللَّهُ أَحَدٌ")
        val longVerse = quranPlaybackAyahWeight("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ")
        assertTrue(shortVerse >= 12)
        assertTrue(longVerse > shortVerse)
    }

    @Test
    fun pauseSymbolsAddEstimatedTimeToAFullSurahVerse() {
        val withoutPause = quranPlaybackAyahWeight("الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ")
        val withPause = quranPlaybackAyahWeight("الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝")
        assertEquals(12, withPause - withoutPause)
    }

    @Test
    fun fullSurahModeStillStartsAtFirstAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 19, 286))
    }
}