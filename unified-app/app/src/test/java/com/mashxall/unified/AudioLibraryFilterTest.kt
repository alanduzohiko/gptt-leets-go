package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLibraryFilterTest {
    @Test
    fun recognizesEnglishAndTransliteratedAdhanNames() {
        assertTrue(isAdhanAudioAsset("Makkah Adhan", "assets/audio/makkah_adhan.mp3", "Mosque audio"))
        assertTrue(isAdhanAudioAsset("Azan Fajr", "content/salawati/azan_fajr.ogg", "Audio"))
        assertTrue(isAdhanAudioAsset("Imported adhan", "custom:local-file.mp3", "Imported adhan from phone"))
    }

    @Test
    fun recognizesArabicAdhanAndMuezzinNames() {
        assertTrue(isAdhanAudioAsset("أذان المسجد النبوي", "content/audio/الاذان.mp3", "صوت مسجد"))
        assertTrue(isAdhanAudioAsset("المؤذن", "content/مؤذن/صوت.mp3", "الأذان"))
        assertTrue(isAdhanAudioAsset("تكبيرات العيد", "audio/takbeer.ogg", "مسجد"))
    }

    @Test
    fun recognizesSoraniAndBadiniNames() {
        assertTrue(isAdhanAudioAsset("بانگی بەیانی", "content/بانگ.mp3", "دەنگی مزگەوت"))
        assertTrue(isAdhanAudioAsset("موەزین", "content/موەزین.wav", "دەنگ"))
        assertTrue(isAdhanAudioAsset("مزگەوت", "audio/نوێژ.ogg", "بانگ"))
    }

    @Test
    fun unrelatedQuranAudioDoesNotLeakIntoAdhanOnlyTab() {
        assertFalse(isAdhanAudioAsset("Surah Al-Baqarah", "content/quran/002.mp3", "Quran recitation"))
        assertFalse(isAdhanAudioAsset("Click sound", "content/ui/click.wav", "UI effects"))
    }
}
