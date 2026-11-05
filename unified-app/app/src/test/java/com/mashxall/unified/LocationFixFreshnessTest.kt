package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixFreshnessTest {
    @Test
    fun acceptsRecentLocationAndRejectsStaleOrFutureFixes() {
        val now = 1_800_000_000_000L
        assertTrue(isRecentLocationFix(now, now))
        assertTrue(isRecentLocationFix(now - 10 * 60_000L, now))
        assertFalse(isRecentLocationFix(now - 31 * 60_000L, now))
        assertFalse(isRecentLocationFix(now + 1_000L, now))
    }

    @Test
    fun callersCanUseAStricterWindowToIdentifyLiveFixes() {
        val now = 1_800_000_000_000L
        assertTrue(isRecentLocationFix(now - 30_000L, now, 60_000L))
        assertFalse(isRecentLocationFix(now - 5 * 60_000L, now, 60_000L))
    }

    @Test
    fun currentFixMustHaveRecentTimestampValidCoordinatesAndPlausibleAccuracy() {
        val now = 1_800_000_000_000L
        assertTrue(isUsableIncomingLocation(now - 5_000L, 36.19, 44.01, now, 18f))
        assertFalse(isUsableIncomingLocation(now - 90_000L, 36.19, 44.01, now, 18f))
        assertFalse(isUsableIncomingLocation(now + 1_000L, 36.19, 44.01, now, 18f))
        assertFalse(isUsableIncomingLocation(now, 91.0, 44.01, now, 18f))
        assertFalse(isUsableIncomingLocation(now, 36.19, 181.0, now, 18f))
        assertFalse(isUsableIncomingLocation(now, 36.19, 44.01, now, 60_000f))
    }

    @Test
    fun firstSavedLocationRemainsUsableAcrossManyAppLaunchesAndWhenGpsIsOff() {
        val now = 1_800_000_000_000L
        val savedOneYearAgo = now - 365L * 24L * 60L * 60L * 1000L

        // Saved coordinates are the user's chosen place; they do not expire just
        // because the device's Location switch was off or the app was not opened.
        assertTrue(isValidPersistedGpsFix(36.19, 44.01, savedOneYearAgo))
        assertFalse(isValidPersistedGpsFix(91.0, 44.01, savedOneYearAgo))
        assertFalse(isValidPersistedGpsFix(36.19, -181.0, savedOneYearAgo))
        assertFalse(isValidPersistedGpsFix(36.19, 44.01, 0L))
    }

    @Test
    fun savedGpsCoordinatesRequireARecentTimestampAndValidCoordinateBounds() {
        val now = 1_800_000_000_000L
        assertTrue(isUsableSavedGpsFix(36.19, 44.01, now - 20 * 60_000L, now))
        assertFalse(isUsableSavedGpsFix(36.19, 44.01, now - 31 * 60_000L, now))
        assertFalse(isUsableSavedGpsFix(36.19, 44.01, 0L, now))
        assertFalse(isUsableSavedGpsFix(91.0, 44.01, now, now))
        assertFalse(isUsableSavedGpsFix(36.19, -181.0, now, now))
        assertFalse(isUsableSavedGpsFix(36.19, 44.01, now + 1_000L, now))
    }
}
