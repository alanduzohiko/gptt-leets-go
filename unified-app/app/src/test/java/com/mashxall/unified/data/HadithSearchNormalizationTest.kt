package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithSearchNormalizationTest {
    @Test
    fun normalizedGlobIgnoresArabicVowelsAndPunctuationBetweenLetters() {
        val pattern = hadithSearchGlobPattern("إِنَّمَا، الْأَعْمَالُ")
        assertTrue(pattern.startsWith("*"))
        assertTrue(pattern.endsWith("*"))
        assertTrue(pattern.contains("[اأإآٱ]"))
        assertEquals("", hadithSearchGlobPattern(" ،؟— "))
    }

    @Test
    fun commonArabicAndSoraniKeyboardVariantsAreIncluded() {
        val pattern = hadithSearchGlobPattern("ي ه و ك")
        assertTrue(pattern.contains("[يىئ]"))
        assertTrue(pattern.contains("[هة]"))
        assertTrue(pattern.contains("[وؤ]"))
        assertTrue(pattern.contains("[كک]"))
    }

    @Test
    fun separateWordPatternsAllowEitherWordOrder() {
        val patterns = hadithSearchGlobPatterns("إِنَّمَا، الْأَعْمَالُ")
        assertEquals(2, patterns.size)
        assertTrue(patterns.all { it.startsWith("*") && it.endsWith("*") })
        assertTrue(patterns.all { it.contains("[اأإآٱ]") })
        assertTrue(hadithSearchGlobPatterns(" ،؟— ").isEmpty())
    }

    @Test
    fun ArabicAndPersianDigitsAreNormalizedForHadithNumberSearch() {
        assertEquals("حديث 123", normalizeArabicSearchText("حديث ١٢٣"))
        assertEquals("حديث 456", normalizeArabicSearchText("حديث ۴۵۶"))
    }

    @Test
    fun latinAndSoraniSearchTermsRemainSearchable() {
        assertTrue(hadithSearchGlobPattern("salah").contains("*s*a*l*a*h*"))
        assertTrue(hadithSearchGlobPattern("فەرموودە").isNotBlank())
    }
}
