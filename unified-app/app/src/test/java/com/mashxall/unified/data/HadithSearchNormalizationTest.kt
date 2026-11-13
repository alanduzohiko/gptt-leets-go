package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HadithSearchNormalizationTest {
    @Test
    fun globIsBroadCandidatePrefilterForPlainAndDiacriticText() {
        val pattern = hadithSearchGlobPattern("إِنَّمَا")
        assertTrue(pattern.startsWith("*"))
        assertTrue(pattern.endsWith("*"))
        assertTrue(pattern.contains("[اأإآٱ]*ن*م*"))
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
        val expectedFirstWildcards = normalizeArabicSearchText("إِنَّمَا").replace(" ", "").length + 1
        val expectedSecondWildcards = normalizeArabicSearchText("الْأَعْمَالُ").replace(" ", "").length + 1
        assertEquals(expectedFirstWildcards, patterns[0].count { it == '*' })
        assertEquals(expectedSecondWildcards, patterns[1].count { it == '*' })
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
        assertEquals("*s*a*l*a*h*", pattern)
        assertTrue(hadithSearchGlobPattern("فەرموودە").isNotBlank())
    }
    @Test
    fun preNormalizedFieldsKeepPartialArabicSearchAccurate() {
        val tokens = normalizeArabicSearchText("إنم الأعم")
            .split(' ')
            .filter(String::isNotBlank)
        val fields = listOf(
            normalizeHadithSearchField("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ"),
            "",
            ""
        )
        assertEquals(2, hadithSearchMatchScoreInNormalizedFields(tokens, fields))
    }

    @Test
    fun preNormalizedFieldsStillRejectDistantUnrelatedWords() {
        val tokens = normalizeArabicSearchText("الصدقة الصلاة")
            .split(' ')
            .filter(String::isNotBlank)
        val fields = listOf(
            normalizeHadithSearchField("الصدقة، ثم طال المجلس وتكلم الناس، وفي آخر الليل أقيمت الصلاة")
        )
        assertEquals(null, hadithSearchMatchScoreInNormalizedFields(tokens, fields))
    }

    @Test
    fun exactHadithPhraseWinsAfterArabicDiacriticNormalization() {
        assertEquals(
            0,
            hadithSearchMatchScore(
                "إِنَّمَا الْأَعْمَالُ",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun closeWordsMatchEvenWhenTheUserTypesThemInReverseOrder() {
        assertEquals(
            2,
            hadithSearchMatchScore(
                "الأعمال إنما",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun distantWordsDoNotCreateAnUnrelatedHadithMatch() {
        assertEquals(
            null,
            hadithSearchMatchScore(
                "الصدقة الصلاة",
                listOf("تحدث الناس عن الصدقة في المجلس ثم جاء الحديث عن فضل الصلاة في آخر الليل")
            )
        )
    }

    @Test
    fun wordsFromDifferentLanguageFieldsCannotBeCombinedIntoOneMatch() {
        assertEquals(
            null,
            hadithSearchMatchScore(
                "الصلاة الصوم",
                listOf("أقيموا الصلاة", "ڕۆژوو فەرزە")
            )
        )
    }

    @Test
    fun singleWordSearchStillFindsTheNarrationContainingThatWord() {
        assertEquals(
            0,
            hadithSearchMatchScore(
                "الصلاة",
                listOf("كان رسول الله يصلي ثم أقيموا الصلاة")
            )
        )
    }

    @Test
    fun incompleteSingleWordFindsTheFullHadithWord() {
        assertEquals(
            1,
            hadithSearchMatchScore(
                "إنم",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun incompleteLastWordFindsTheHadithFromAPartialPhrase() {
        assertEquals(
            2,
            hadithSearchMatchScore(
                "إنما الأعم",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun incompleteFragmentsInSeveralWordsStillFindTheSameHadith() {
        assertEquals(
            2,
            hadithSearchMatchScore(
                "إنم الأعم",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun partialEndingOfHadithWordMatchesWithoutCompletingIt() {
        assertEquals(
            1,
            hadithSearchMatchScore(
                "بالني",
                listOf("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
            )
        )
    }

    @Test
    fun broadSqlCandidateGapCannotOverrideStrictFinalRelevanceCheck() {
        assertEquals(
            null,
            hadithSearchMatchScore(
                "الصدقة الصلاة",
                listOf("الصدقة، ثم طال المجلس وتكلم الناس، وفي آخر الليل أقيمت الصلاة")
            )
        )
    }
}
