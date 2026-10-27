package com.mashxall.unified.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerAlarmCitySelectionTest {
    @Test
    fun missingCityDoesNotSilentlyChooseErbil() {
        assertNull(selectedCityForAlarm(savedCity = null, hasManualCoordinates = false))
        assertNull(selectedCityForAlarm(savedCity = "", hasManualCoordinates = false))
        assertNull(selectedCityForAlarm(savedCity = "   ", hasManualCoordinates = false))
        assertNull(selectedCityForAlarm(savedCity = "Erbil", hasManualCoordinates = false))
        assertNull(selectedCityForAlarm(savedCity = "ERBIL", hasManualCoordinates = false))
    }

    @Test
    fun anExplicitNonDefaultCityCanBeUsedForAlarms() {
        assertEquals(
            "Sulaymaniyah",
            selectedCityForAlarm(savedCity = " Sulaymaniyah ", hasManualCoordinates = false)
        )
        assertEquals(
            "Duhok",
            selectedCityForAlarm(savedCity = "Duhok", hasManualCoordinates = false)
        )
    }

    @Test
    fun explicitlySelectedErbilIsValidWhenItsCoordinatesWereSaved() {
        assertEquals(
            "Erbil",
            selectedCityForAlarm(savedCity = "Erbil", hasManualCoordinates = true)
        )
    }
}
