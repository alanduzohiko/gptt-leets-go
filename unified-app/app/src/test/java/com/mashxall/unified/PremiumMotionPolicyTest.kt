package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumMotionPolicyTest {
    @Test
    fun staggerDelayStartsImmediatelyAndCapsTheCascade() {
        assertEquals(0L, staggerDelayMillis(0))
        assertEquals(40L, staggerDelayMillis(1))
        assertEquals(160L, staggerDelayMillis(4))
        assertEquals(320L, staggerDelayMillis(8))
        assertEquals(320L, staggerDelayMillis(99))
    }

    @Test
    fun negativePositionsNeverAddAnUnexpectedDelay() {
        assertEquals(0L, staggerDelayMillis(-1))
    }

    @Test
    fun pressFeedbackIsSubtleAndNeverScalesDisabledControls() {
        assertEquals(0.968f, premiumPressTargetScale(enabled = true, pressed = true), 0.0001f)
        assertEquals(1f, premiumPressTargetScale(enabled = true, pressed = false), 0.0001f)
        assertEquals(1f, premiumPressTargetScale(enabled = false, pressed = true), 0.0001f)
        assertEquals(0.90f, premiumPressTargetScale(enabled = true, pressed = true, pressedScale = 0.2f), 0.0001f)
        assertEquals(1f, premiumPressTargetScale(enabled = true, pressed = true, pressedScale = 1.2f), 0.0001f)
        assertEquals(0.968f, premiumPressTargetScale(enabled = true, pressed = true, pressedScale = Float.NaN), 0.0001f)
    }

    @Test
    fun rootNavigationDirectionFollowsTabOrder() {
        assertEquals(1, rootNavigationDirection("home", "quran"))
        assertEquals(-1, rootNavigationDirection("settings", "more"))
        assertEquals(1, rootNavigationDirection("quran", "quran"))
        assertEquals(1, rootNavigationDirection("prayer", "home"))
    }

    @Test
    fun slideDirectionRespectsRtlAndBackNavigation() {
        assertEquals(1, navigationSlideSign(isRtl = false, direction = 1))
        assertEquals(-1, navigationSlideSign(isRtl = false, direction = -1))
        assertEquals(-1, navigationSlideSign(isRtl = true, direction = 1))
        assertEquals(1, navigationSlideSign(isRtl = true, direction = -1))
    }

    @Test
    fun ambientBackdropOnlyRunsOnHomeWhenMotionIsAllowed() {
        assertEquals(true, shouldAnimateAmbientBackdrop("home", motionAllowed = true))
        assertEquals(false, shouldAnimateAmbientBackdrop("quran", motionAllowed = true))
        assertEquals(false, shouldAnimateAmbientBackdrop("prayer", motionAllowed = true))
        assertEquals(false, shouldAnimateAmbientBackdrop("home", motionAllowed = false))
        assertEquals(false, shouldAnimateAmbientBackdrop("hadith", motionAllowed = false))
    }

    @Test
    fun contentSizeMotionRespectsSystemPreferenceAndLocalDisable() {
        assertEquals(true, shouldAnimateContentSize(motionAllowed = true))
        assertEquals(false, shouldAnimateContentSize(motionAllowed = false))
        assertEquals(false, shouldAnimateContentSize(motionAllowed = true, enabled = false))
    }

}