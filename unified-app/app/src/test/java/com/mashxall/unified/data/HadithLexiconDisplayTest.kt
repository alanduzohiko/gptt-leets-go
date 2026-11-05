package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithLexiconDisplayTest {
    @Test
    fun stripsOnlyTheIsolatedHadithHSourceMarker() {
        val source = "عَنِ الْمُغِيرَةِ بْنِ شُعْبَةَ <rr>h</rr>: أَنَّ النَّبِيَّ <rr>ﷺ</rr> قَالَ"
        val displayed = stripHadithHSourceMarker(source)

        assertEquals("عَنِ الْمُغِيرَةِ بْنِ شُعْبَةَ : أَنَّ النَّبِيَّ <rr>ﷺ</rr> قَالَ", displayed)
        assertTrue(displayed.contains("ﷺ"))
        assertFalse(displayed.contains("<rr>h</rr>"))
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
