package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PremiumActionButtonMotionTest {
    @Test
    fun everyPrimaryActionHasItsOwnPressDepth() {
        val actions = listOf(
            "try_again", "browse_reciters", "share_tafsir", "retry_mushaf",
            "open_bookmark", "load_more_hadith", "azkar_count", "tasbih_reset",
            "tasbih_new_round", "backup_save", "backup_restore", "pdf_download",
            "browse_adhan", "choose_adhan_sound", "exact_alarms",
            "browse_online_adhan", "word_study", "farmuda_copy"
        )
        val scales = actions.map(::premiumActionButtonPressScale)
        assertEquals(actions.size, scales.toSet().size)
        assertNotEquals(
            premiumActionButtonPressScale("tasbih_reset"),
            premiumActionButtonPressScale("backup_save")
        )
    }

    @Test
    fun unknownActionUsesSafeFallback() {
        assertEquals(0.956f, premiumActionButtonPressScale("unknown"), 0.0001f)
    }
}
