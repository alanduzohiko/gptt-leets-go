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
    fun fullSurahSelectedAyahSeeksToAnEstimatedStartOffset() {
        val duration = 100_000
        val uniformWeights = List(10) { 10 }

        assertEquals(0, quranPlaybackPositionAtAyahStart(1, 10, duration, uniformWeights))
        assertEquals(40_000, quranPlaybackPositionAtAyahStart(5, 10, duration, uniformWeights))
        val finalAyahStart = quranPlaybackPositionAtAyahStart(10, 10, duration, uniformWeights)
        assertTrue(finalAyahStart > 0)
        assertTrue(finalAyahStart < duration)
    }

    @Test
    fun fullSurahSelectedAyahSeekUsesVerseWeightsAndSafeFallbacks() {
        val weighted = quranPlaybackPositionAtAyahStart(
            targetAyah = 2,
            ayahCount = 3,
            durationMs = 90_000,
            ayahWeights = listOf(10, 20, 30)
        )
        assertEquals(15_000, weighted)
        assertEquals(40_000, quranPlaybackPositionAtAyahStart(5, 10, 100_000))
        assertEquals(0, quranPlaybackPositionAtAyahStart(3, 0, 100_000))
        assertEquals(0, quranPlaybackPositionAtAyahStart(3, 10, 0))
    }

    @Test
    fun fullSurahModeStillStartsAtFirstAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 19, 286))
    }
}