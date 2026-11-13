package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithSearchNormalizationTest {
    @Test
    fun globMatchesAdjacentLettersAndAllowsArabicMarksBetweenThem() {
        val pattern = hadithSearchGlobPattern("إِنَّمَا")
        assertTrue(pattern.startsWith("*"))
        assertTrue(pattern.endsWith("*"))
        assertTrue(pattern.contains("[اأإآٱ]"))
        assertTrue(pattern.contains("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u08D3-\u08FF\u0640]*"))
        assertFalse(pattern.contains("**"))
        assertEquals("", hadithSearchGlobPattern(" ،؟— "))
    }

    @Test
    fun commonArabicAndSoraniKeyboardVariantsAreIncluded() {
        val pattern = hadithSearchGlobPattern("يهوك")
        assertTrue(pattern.contains("[يىئ]"))
        assertTrue(pattern.contains("[هة]"))
        assertTrue(pattern.contains("[وؤ]"))
        assertTrue(pattern.contains("[كک]"))
    }

    @Test
    fun separateWordPatternsAllowEitherWordOrderWithoutFuzzyLetterMatching() {
        val patterns = hadithSearchGlobPatterns("إِنَّمَا، الْأَعْمَالُ")
        assertEquals(2, patterns.size)
        assertTrue(patterns.all { it.startsWith("*") && it.endsWith("*") })
        assertTrue(patterns.all { it.contains("[اأإآٱ]") })
        assertFalse(patterns.any { it.contains("*ا*") || it.contains("*ل*") })
        assertTrue(hadithSearchGlobPatterns(" ،؟— ").isEmpty())
    }

    @Test
    fun ArabicAndPersianDigitsAreNormalizedForHadithNumberSearch() {
        assertEquals("حديث 123", normalizeArabicSearchText("حديث ١٢٣"))
        assertEquals("حديث 456", normalizeArabicSearchText("حديث ۴۵۶"))
    }

    @Test
    fun latinAndSoraniSearchTermsRemainSearchableWithoutScatteredLetters() {
        val pattern = hadithSearchGlobPattern("salah")
        assertTrue(pattern.startsWith("*s["))
        assertTrue(pattern.contains("]*a["))
        assertTrue(pattern.contains("]*l["))
        assertTrue(pattern.contains("]*a["))
        assertTrue(pattern.endsWith("]*h*"))
        assertFalse(pattern.contains("*s*a*l*a*h*"))
        assertTrue(hadithSearchGlobPattern("فەرموودە").isNotBlank())
    }
}
