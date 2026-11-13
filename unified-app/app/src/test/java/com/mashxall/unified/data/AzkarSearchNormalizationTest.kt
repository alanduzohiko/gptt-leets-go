package com.mashxall.unified.data

import com.mashxall.unified.data.azkarSearchMatches
import com.mashxall.unified.data.normalizeAzkarSearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarSearchNormalizationTest {
    private fun sample() = AzkarItem(
        id = 7,
        titleCkb = "زیکری بەیانی",
        titleAr = "أذكار الصباح",
        titleEn = "Morning remembrance",
        arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ",
        ckb = "ئێمە بەیانی بووین و هەموو دەسەڵات بۆ خوایە",
        english = "We have entered the morning and all dominion belongs to Allah",
        count = 1,
        referenceCkb = "سەرچاوە: مسلم",
        categoryId = 2,
        categoryCkb = "بەیانی",
        categoryArabic = "الصباح",
        categoryEnglish = "Morning"
    )

    @Test
    fun arabicSearchIgnoresHarakatAndNormalizesAlefForms() {
        assertEquals("اصبحنا", normalizeAzkarSearchText("أَصْبَحْنَا"))
        assertTrue(azkarSearchMatches("اصبحنا", sample()))
        assertTrue(azkarSearchMatches("أصبحنا", sample()))
    }

    @Test
    fun soraniAndEnglishTranslationsAreSearchableAfterMerge() {
        assertTrue(azkarSearchMatches("دەسەڵات", sample()))
        assertTrue(azkarSearchMatches("dominion belongs", sample()))
        assertTrue(azkarSearchMatches("زیکری بەیانی", sample()))
    }

    @Test
    fun titlesCategoriesAndReferencesAreIncluded() {
        assertTrue(azkarSearchMatches("الصباح", sample()))
        assertTrue(azkarSearchMatches("مسلم", sample()))
        assertFalse(azkarSearchMatches("evening", sample()))
    }

    @Test
    fun multiWordSearchRequiresAllTerms() {
        assertTrue(azkarSearchMatches("morning Allah", sample()))
        assertFalse(azkarSearchMatches("morning unrelated", sample()))
    }

    @Test
    fun emptyQueryMatchesEveryItem() {
        assertTrue(azkarSearchMatches("   ", sample()))
    }
}
