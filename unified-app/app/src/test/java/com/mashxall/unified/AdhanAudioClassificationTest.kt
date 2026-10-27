package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanAudioClassificationTest {
    @Test
    fun recognizesAdhanAndMosqueAudioNames() {
        assertTrue(isAdhanAudioDescriptor("Adhan Madina", "content/audio/adhan_madina.mp3", "adhan"))
        assertTrue(isAdhanAudioDescriptor("Call to prayer", "content/audio/call_to_prayer.wav", "mosque"))
        assertTrue(isAdhanAudioDescriptor("Muadhin", "content/muadhin/fajr.mp3", "muadhin"))
        assertTrue(isAdhanAudioDescriptor("Iqama", "content/iqamah.mp3", "iqama"))
    }

    @Test
    fun doesNotMisclassifyQuranReciterNamesAsAdhan() {
        assertFalse(isAdhanAudioDescriptor("Mishary Rashid Alafasy", "content/reciters/mishary/001001.mp3", "Quran"))
        assertFalse(isAdhanAudioDescriptor("Abdul Basit", "content/audio/abdul_basit_full_surah.mp3", "Recitation"))
        assertFalse(isAdhanAudioDescriptor("Yasser Al-Dosari", "content/reciters/yasser/001.mp3", "Quran audio"))
    }

    @Test
    fun allowsAnExplicitAdhanFolderToIdentifyAnOtherwiseGenericFile() {
        assertTrue(isAdhanAudioDescriptor("Track 01", "content/adhan/track01.mp3", "adhan"))
    }
}
