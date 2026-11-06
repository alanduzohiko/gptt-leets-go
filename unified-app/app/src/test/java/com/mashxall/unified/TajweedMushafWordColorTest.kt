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
    fun onlyTheMatchingSurahAyahAndWordGroupGetsAnSvgFill() {
        val svg = """
            <svg>
              <g data-type="text" data-surah="001" data-aya="001" data-word-index-in-ayah="1"><path id="plain"/></g>
              <g data-type="text" data-surah="001" data-aya="001" data-word-index-in-ayah="2"><path id="tajweed"/></g>
              <g data-type="text" data-surah="001" data-aya="002" data-word-index-in-ayah="2"><path id="other-ayah"/></g>
              <g data-type="text" data-surah="002" data-aya="001" data-word-index-in-ayah="2"><path id="other-surah"/></g>
            </svg>
        """.trimIndent()
        val taggedAyahs = mapOf(
            1 to mapOf(1 to "وَ [q:1[قَالَ] الرَّحْمَٰنُ")
        )

        val coloured = TajweedText.colorMushafSvgWords(svg, taggedAyahs)

        assertTrue(coloured.contains("""data-word-index-in-ayah="2" fill="#DD0008""""))
        assertTrue(coloured.contains("""<path id="tajweed"/>"""))
        assertTrue(coloured.contains("""<path id="plain"/>"""))
        assertTrue(coloured.contains("""<path id="other-ayah"/>"""))
        assertTrue(coloured.contains("""<path id="other-surah"/>"""))
        assertFalse(coloured.contains("""data-word-index-in-ayah="1" fill="""))
    }

    @Test
    fun disablingTajweedOrMissingPageDataLeavesTheOriginalSvgUntouched() {
        val svg = """<svg><g data-surah="001" data-aya="001" data-word-index-in-ayah="1"/></svg>"""
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, emptyMap()))
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, mapOf(2 to mapOf(1 to "[q:1[ق]")))
    }

    @Test
    fun invalidWordIndexesDoNotModifyTheSvg() {
        val svg = """<svg><g data-surah="001" data-aya="001" data-word-index-in-ayah="0"/></svg>"""
        val tagged = mapOf(1 to mapOf(1 to "[q:1[ق]"))
        assertEquals(svg, TajweedText.colorMushafSvgWords(svg, tagged))
    }
}
