package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSelectionTest {
    @Test
    fun explicitManualCityPreventsAutomaticGpsReplacement() {
        assertFalse(shouldAutoRequestLocation(null, null, "Sulaymaniyah", 35.56, 45.44))
    }

    @Test
    fun gpsCanBeRequestedWhenThereIsNoManualCity() {
        assertTrue(shouldAutoRequestLocation(null, null, "", null, null))
        assertTrue(shouldAutoRequestLocation(null, null, "Erbil", null, null))
    }

    @Test
    fun existingGpsFixTakesPriorityOverManualCityCoordinates() {
        assertFalse(shouldAutoRequestLocation(36.19, 44.01, "Sulaymaniyah", 35.56, 45.44))
    }

    @Test
    fun incompleteGpsFixDoesNotReplaceAValidManualCity() {
        assertFalse(shouldAutoRequestLocation(36.19, null, "Halabja", 35.18, 45.99))
    }
}
