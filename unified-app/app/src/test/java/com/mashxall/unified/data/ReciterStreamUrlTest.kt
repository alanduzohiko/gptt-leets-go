package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReciterStreamUrlTest {
    @Test
    fun upgradesLegacyHttpReciterHostsToHttps() {
        assertEquals(
            "https://server8.mp3quran.net/reader/",
            ContentRepository.secureReciterStreamBaseUrl("http://server8.mp3quran.net/reader/")
        )
    }

    @Test
    fun preservesAlreadySecureAndOtherSourceValues() {
        assertEquals(
            "https://server12.mp3quran.net/maher/",
            ContentRepository.secureReciterStreamBaseUrl("https://server12.mp3quran.net/maher/")
        )
        assertEquals(
            "content/audio/recitation.mp3",
            ContentRepository.secureReciterStreamBaseUrl("content/audio/recitation.mp3")
        )
    }

    @Test
    fun buildsEveryAyahPerVerseUrlForNewReciters() {
        val everyAyah = Reciter(
            2_000_003,
            "ياسر الدوسري",
            "Yasser Ad-Dussary",
            0,
            "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/",
            2_003,
            sourceId = "everyayah:Yasser_Ad-Dussary_128kbps"
        )
        assertEquals(
            "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/002255.mp3",
            ContentRepository.reciterSampleUrl(everyAyah, 2, 255)
        )
    }

    @Test
    fun buildsTheExpectedAyahAndFullSurahFileNames() {
        val verse = Reciter(1, "قارئ", "Reader", 0, "http://server8.mp3quran.net/reader/", 1)
        val full = Reciter(2, "قارئ", "Reader", 1, "http://server8.mp3quran.net/reader/", 1)
        assertEquals("https://server8.mp3quran.net/reader/002255.mp3", ContentRepository.reciterSampleUrl(verse, 2, 255))
        assertEquals("https://server8.mp3quran.net/reader/002.mp3", ContentRepository.reciterSampleUrl(full, 2, 1))
    }
}
