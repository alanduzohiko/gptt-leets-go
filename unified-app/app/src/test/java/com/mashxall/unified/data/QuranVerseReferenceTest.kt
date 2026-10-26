package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuranVerseReferenceTest {
    @Test
    fun parsesStandardAndArabicColonVerseReferences() {
        assertEquals(1 to 2, parseQuranVerseReference("1:2"))
        assertEquals(114 to 6, parseQuranVerseReference("114:6"))
        assertEquals(2 to 255, parseQuranVerseReference("2 : 255"))
        assertEquals(1 to 1, parseQuranVerseReference("١:١").let { it })
    }

    @Test
    fun rejectsNonReferenceQueriesAndOutOfRangeValues() {
        assertNull(parseQuranVerseReference(""))
        assertNull(parseQuranVerseReference("al-Fatiha"))
        assertNull(parseQuranVerseReference("1:0"))
        assertNull(parseQuranVerseReference("115:1"))
        assertNull(parseQuranVerseReference("1:287"))
        assertNull(parseQuranVerseReference("1:2 extra"))
    }
}
