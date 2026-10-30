package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerSourceDescriptionTest {
    @Test
    fun localizesDirectSalawatiSourceWithoutLeakingRawRoutingKeys() {
        val english = prayerSourceDescription("salawati:Sulaymaniyah, Iraq", AppLanguage.EN)
        val arabic = prayerSourceDescription("salawati:Sulaymaniyah, Iraq", AppLanguage.AR)
        val sorani = prayerSourceDescription("salawati:Sulaymaniyah, Iraq", AppLanguage.CKB)
        assertTrue(english.contains("Salawati original timetable"))
        assertTrue(arabic.contains("جدول صلواتي الأصلي"))
        assertTrue(sorani.contains("خشتەی ڕەسەنی صەڵەواتی"))
        assertFalse(english.contains("salawati:"))
    }

    @Test
    fun describesLinkedLocalityAndSourceDistanceInThreeLanguages() {
        val source = "salawati-dependent:Kalak → Erbil, Iraq • 3.4km"
        val english = prayerSourceDescription(source, AppLanguage.EN)
        val arabic = prayerSourceDescription(source, AppLanguage.AR)
        val sorani = prayerSourceDescription(source, AppLanguage.CKB)
        assertTrue(english.contains("linked locality"))
        assertTrue(english.contains("3.4 km away"))
        assertTrue(arabic.contains("مدينة مرتبطة"))
        assertTrue(arabic.contains("3.4 كم"))
        assertTrue(sorani.contains("شاری بەستراو"))
        assertTrue(sorani.contains("3.4 کیلۆمەتر"))
        assertFalse(sorani.contains("salawati-dependent"))
    }

    @Test
    fun refusesToDescribeUnknownSourceAsSalawati() {
        val message = prayerSourceDescription("noor-base:Erbil", AppLanguage.AR)
        assertTrue(message.contains("مصدر جدول الصلاة غير متاح"))
        assertFalse(message.contains("نور"))
        assertFalse(message.contains("noor-base"))
    }
}
