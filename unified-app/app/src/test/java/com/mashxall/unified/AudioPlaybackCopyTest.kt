package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackCopyTest {
    @Test
    fun adhanActionsAreLocalizedInAllSupportedLanguages() {
        assertEquals("Play adhan", adhanAudioActionDescription(AppLanguage.EN, false))
        assertEquals("Pause adhan", adhanAudioActionDescription(AppLanguage.EN, true))
        assertEquals("تشغيل الأذان", adhanAudioActionDescription(AppLanguage.AR, false))
        assertEquals("إيقاف الأذان مؤقتًا", adhanAudioActionDescription(AppLanguage.AR, true))
        assertEquals("لێدانی بانگ", adhanAudioActionDescription(AppLanguage.CKB, false))
        assertEquals("وەستاندنی کاتی بانگ", adhanAudioActionDescription(AppLanguage.CKB, true))
    }

    @Test
    fun catalogNoticeAndPlaybackErrorsRespectSelectedLanguage() {
        assertTrue(audioCatalogSourceNotice(AppLanguage.AR).contains("حتى تختار تنزيلها"))
        assertTrue(audioCatalogSourceNotice(AppLanguage.CKB).contains("کاتەلۆگی سەرچاوە"))
        assertTrue(audioPlaybackFailureMessage(AppLanguage.CKB, AudioPlaybackFailure.STREAM).contains("قورئان‌خوێنێکی تر"))
        assertTrue(audioPlaybackFailureMessage(AppLanguage.AR, AudioPlaybackFailure.LOCAL_ASSET).contains("تعذّر فتح الملف"))
        assertTrue(audioPlaybackFailureMessage(AppLanguage.EN, AudioPlaybackFailure.RECITATION_SEGMENT).contains("download the surah"))
    }
}
