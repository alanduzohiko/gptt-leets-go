package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranPlaybackTimingTest {
    @Test
    fun continuousFullSurahFileStartsAtBeginningWithoutFabricatedOffsets() {
        val plan = quranPlaybackStartPlan(audioType = 1, requestedAyah = 1, ayahCount = 114)
        assertTrue(plan.isSupported)
        assertFalse(plan.usesAyahClips)
        assertEquals(1, plan.startingAyah)
    }

    @Test
    fun continuousFullSurahFileCannotPretendToSeekToAnUnmarkedVerse() {
        val plan = quranPlaybackStartPlan(audioType = 1, requestedAyah = 43, ayahCount = 114)
        assertFalse(plan.isSupported)
        assertFalse(plan.usesAyahClips)
    }

    @Test
    fun continuousFullSurahWithVerifiedAyahTimesCanSeekToItsChosenVerse() {
        val plan = quranPlaybackStartPlan(
            audioType = 1,
            requestedAyah = 43,
            ayahCount = 114,
            hasAyahTimings = true
        )
        assertTrue(plan.isSupported)
        assertFalse(plan.usesAyahClips)
        assertEquals(43, plan.startingAyah)
    }


    @Test
    fun timedContinuousRecordingUsesPreviousAndNextAyahControls() {
        val navigation = quranInlineNavigationAvailability(
            usesAyahClips = false,
            timedAyahCount = 286,
            activeAyah = 43,
            startingAyah = 43,
            ayahCount = 286,
            surahNumber = 2,
            availableSurahs = 114
        )
        assertTrue(navigation.navigatesByAyah)
        assertTrue(navigation.hasPrevious)
        assertTrue(navigation.hasNext)
    }

    @Test
    fun fullSurahNavigationDoesNotDependOnPlayerDurationBeingKnown() {
        val navigation = quranInlineNavigationAvailability(
            usesAyahClips = false,
            timedAyahCount = 0,
            activeAyah = 1,
            startingAyah = 1,
            ayahCount = 286,
            surahNumber = 2,
            availableSurahs = 114
        )
        assertFalse(navigation.navigatesByAyah)
        assertTrue(navigation.hasPrevious)
        assertTrue(navigation.hasNext)
    }

    @Test
    fun ayahClipNavigationRemainsInsideItsLoadedSequence() {
        val first = quranInlineNavigationAvailability(
            usesAyahClips = true,
            timedAyahCount = 0,
            activeAyah = 43,
            startingAyah = 43,
            ayahCount = 286,
            surahNumber = 2,
            availableSurahs = 114
        )
        assertFalse(first.hasPrevious)
        assertTrue(first.hasNext)

        val later = quranInlineNavigationAvailability(
            usesAyahClips = true,
            timedAyahCount = 0,
            activeAyah = 44,
            startingAyah = 43,
            ayahCount = 286,
            surahNumber = 2,
            availableSurahs = 114
        )
        assertTrue(later.hasPrevious)
        assertTrue(later.hasNext)
    }

    @Test
    fun exactAyahEditionStartsAtTheSelectedVerseAndContinuesToTheEnd() {
        val plan = quranPlaybackStartPlan(audioType = 0, requestedAyah = 43, ayahCount = 114)
        assertTrue(plan.isSupported)
        assertTrue(plan.usesAyahClips)
        assertEquals(43, plan.startingAyah)
        assertEquals(
            listOf(43, 44, 45, 46),
            quranPlaybackTrackAyahs(audioType = 0, startingAyah = plan.startingAyah, ayahCount = 46)
        )
    }

    @Test
    fun selectedAyahIsClampedToTheActualSurahRange() {
        assertEquals(1, quranPlaybackStartPlan(0, -5, 7).startingAyah)
        assertEquals(7, quranPlaybackStartPlan(0, 90, 7).startingAyah)
    }

    @Test
    fun aMissingSurahCannotCreateAPlaybackPlan() {
        assertFalse(quranPlaybackStartPlan(0, 1, 0).isSupported)
    }

    @Test
    fun legacyModeHelperStillDefinesTheFullFileDefaultAsTheFirstAyah() {
        assertEquals(1, resolveReciterStartAyah("full", 19, 286))
    }
}
