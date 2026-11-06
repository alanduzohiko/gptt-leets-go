package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TajweedMushafWordColorTest {
    @Test
    fun taggedRulesMapToTheCorrectWhitespaceSeparatedWordIndexes() {
        val colors = TajweedText.wordRuleColors("وَ [q:1[قَالَ] الرَّحْمَٰنُ")

        assertEquals(3, colors.size)
        assertNull(colors[0])
        assertEquals(TajweedText.ruleColor('q'), colors[1])
        assertNull(colors[2])
    }

    @Test
    fun allSurahsUseTheSameCommonPalette() {
        val svg = """
            <svg>
              <g data-surah="001" data-aya="001" data-word-index-in-ayah="1"><path id="surah-one"/></g>
              <g data-surah="114" data-aya="006" data-word-index-in-ayah="1"><path id="surah-114"/></g>
            </svg>
        """.trimIndent()
        val taggedAyahs = mapOf(
            1 to mapOf(1 to "[q:1[قَالَ]"),
            114 to mapOf(6 to "[a:1[مِنَ]")
        )

        val coloured = TajweedText.colorMushafSvgWords(svg, taggedAyahs)

        assertTrue(coloured.contains("""<path id="surah-one" fill="#D23935"/>"""))
        assertTrue(coloured.contains("""<path id="surah-114" fill="#2B945C"/>"""))
    }

    @Test
    fun maddWordsAreSelectedForTheReferenceYellowHighlight() {
        assertEquals(
            listOf(true, false, false, true),
            TajweedText.wordMaddHighlightFlags("[n[قَالَ] وَ [q[ق] [o[مَدّ]")
        )
    }

    @Test
    fun nightModePrecompensatesForThePageInversionFilter() {
        val svg = """<svg><g data-surah="001" data-aya="001" data-word-index-in-ayah="1"><path id="night"/></g></svg>"""
        val tagged = mapOf(1 to mapOf(1 to "[q:1[ق]"))

        val coloured = TajweedText.colorMushafSvgWords(svg, tagged, darkMode = true)

        assertTrue(coloured.contains("""fill="#00868B""""))
        assertTrue(coloured.contains("""<path id="night" fill="#00868B"/>"""))
    }

    @Test
    fun disablingTajweedOrMissingPageDataLeavesTheOriginalSvgUntouched() {
        val svg = """<svg><g data-surah="001" data-aya="001" data-word-index-in-ayah="1"/></svg>"""
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, emptyMap()))
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, mapOf(2 to mapOf(1 to "[q:1[ق]"))))
    }

    @Test
    fun invalidWordIndexesDoNotModifyTheSvg() {
        val svg = """<svg><g data-surah="001" data-aya="001" data-word-index-in-ayah="0"/></svg>"""
        val tagged = mapOf(1 to mapOf(1 to "[q:1[ق]"))
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, tagged))
    }
}
