package com.mashxall.unified.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedPrayerCityTest {
    @Test
    fun blankSavedCityIsNotReplacedWithErbil() {
        assertNull(usableSavedPrayerCityName("", hasManualCoordinates = false))
        assertNull(usableSavedPrayerCityName(null, hasManualCoordinates = false))
    }

    @Test
    fun oldErbilPlaceholderIsRejectedWithoutCoordinates() {
        assertNull(usableSavedPrayerCityName("Erbil", hasManualCoordinates = false))
        assertNull(usableSavedPrayerCityName("ERBIL", hasManualCoordinates = false))
    }

    @Test
    fun explicitlySelectedErbilRemainsValidWhenItsCoordinatesAreSaved() {
        assertEquals("Erbil", usableSavedPrayerCityName("Erbil", hasManualCoordinates = true))
    }

    @Test
    fun otherSavedCitiesRemainAvailable() {
        assertEquals("Halabja", usableSavedPrayerCityName(" Halabja ", hasManualCoordinates = false))
    }
}
