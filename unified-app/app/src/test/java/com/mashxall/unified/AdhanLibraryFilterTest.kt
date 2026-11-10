package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanLibraryFilterTest {
    @Test
    fun allFilterKeepsBothSourceCategoriesAndDownloadedOrOnlineTracks() {
        assertTrue(matchesAdhanLibraryFilter("all", "muezzin", downloaded = false))
        assertTrue(matchesAdhanLibraryFilter("all", "mosque", downloaded = true))
        assertTrue(matchesAdhanLibraryFilter("all", "unknown-source-category", downloaded = false))
    }

    @Test
    fun muezzinAndMosqueFiltersStaySeparate() {
        assertTrue(matchesAdhanLibraryFilter("muezzin", "muezzin", downloaded = false))
        assertFalse(matchesAdhanLibraryFilter("muezzin", "mosque", downloaded = true))
        assertTrue(matchesAdhanLibraryFilter("mosque", "mosque", downloaded = false))
        assertFalse(matchesAdhanLibraryFilter("mosque", "muezzin", downloaded = true))
    }

    @Test
    fun downloadedFilterShowsOnlySavedTracks() {
        assertTrue(matchesAdhanLibraryFilter("downloaded", "mosque", downloaded = true))
        assertFalse(matchesAdhanLibraryFilter("downloaded", "muezzin", downloaded = false))
    }

    @Test
    fun unknownFilterFallsBackToAllRatherThanHidingSourceData() {
        assertTrue(matchesAdhanLibraryFilter("invalid", "mosque", downloaded = false))
    }
}
