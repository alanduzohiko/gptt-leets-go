package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPauseControlTest {
    @Test
    fun pauseControlIsUnavailableWhileAnAssetIsPreparing() {
        assertFalse(audioPauseControlEnabled(isPreparing = true, isPrepared = false))
        assertFalse(audioPauseControlEnabled(isPreparing = true, isPrepared = true))
    }

    @Test
    fun pauseControlIsUnavailableUntilThePlayerIsPrepared() {
        assertFalse(audioPauseControlEnabled(isPreparing = false, isPrepared = false))
    }

    @Test
    fun pauseControlBecomesAvailableOnlyForThePreparedActivePlayer() {
        assertTrue(audioPauseControlEnabled(isPreparing = false, isPrepared = true))
    }
}
