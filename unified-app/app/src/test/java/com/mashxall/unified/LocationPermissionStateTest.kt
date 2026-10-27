package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionStateTest {
    @Test
    fun savedGpsCoordinatesAreClearedWhenLocationPermissionIsRevoked() {
        assertTrue(shouldClearSavedGpsFix(false, 35.56, 45.43))
        assertTrue(shouldClearSavedGpsFix(false, 35.56, null))
        assertTrue(shouldClearSavedGpsFix(false, null, 45.43))
    }

    @Test
    fun validPermissionKeepsSavedCoordinates() {
        assertFalse(shouldClearSavedGpsFix(true, 35.56, 45.43))
        assertFalse(shouldClearSavedGpsFix(true, 35.56, null))
    }

    @Test
    fun noSavedCoordinatesDoNotTriggerUnnecessaryCleanup() {
        assertFalse(shouldClearSavedGpsFix(false, null, null))
    }
}
