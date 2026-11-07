package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class QuranReaderActionMotionTest {
    @Test
    fun readerActionsHaveDistinctPressFeedback() {
        val actions = listOf(
            "previous_surah", "next_surah", "tafsir", "play_verse",
            "bookmark_verse", "share_verse", "back", "verse_view",
            "play_surah", "page_bookmark", "reader_settings",
            "previous_page", "next_page"
        )
        val scales = actions.map(::quranReaderActionPressScale)
        assertEquals(actions.size, scales.toSet().size)
    }

    @Test
    fun readerSettingsCategoriesHaveDistinctPressFeedback() {
        val categories = listOf("appearance", "font", "translation", "tafsir", "reciters")
        val scales = categories.map(::quranReaderSettingsCategoryPressScale)
        assertEquals(categories.size, scales.toSet().size)
    }
}
