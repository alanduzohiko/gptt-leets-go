package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBackExitMessageTest {
    @Test
    fun homeExitHintIsLocalized() {
        assertEquals("Press Back again to exit", homeBackExitMessage(AppLanguage.EN))
        assertEquals("اضغط رجوع مرة أخرى للخروج", homeBackExitMessage(AppLanguage.AR))
        assertEquals("بۆ دەرچوون جارێکی تر دوگمەی گەڕانەوە دابگرە", homeBackExitMessage(AppLanguage.CKB))
    }
}
