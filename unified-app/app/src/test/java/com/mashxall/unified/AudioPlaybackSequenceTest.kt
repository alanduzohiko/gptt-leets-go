package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackSequenceTest {
    @Test
    fun unavailableVerseTrackSkipsToTheNextAyah() {
        assertEquals(
            AudioSequenceFailureDecision(nextIndex = 1, showFailureMessage = false),
            audioSequenceFailureDecision(currentIndex = 0, totalTracks = 114, hasStartedPlayback = false)
        )
    }

    @Test
    fun aMissingTrackAfterPlaybackStartedDoesNotMarkTheWholeSurahAsFailed() {
        val decision = audioSequenceFailureDecision(
            currentIndex = 23,
            totalTracks = 286,
            hasStartedPlayback = true
        )
        assertEquals(24, decision.nextIndex)
        assertFalse(decision.showFailureMessage)
    }

    @Test
    fun finalMissingTrackShowsErrorOnlyWhenNoTrackEverPlayed() {
        val neverStarted = audioSequenceFailureDecision(0, 1, hasStartedPlayback = false)
        assertEquals(null, neverStarted.nextIndex)
        assertTrue(neverStarted.showFailureMessage)

        val playedEarlier = audioSequenceFailureDecision(113, 114, hasStartedPlayback = true)
        assertEquals(null, playedEarlier.nextIndex)
        assertFalse(playedEarlier.showFailureMessage)
    }

    @Test
    fun sequenceEndsWithoutSkippingBeyondTheLastTrack() {
        assertEquals(
            AudioSequenceFailureDecision(nextIndex = null, showFailureMessage = true),
            audioSequenceFailureDecision(4, 5, hasStartedPlayback = false)
        )
    }
}
