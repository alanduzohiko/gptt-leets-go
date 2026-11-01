package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeQuranResumeDetailsTest {
    @Test
    fun newReaderIsInvitedToChooseASurah() {
        assertEquals(
            tr(AppLanguage.CKB, "mushaf_offline"),
            homeQuranResumeDetails(
                AppLanguage.CKB,
                hasReadingHistory = false,
                surah = 1,
                ayah = 1,
                readerMode = "verse",
                mushafPage = 1
            )
        )
    }

    @Test
    fun verseReaderDoesNotShowAStaleMushafPage() {
        val detail = homeQuranResumeDetails(
            AppLanguage.EN,
            hasReadingHistory = true,
            surah = 2,
            ayah = 17,
            readerMode = "verse",
            mushafPage = 256
        )
        assertTrue(detail.contains("Surah 2"))
        assertTrue(detail.contains("Ayah 17"))
        assertTrue(detail.contains(tr(AppLanguage.EN, "verse_view")))
        assertTrue(!detail.contains("256"))
    }

    @Test
    fun graphicMushafShowsTheSavedPage() {
        val detail = homeQuranResumeDetails(
            AppLanguage.AR,
            hasReadingHistory = true,
            surah = 18,
            ayah = 5,
            readerMode = "mushaf",
            mushafPage = 293
        )
        assertTrue(detail.contains("18"))
        assertTrue(detail.contains("5"))
        assertTrue(detail.contains("293"))
        assertTrue(detail.contains(tr(AppLanguage.AR, "mushaf_view")))
    }
}
