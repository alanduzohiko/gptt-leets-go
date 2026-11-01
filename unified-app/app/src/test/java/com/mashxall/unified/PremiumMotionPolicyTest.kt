package com.mashxall.unified

import androidx.compose.ui.semantics.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumMotionPolicyTest {
    @Test
    fun decorativeMotionRequiresBothSystemAndAppPermission() {
        assertEquals(true, shouldAllowMashxallMotion(true, 1f, false))
        assertEquals(false, shouldAllowMashxallMotion(true, 1f, true))
        assertEquals(false, shouldAllowMashxallMotion(false, 1f, false))
        assertEquals(false, shouldAllowMashxallMotion(true, 0f, false))
        assertEquals(false, shouldAllowMashxallMotion(true, -1f, false))
        assertEquals(false, shouldAllowMashxallMotion(true, Float.NaN, false))
        assertEquals(false, shouldAllowMashxallMotion(true, Float.POSITIVE_INFINITY, false))
    }

    @Test
    fun touchScaleChangesWithControlRoleWithoutExceedingSafeBounds() {
        assertEquals(0.946f, premiumRolePressScale(Role.Button), 0.0001f)
        assertEquals(0.958f, premiumRolePressScale(Role.Tab), 0.0001f)
        assertEquals(0.968f, premiumRolePressScale(null), 0.0001f)
        assertEquals(0.90f, premiumRolePressScale(Role.Button, 0.2f), 0.0001f)
        assertEquals(0.958f, premiumRolePressScale(Role.Tab, 1.2f), 0.0001f)
    }

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

    @Test
    fun islamicRosetteVerticesKeepEightfoldSymmetryAndAlternatingRadii() {
        val outerTop = premiumOrnamentVertex(0, outerRadius = 50f, innerRadius = 24f)
        val innerNext = premiumOrnamentVertex(1, outerRadius = 50f, innerRadius = 24f)
        val oppositeOuter = premiumOrnamentVertex(8, outerRadius = 50f, innerRadius = 24f)

        assertEquals(0f, outerTop.x, 0.0001f)
        assertEquals(-50f, outerTop.y, 0.0001f)
        assertEquals(24f, kotlin.math.hypot(innerNext.x, innerNext.y), 0.0001f)
        assertEquals(-outerTop.x, oppositeOuter.x, 0.0001f)
        assertEquals(-outerTop.y, oppositeOuter.y, 0.0001f)
    }

    @Test
    fun islamicRosetteVertexInputIsWrappedAndClampedSafely() {
        assertEquals(
            premiumOrnamentVertex(0, 40f, 20f),
            premiumOrnamentVertex(-16, 40f, 20f)
        )
        assertEquals(
            premiumOrnamentVertex(1, 10f, 10f),
            premiumOrnamentVertex(1, 10f, 50f)
        )
        assertEquals(
            androidx.compose.ui.geometry.Offset.Zero,
            premiumOrnamentVertex(0, Float.NaN, 20f)
        )
    }

}