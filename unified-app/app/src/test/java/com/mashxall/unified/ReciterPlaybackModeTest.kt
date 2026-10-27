package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterPlaybackModeTest {
    @Test
    fun verseByVerseModeAcceptsOnlyPerAyahSources() {
        assertTrue(reciterAudioModeMatches(audioType = 0, playbackMode = 0))
        assertFalse(reciterAudioModeMatches(audioType = 1, playbackMode = 0))
        assertFalse(reciterAudioModeMatches(audioType = 2, playbackMode = 0))
    }

    @Test
    fun fullSurahModeAcceptsFullRecordingSources() {
        assertFalse(reciterAudioModeMatches(audioType = 0, playbackMode = 1))
        assertTrue(reciterAudioModeMatches(audioType = 1, playbackMode = 1))
        assertTrue(reciterAudioModeMatches(audioType = 2, playbackMode = 1))
    }

    @Test
    fun unknownPlaybackModeUsesFullSurahSemanticsSafely() {
        assertFalse(reciterAudioModeMatches(audioType = 0, playbackMode = 9))
        assertTrue(reciterAudioModeMatches(audioType = 1, playbackMode = 9))
    }
}
