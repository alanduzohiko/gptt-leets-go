package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanLibraryFilterTest {
    @Test
    fun allVoicesKeepsEveryOnlineAndBundledRecording() {
        assertTrue(matchesAdhanLibraryFilter("all", downloaded = false))
        assertTrue(matchesAdhanLibraryFilter("all", downloaded = true))
        // Unknown and legacy filter names safely fall back to all voices.
        assertTrue(matchesAdhanLibraryFilter("featured", downloaded = false))
        assertTrue(matchesAdhanLibraryFilter("unknown", downloaded = true))
    }

    @Test
    fun downloadedFilterShowsOnlySavedRecordings() {
        assertTrue(matchesAdhanLibraryFilter("downloaded", downloaded = true))
        assertFalse(matchesAdhanLibraryFilter("downloaded", downloaded = false))
    }
}
