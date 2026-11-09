package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranVerseSearchNormalizationTest {
    @Test
    fun removesVowelMarksAndQuranicAnnotationSigns() {
        assertEquals(
            "بسم الله الرحمن الرحيم",
            normalizeArabicSearchText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
        )
    }

    @Test
    fun foldsCommonAlifHamzaAndPersianKeyboardVariants() {
        assertEquals(
            "ان الله",
            normalizeArabicSearchText("إِنَّ اللّٰهَ")
        )
        assertEquals("ي", normalizeArabicSearchText("ىئ ی"))
        assertEquals("ك", normalizeArabicSearchText("ک"))
    }

    @Test
    fun findsAFullAyahWhenTheUserTypesItWithDifferentVowelMarks() {
        assertTrue(
            quranSearchMatches(
                query = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                arabicWithoutHarakat = "بسم الله الرحمن الرحيم",
                translation = "",
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatiha",
                surah = 1,
                ayah = 1
            )
        )
    }

    @Test
    fun findsAyahWhenSpacingOrPunctuationDiffers() {
        assertTrue(
            quranSearchMatches(
                query = "مالك يوم الدين",
                arabic = "مَالِكِ يَوْمِ الدِّينِ",
                arabicWithoutHarakat = "مالك يوم الدين",
                translation = "",
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatiha",
                surah = 1,
                ayah = 4
            )
        )
    }

    @Test
    fun matchesEveryMeaningfulWordRatherThanOnlyAnExactContinuousPhrase() {
        assertTrue(
            quranSearchMatches(
                query = "اهدنا الصراط المستقيم",
                arabic = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                arabicWithoutHarakat = "اهدنا الصراط المستقيم",
                translation = "",
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatiha",
                surah = 1,
                ayah = 6
            )
        )
    }

    @Test
    fun searchesTheSelectedTranslationAndSurahNamesToo() {
        assertTrue(
            quranSearchMatches(
                query = "straight path",
                arabic = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                arabicWithoutHarakat = "اهدنا الصراط المستقيم",
                translation = "Guide us to the straight path.",
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatiha",
                surah = 1,
                ayah = 6
            )
        )
        assertTrue(
            quranSearchMatches(
                query = "الفاتحة",
                arabic = "",
                arabicWithoutHarakat = "",
                translation = "",
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatiha",
                surah = 1,
                ayah = 1
            )
        )
    }

    @Test
    fun retainsDirectSurahAndAyahReferenceSearch() {
        assertTrue(
            quranSearchMatches(
                query = "2:255",
                arabic = "",
                arabicWithoutHarakat = "",
                translation = "",
                surahNameArabic = "",
                surahNameEnglish = "",
                surah = 2,
                ayah = 255
            )
        )
        assertFalse(
            quranSearchMatches(
                query = "2:254",
                arabic = "",
                arabicWithoutHarakat = "",
                translation = "",
                surahNameArabic = "",
                surahNameEnglish = "",
                surah = 2,
                ayah = 255
            )
        )
    }
}
