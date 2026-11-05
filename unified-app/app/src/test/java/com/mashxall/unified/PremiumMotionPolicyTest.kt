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
    fun touchHapticsRespectTheAppPreference() {
        assertEquals(true, shouldPerformMashxallHaptic(true))
        assertEquals(false, shouldPerformMashxallHaptic(false))
    }

    @Test
    fun sliderHapticBucketsStayWithinRangeAndRespectDiscreteSteps() {
        assertEquals(0, premiumSliderHapticBucket(0f, 0f, 10f, 4))
        assertEquals(2, premiumSliderHapticBucket(5f, 0f, 10f, 4))
        assertEquals(3, premiumSliderHapticBucket(5.01f, 0f, 10f, 4))
        assertEquals(5, premiumSliderHapticBucket(10f, 0f, 10f, 4))
        assertEquals(0, premiumSliderHapticBucket(-2f, 0f, 10f, 4))
        assertEquals(5, premiumSliderHapticBucket(12f, 0f, 10f, 4))
        assertEquals(10, premiumSliderHapticBucket(5f, 0f, 10f, 0))
        assertEquals(0, premiumSliderHapticBucket(Float.NaN, 0f, 10f, 4))
        assertEquals(0, premiumSliderHapticBucket(5f, 10f, 0f, 4))
    }

    @Test
    fun touchScaleChangesWithControlRoleWithoutExceedingSafeBounds() {
        assertEquals(0.932f, premiumRolePressScale(Role.Button), 0.0001f)
        assertEquals(0.952f, premiumRolePressScale(Role.Tab), 0.0001f)
        assertEquals(0.962f, premiumRolePressScale(null), 0.0001f)
        assertEquals(0.90f, premiumRolePressScale(Role.Button, 0.2f), 0.0001f)
        assertEquals(0.952f, premiumRolePressScale(Role.Tab, 1.2f), 0.0001f)
    }

    @Test
    fun individualControlDepthProducesDistinctPressTilt() {
        val firmButton = premiumControlPressRotationDegrees(Role.Button, 0.94f)
        val softButton = premiumControlPressRotationDegrees(Role.Button, 0.97f)
        val tab = premiumControlPressRotationDegrees(Role.Tab, 0.94f)

        assertEquals(false, firmButton == softButton)
        assertEquals(false, firmButton == tab)
        assertEquals(-0.5584f, firmButton, 0.0001f)
        assertEquals(-0.6244f, softButton, 0.0001f)
    }

    @Test
    fun eachInteractiveRoleHasItsOwnTouchTiltAndGlow() {
        assertEquals(-0.62f, premiumRolePressRotationDegrees(Role.Button), 0.0001f)
        assertEquals(0.24f, premiumRolePressRotationDegrees(Role.Tab), 0.0001f)
        assertEquals(0.38f, premiumRolePressRotationDegrees(null), 0.0001f)
        assertEquals(0.30f, premiumRolePressGlowAlpha(Role.Button), 0.0001f)
        assertEquals(0.18f, premiumRolePressGlowAlpha(Role.Tab), 0.0001f)
        assertEquals(0.23f, premiumRolePressGlowAlpha(null), 0.0001f)
    }

    @Test
    fun choicePillIconsHaveAQuietSelectedSpring() {
        assertEquals(1.10f, premiumChoiceIconScale(true), 0.0001f)
        assertEquals(1f, premiumChoiceIconScale(false), 0.0001f)
        assertEquals(-8f, premiumChoiceIconRotationDegrees(true), 0.0001f)
        assertEquals(0f, premiumChoiceIconRotationDegrees(false), 0.0001f)
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
    fun sharedAmbientBackdropAnimatesAcrossEveryNormalDestination() {
        listOf("home", "quran", "tafsir", "prayer", "qibla", "audio", "hadith", "settings").forEach { screen ->
            assertEquals(screen, true, shouldAnimateAmbientBackdrop(screen, motionAllowed = true, style = "ambient"))
        }
        assertEquals(false, shouldAnimateAmbientBackdrop("immersive_mushaf", motionAllowed = true))
        assertEquals(false, shouldAnimateAmbientBackdrop("quran", motionAllowed = false))
        assertEquals(true, shouldAnimateAmbientBackdrop("home", motionAllowed = true, style = "orbit"))
        assertEquals(true, shouldAnimateAmbientBackdrop("home", motionAllowed = true, style = "ornament"))
        assertEquals(false, shouldAnimateAmbientBackdrop("home", motionAllowed = true, style = "off"))
        assertEquals(true, shouldShowAmbientBackdrop("ornament"))
        assertEquals(false, shouldShowAmbientBackdrop("off"))
    }

    @Test
    fun ambientBackgroundSettingsStayVisibleAcrossReadersAndProtectTheMushaf() {
        assertEquals("ambient", normalizeAmbientBackdropStyle(null))
        assertEquals("orbit", normalizeAmbientBackdropStyle("orbit"))
        assertEquals("ambient", normalizeAmbientBackdropStyle("unknown"))
        assertEquals(0.82f, normalizeAmbientBackdropIntensity(Float.NaN), 0.0001f)
        assertEquals(0.20f, normalizeAmbientBackdropIntensity(0f), 0.0001f)
        assertEquals(1f, normalizeAmbientBackdropIntensity(2f), 0.0001f)
        assertEquals(0.72f, normalizeAmbientBackdropSpeed(Float.NaN), 0.0001f)
        assertEquals(0.45f, normalizeAmbientBackdropSpeed(0.1f), 0.0001f)
        assertEquals(1.45f, normalizeAmbientBackdropSpeed(2f), 0.0001f)
        assertEquals(0.72f, ambientBackdropIntensityForScreen("quran", 1f), 0.0001f)
        assertEquals(0.66f, ambientBackdropIntensityForScreen("tafsir", 1f), 0.0001f)
        assertEquals(0.82f, ambientBackdropIntensityForScreen("prayer", 0.82f), 0.0001f)
        assertEquals(0.72f, premiumScreenOrnamentIntensity("quran", 1f), 0.0001f)
        assertEquals(0.66f, premiumScreenOrnamentIntensity("tafsir", 1f), 0.0001f)
        assertEquals(0.78f, premiumScreenOrnamentIntensity("prayer", 1f), 0.0001f)
        assertEquals(0.16f, premiumScreenOrnamentIntensity("quran", 1f, immersiveMushaf = true), 0.0001f)
        assertEquals(0.156f, premiumScreenOrnamentIntensity("prayer", 0f), 0.0001f)
        assertEquals(20_000, ambientMotionDurationMillis(10_000, 0.5f))
        assertEquals(10_000, ambientMotionDurationMillis(10_000, 1f))
        assertEquals(6_897, ambientMotionDurationMillis(10_000, 1.45f))
        assertEquals(22_222, ambientMotionDurationMillis(10_000, 0.45f))
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