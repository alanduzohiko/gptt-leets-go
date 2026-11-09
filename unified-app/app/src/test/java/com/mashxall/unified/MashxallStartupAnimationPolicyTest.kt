package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MashxallStartupAnimationPolicyTest {
    @Test
    fun brandNewInstallPlaysTheIntroOnce() {
        assertTrue(
            shouldPlayMashxallStartupAnimation(
                startupPlayed = false,
                startupStarted = false,
                firstRunSetupComplete = false,
                legacyUserState = false
            )
        )
    }

    @Test
    fun alreadyPlayedIntroIsNeverRepeated() {
        assertFalse(
            shouldPlayMashxallStartupAnimation(
                startupPlayed = true,
                startupStarted = false,
                firstRunSetupComplete = false,
                legacyUserState = false
            )
        )
    }

    @Test
    fun interruptedFirstRunDoesNotReplayTheIntroOnEveryColdStart() {
        assertFalse(
            shouldPlayMashxallStartupAnimation(
                startupPlayed = false,
                startupStarted = true,
                firstRunSetupComplete = false,
                legacyUserState = false
            )
        )
    }

    @Test
    fun completedSetupSkipsAnyLegacyIntroReplay() {
        assertFalse(
            shouldPlayMashxallStartupAnimation(
                startupPlayed = false,
                startupStarted = false,
                firstRunSetupComplete = true,
                legacyUserState = false
            )
        )
    }

    @Test
    fun olderConfiguredInstallWithoutNewSetupFlagsSkipsTheIntro() {
        assertFalse(
            shouldPlayMashxallStartupAnimation(
                startupPlayed = false,
                startupStarted = false,
                firstRunSetupComplete = false,
                legacyUserState = true
            )
        )
    }
}
