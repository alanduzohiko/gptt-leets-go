package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranBookmarkStateTest {
    @Test
    fun addsNewBookmarkAndPreservesOtherVerses() {
        val original = setOf("1:1", "2:17")
        val result = toggleQuranBookmark(original, 18, 10)

        assertTrue(result.isBookmarked)
        assertEquals(setOf("1:1", "2:17", "18:10"), result.bookmarks)
        assertEquals(setOf("1:1", "2:17"), original)
    }

    @Test
    fun tappingSavedVerseRemovesOnlyThatBookmark() {
        val result = toggleQuranBookmark(setOf("1:1", "2:17"), 2, 17)

        assertFalse(result.isBookmarked)
        assertEquals(setOf("1:1"), result.bookmarks)
    }

    @Test
    fun bookmarkKeyIsStableAndReadable() {
        assertEquals("114:6", quranBookmarkKey(114, 6))
    }
}
