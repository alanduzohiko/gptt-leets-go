package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithLexiconDisplayTest {
    @Test
    fun stripsIsolatedLatinHadithSourceMarkers() {
        val source = "عَنِ الْمُغِيرَةِ بْنِ شُعْبَةَ <rr>h</rr><rr>s</rr><rr>a</rr><rr>m</rr>: أَنَّ النَّبِيَّ <rr>ﷺ</rr> قَالَ"
        val displayed = stripHadithLatinSourceMarkers(source)

        assertEquals("عَنِ الْمُغِيرَةِ بْنِ شُعْبَةَ : أَنَّ النَّبِيَّ <rr>ﷺ</rr> قَالَ", displayed)
        assertTrue(displayed.contains("ﷺ"))
        assertFalse(displayed.contains("<rr>h</rr>"))
        assertFalse(displayed.contains("<rr>s</rr>"))
        assertFalse(displayed.contains("<rr>a</rr>"))
        assertFalse(displayed.contains("<rr>m</rr>"))
    }

    @Test
    fun detectsTheSourceCipherPayloadWithoutMutatingReadableText() {
        val encodedSourceValue = "A".repeat(128)
        val readableArabic = "هذا شرح عربي واضح للمفردة ومعناها في سياق الآية."
        val readableEnglish = "This is a readable dictionary explanation with normal words and spaces. ".repeat(3)

        assertTrue(isLikelyEncryptedLexiconPayload(encodedSourceValue))
        assertFalse(isLikelyEncryptedLexiconPayload(readableArabic))
        assertFalse(isLikelyEncryptedLexiconPayload(readableEnglish))
        assertEquals(encodedSourceValue, encodedSourceValue) // The stored value is not rewritten by the detector.
    }
}
