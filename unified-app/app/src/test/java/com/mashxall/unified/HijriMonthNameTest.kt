package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Locale

class HijriMonthNameTest {
    @Test
    fun jumadaAlThaniUsesTheHijriNameForTheSixthCalendarMonth() {
        // The shared Hijri month-name helper uses the conventional 1..12 month numbers.
        assertEquals("جمادى الآخرة", hijriMonthName(6, AppLanguage.AR))
        assertEquals("جومادای دووەم", hijriMonthName(6, AppLanguage.CKB))
        assertEquals("Jumada al-Thani", hijriMonthName(6, AppLanguage.EN))
    }

    @Test
    fun allTwelveHijriMonthNumbersAreMappedAndInvalidNumbersAreRejected() {
        for (month in 1..12) {
            assertNotNull(hijriMonthName(month, AppLanguage.AR))
        }
        assertEquals("", hijriMonthName(0, AppLanguage.AR))
        assertEquals("", hijriMonthName(13, AppLanguage.AR))
    }

    @Test
    fun hijriYearNumbersNeverUseThousandsSeparators() {
        for (languageTag in listOf("ar", "ckb", "en")) {
            val rendered = formatHijriNumber(1448, Locale.forLanguageTag(languageTag))
            assertFalse("Unexpected grouping separator in $rendered", rendered.any {
                it == ',' || it == '،' || it == '٬'
            })
            assertEquals(4, rendered.count { it.isDigit() })
        }
    }
}
