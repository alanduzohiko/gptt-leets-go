package com.mashxall.unified

import com.mashxall.unified.data.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BundledAdhanAssetTest {
    @Test
    fun recognizesBundledAdhanAndMuezzinFilesByTheirActualPathOrLabel() {
        assertEquals(
            "muezzin",
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/noor/base/assets/audio/adhan/adhan_fajr.mp3",
                    title = "Fajr Adhan",
                    group = "adhan"
                )
            )
        )
        assertEquals(
            "muezzin",
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/source/audio/muezzin/004.mp3",
                    title = "Muezzin 004",
                    group = "muezzin"
                )
            )
        )
        assertEquals(
            "muezzin",
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/source/audio/azan_fajr.mp3",
                    title = "Azan Fajr",
                    group = "audio"
                )
            )
        )
    }

    @Test
    fun recognizesBundledMosqueAndHolySiteRecordings() {
        assertEquals(
            "mosque",
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/source/audio/Masjid_Al_Haram.mp3",
                    title = "Masjid Al Haram",
                    group = "mosque"
                )
            )
        )
        assertEquals(
            "mosque",
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/source/audio/Madinah_Nabawi.mp3",
                    title = "Madinah Nabawi",
                    group = "audio"
                )
            )
        )
    }

    @Test
    fun selectablePrayerAlertAssetsFilterOutGenericSourceAudioAndDeduplicatePaths() {
        val adhan = AudioItem(
            assetPath = "content/source/audio/adhan/adhan_fajr.mp3",
            title = "Fajr adhan",
            group = "adhan"
        )
        val quran = AudioItem(
            assetPath = "content/quran/audio/abdul_basit/001.mp3",
            title = "Abdul Basit 001",
            group = "quran"
        )
        val adhkar = AudioItem(
            assetPath = "content/azkar/audio/morning.mp3",
            title = "Morning adhkar",
            group = "azkar"
        )
        assertEquals(listOf(adhan), filterSelectableAdhanAssets(listOf(quran, adhan, adhkar, adhan)))
    }

    @Test
    fun scheduledAdhanSelectionUsesSourceMetadataInsteadOfFilenameGuessing() {
        val sourceAdhan = AudioItem(
            assetPath = "content/source/audio/005.mp3",
            title = "Fajr call",
            group = "adhan"
        )
        val quran = AudioItem(
            assetPath = "content/quran/audio/abdul_basit/001.mp3",
            title = "Abdul Basit 001",
            group = "quran"
        )
        val adhkar = AudioItem(
            assetPath = "content/azkar/audio/morning.mp3",
            title = "Morning adhkar",
            group = "azkar"
        )
        val available = filterSelectableAdhanAssets(listOf(sourceAdhan, quran, adhkar))
        assertEquals(true, hasSelectableAdhanAsset(sourceAdhan.assetPath, available))
        assertEquals(false, hasSelectableAdhanAsset(quran.assetPath, available))
        assertEquals(false, hasSelectableAdhanAsset(adhkar.assetPath, available))
        assertEquals(false, hasSelectableAdhanAsset("", available))
    }

    @Test
    fun noSelectionExplainsTheRequiredActionInAllSupportedLanguages() {
        assertEquals(true, adhanSourceRequiredMessage(AppLanguage.EN).contains("download a track"))
        assertEquals(true, adhanSourceRequiredMessage(AppLanguage.AR).contains("نزّل تسجيلًا"))
        assertEquals(true, adhanSourceRequiredMessage(AppLanguage.CKB).contains("دەنگێک دابگرە"))
    }

    @Test
    fun doesNotMixGenericQuranRecitationOrAdhkarIntoAdhanLibrary() {
        assertNull(
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/quran/audio/abdul_basit/001.mp3",
                    title = "Abdul Basit 001",
                    group = "quran"
                )
            )
        )
        assertNull(
            bundledAdhanCategory(
                AudioItem(
                    assetPath = "content/azkar/audio/morning.mp3",
                    title = "Morning adhkar",
                    group = "azkar"
                )
            )
        )
    }
}
