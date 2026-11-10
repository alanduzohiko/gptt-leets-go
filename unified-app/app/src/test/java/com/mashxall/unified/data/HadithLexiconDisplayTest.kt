package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithLexiconDisplayTest {
    @Test
    fun stripsTaggedAndStandaloneLatinHadithSourceMarkers() {
        val source = "عَنِ الْمُغِيرَةِ بْنِ شُعْبَةَ <rr>h</rr><rr>s</rr><rr>a</rr><rr>m</rr>: " +
            "أَنَّ النَّبِيَّ <rr>ﷺ</rr> قَالَ a النَّبِيَّ f عَبْدُ اللهِ i وَبِهِ z تَعَالَى v"
        val displayed = stripHadithLatinSourceMarkers(source)

        assertTrue(displayed.contains("ﷺ"))
        assertTrue(displayed.contains("النَّبِيَّ"))
        assertFalse(displayed.contains("<rr>h</rr>"))
        assertFalse(displayed.contains("<rr>s</rr>"))
        assertFalse(displayed.contains("<rr>a</rr>"))
        assertFalse(displayed.contains("<rr>m</rr>"))
        assertFalse(
            Regex("""(?<=[\u0600-\u06FF])\s+[A-Za-z](?=\s*[\u0600-\u06FF:：،,.;!?؟»«–—-]|$)""")
                .containsMatchIn(displayed)
        )
    }

    @Test
    fun repairsSplitNbspAndLiteralSourceLineBreaksForDisplay() {
        val source = "قَالَ &n<rr>b</rr><rr>s</rr>p; بَعْدَهُ\\nثُمَّ"
        val displayed = stripHadithLatinSourceMarkers(source)

        assertTrue(displayed.contains("&nbsp;"))
        assertTrue(displayed.contains("\n"))
        assertFalse(displayed.contains("<rr>"))
    }

    @Test
    fun detectsCipherPayloadWithoutClassifyingReadableWords() {
        val encryptedSourceValue = "A".repeat(128)
        val readableArabic = "هذا شرح عربي واضح للمفردة ومعناها في سياق الآية."
        val readableEnglish = "This is a readable dictionary explanation with normal words and spaces. ".repeat(3)

        assertTrue(isLikelyEncryptedLexiconPayload(encryptedSourceValue))
        assertFalse(isLikelyEncryptedLexiconPayload(readableArabic))
        assertFalse(isLikelyEncryptedLexiconPayload(readableEnglish))
    }
}
