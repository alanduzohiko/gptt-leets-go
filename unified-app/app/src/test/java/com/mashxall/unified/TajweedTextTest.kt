package com.mashxall.unified

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TajweedTextTest {
    @Test
    fun stripsMarkupAndColoursOnlyTheTaggedLetterSpans() {
        val text = TajweedText.annotate("بِسْمِ [q[ق] بِلَا [g[ن]")
        assertEquals("بِسْمِ ق بِلَا ن", text.text)
        assertEquals(2, text.spanStyles.size)
        assertEquals(Color(0xFFDD0008), text.spanStyles[0].item.color)
        assertEquals(Color(0xFFFF7E1E), text.spanStyles[1].item.color)
        assertEquals(7, text.spanStyles[0].start)
        assertEquals(8, text.spanStyles[0].end)
    }

    @Test
    fun acceptsRuleIdentifiersWithNumericSourceMetadata() {
        val text = TajweedText.annotate("ٱللَّهِ [h:9421[ٱ] نُورٌ")
        assertEquals("ٱللَّهِ ٱ نُورٌ", text.text)
        assertEquals(1, text.spanStyles.size)
        assertEquals(Color(0xFFAAAAAA), text.spanStyles.single().item.color)
    }

    @Test
    fun unknownOrIncompleteMarkersDoNotDropTheSourceText() {
        val unknown = TajweedText.annotate("plain [x[unmapped] tail")
        val incomplete = TajweedText.annotate("بِسْمِ [q[ق")
        assertEquals("plain [x[unmapped] tail", unknown.text)
        assertEquals("بِسْمِ [q[ق", incomplete.text)
    }

    @Test
    fun darkReaderUsesAHighContrastPalette() {
        assertNotEquals(
            TajweedText.ruleColor('q', darkMode = false),
            TajweedText.ruleColor('q', darkMode = true)
        )
        assertTrue(TajweedText.annotate("[g[ن]", darkMode = true).spanStyles.single().item.color != Color.Unspecified)
    }
}
