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

    @Test
    fun introVideoCenterCropFillsTheViewportWithoutStretching() {
        val narrowViewport = introVideoCenterCropScale(
            viewWidth = 480,
            viewHeight = 1000,
            videoWidth = 720,
            videoHeight = 1280
        )
        assertTrue(kotlin.math.abs(narrowViewport.first - 1.171875f) < 0.0001f)
        assertTrue(kotlin.math.abs(narrowViewport.second - 1f) < 0.0001f)

        val wideViewport = introVideoCenterCropScale(
            viewWidth = 900,
            viewHeight = 1500,
            videoWidth = 720,
            videoHeight = 1280
        )
        assertTrue(kotlin.math.abs(wideViewport.first - 1f) < 0.0001f)
        assertTrue(kotlin.math.abs(wideViewport.second - (1.0666667f)) < 0.0001f)
        assertTrue(introVideoCenterCropScale(0, 1000, 720, 1280) == (1f to 1f))
    }
}
