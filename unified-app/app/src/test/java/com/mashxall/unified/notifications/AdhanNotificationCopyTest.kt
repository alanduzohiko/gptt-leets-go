package com.mashxall.unified.notifications

import com.mashxall.unified.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AdhanNotificationCopyTest {
    @Test
    fun missingSelectedAudioExplainsHowToFixItInEverySupportedLanguage() {
        assertEquals(true, missingAdhanAudioMessage(AppLanguage.EN).contains("Prayer settings"))
        assertEquals(true, missingAdhanAudioMessage(AppLanguage.AR).contains("إعدادات الصلاة"))
        assertEquals(true, missingAdhanAudioMessage(AppLanguage.CKB).contains("ڕێکخستنەکانی نوێژ"))
    }

    @Test
    fun notificationContentAndStopActionAreLocalized() {
        assertEquals("Prayer adhan", adhanNotificationCopy(AppLanguage.EN).channelName)
        assertEquals("Stop", adhanNotificationCopy(AppLanguage.EN).stopAction)
        assertEquals("أذان الصلاة", adhanNotificationCopy(AppLanguage.AR).channelName)
        assertEquals("إيقاف", adhanNotificationCopy(AppLanguage.AR).stopAction)
        assertEquals("بانگی نوێژ", adhanNotificationCopy(AppLanguage.CKB).channelName)
        assertEquals("وەستاندن", adhanNotificationCopy(AppLanguage.CKB).stopAction)
    }
}
