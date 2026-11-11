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
    fun latinAndSoraniSearchTermsRemainSearchable() {
        assertTrue(hadithSearchGlobPattern("salah").contains("*s*a*l*a*h*"))
        assertTrue(hadithSearchGlobPattern("فەرموودە").isNotBlank())
    }
}
