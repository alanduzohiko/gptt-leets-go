package com.mashxall.unified

import com.mashxall.unified.data.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun recognizesSoraniDownloadedAdhanAsASelectablePrayerAlertAsset() {
        val downloadedCatalogAdhan = AudioItem(
            "local:/data/user/0/com.mashxall/files/adhan-audio/adhan-068.mp3",
            "أحمد النفيس",
            "بانگی داگیراو بۆ بەبێ ئینتەرنێت"
        )
        assertEquals("muezzin", bundledAdhanCategory(downloadedCatalogAdhan))
        assertTrue(filterSelectableAdhanAssets(listOf(downloadedCatalogAdhan)).contains(downloadedCatalogAdhan))
        assertTrue(hasSelectableAdhanAsset(downloadedCatalogAdhan.assetPath, listOf(downloadedCatalogAdhan)))
        val downloadedSoraniAdhan = AudioItem(
            "local:/data/user/0/com.mashxall/files/adhan-audio/kurdish.mp3",
            "دەنگی بانگ",
            "موئەزینەکان"
        )
        assertEquals("muezzin", bundledAdhanCategory(downloadedSoraniAdhan))
        assertTrue(filterSelectableAdhanAssets(listOf(downloadedSoraniAdhan)).contains(downloadedSoraniAdhan))
        assertTrue(hasSelectableAdhanAsset(downloadedSoraniAdhan.assetPath, listOf(downloadedSoraniAdhan)))
    }

    @Test
    fun recognizesNewCuratedMuezzinDownloadsWithTheSoraniLibraryLabel() {
        val downloadedMuezzin = AudioItem(
            "local:/data/user/0/com.mashxall/files/adhan-audio/muezzin-kurdish.mp3",
            "أحمد الكوردي",
            "دەنگی موئەزین داگیراو بۆ بەبێ ئینتەرنێت"
        )
        assertEquals("muezzin", bundledAdhanCategory(downloadedMuezzin))
        assertTrue(filterSelectableAdhanAssets(listOf(downloadedMuezzin)).contains(downloadedMuezzin))
        assertTrue(hasSelectableAdhanAsset(downloadedMuezzin.assetPath, listOf(downloadedMuezzin)))
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
