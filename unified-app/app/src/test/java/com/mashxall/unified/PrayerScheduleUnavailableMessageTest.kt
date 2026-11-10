package com.mashxall.unified

import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerScheduleUnavailableMessageTest {
    @Test
    fun gpsFailureExplainsThatLocationAndTimetableAreDifferent() {
        val message = prayerScheduleUnavailableMessage(
            AppLanguage.EN, "gps", "",
            nearestCityName = "Sulaymaniyah",
            nearestCityCountry = "Iraq",
            nearestDistanceKm = "12.4"
        )
        assertTrue(message.contains("GPS position"))
        assertTrue(message.contains("Sulaymaniyah"))
        assertTrue(message.contains("12.4 km"))
        assertTrue(message.contains("never guessed"))
    }

    @Test
    fun cityFailureNamesTheSelectedCityAndTheActualRecoveryAction() {
        val message = prayerScheduleUnavailableMessage(
            AppLanguage.AR, "city", "السليمانية"
        )
        assertTrue(message.contains("السليمانية"))
        assertTrue(message.contains("زر البحث"))
        assertTrue(message.contains("لن نخمن"))
    }

    @Test
    fun blankCityUsesALocalizedFallbackLabel() {
        val message = prayerScheduleUnavailableMessage(
            AppLanguage.CKB, "city", ""
        )
        assertTrue(message.contains("شاری هەڵبژێردراو"))
        assertTrue(message.contains("گەڕانی سەرەوە"))
    }
}
