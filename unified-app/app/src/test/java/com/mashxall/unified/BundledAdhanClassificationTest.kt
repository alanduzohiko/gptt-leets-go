package com.mashxall.unified

import com.mashxall.unified.data.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BundledAdhanClassificationTest {
    @Test
    fun recognizesArabicNamedMuezzinTracksFromOriginalBundles() {
        assertEquals(
            "muezzin",
            bundledAdhanCategory(AudioItem("content/noor/audio/أذان الفجر.mp3", "أذان الفجر", ""))
        )
        assertEquals(
            "muezzin",
            bundledAdhanCategory(AudioItem("content/salawati/المؤذن_الكبير.ogg", "المؤذن الكبير", ""))
        )
    }

    @Test
    fun recognizesArabicNamedMosqueRecordings() {
        assertEquals(
            "mosque",
            bundledAdhanCategory(AudioItem("content/khairah/صوت المسجد الحرام.mp3", "صوت المسجد الحرام", ""))
        )
        assertEquals(
            "mosque",
            bundledAdhanCategory(AudioItem("content/khairah/المدينة المنورة.mp3", "المدينة المنورة", ""))
        )
    }

    @Test
    fun doesNotTreatGenericQuranRecitationOrAzkarAsAdhan() {
        assertNull(
            bundledAdhanCategory(AudioItem("content/noor/audio/تلاوة القرآن الكريم.mp3", "تلاوة القرآن الكريم", ""))
        )
        assertNull(
            bundledAdhanCategory(AudioItem("content/noor/audio/دعاء الصباح.mp3", "دعاء الصباح", ""))
        )
    }

    @Test
    fun preservesExistingEnglishSourceFilenameClassification() {
        assertEquals(
            "muezzin",
            bundledAdhanCategory(AudioItem("content/adhan/Abdul_Majid_Al_Surehi.mp3", "Abdul Majid Al Surehi", "adhan"))
        )
        assertEquals(
            "mosque",
            bundledAdhanCategory(AudioItem("content/mosque/Masjid_Al_Haram.mp3", "Masjid Al Haram", "mosque"))
        )
    }
}
