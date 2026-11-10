package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MashxallStartupAnimationPolicyTest {
    @Test
    fun completedOrLegacyInstallSkipsColdStartEntranceButFirstRunDoesNot() {
        assertTrue(shouldSuppressMashxallColdStartEntrance(
            firstRunSetupComplete = true,
            legacyUserState = false
        ))
        assertTrue(shouldSuppressMashxallColdStartEntrance(
            firstRunSetupComplete = false,
            legacyUserState = true
        ))
        assertFalse(shouldSuppressMashxallColdStartEntrance(
            firstRunSetupComplete = false,
            legacyUserState = false
        ))
    }

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
    fun returningInstallWithSavedDataSkipsIntroEvenWhenPreviousOnboardingWasStarted() {
        // first_run_setup_started may already be true on an upgraded install.
        // Saved app preferences must still keep that user out of the startup video.
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
