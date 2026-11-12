package com.mashxall.unified

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanLibraryFilterTest {
    @Test
    fun allVoicesKeepsEverySourceAndOfflineStatus() {
        assertTrue(matchesAdhanLibraryFilter("all", featured = false, downloaded = false))
        assertTrue(matchesAdhanLibraryFilter("all", featured = true, downloaded = true))
        assertTrue(matchesAdhanLibraryFilter("unknown", featured = false, downloaded = false))
    }

    @Test
    fun popularVoicesFilterShowsOnlyFeaturedRecordings() {
        assertTrue(matchesAdhanLibraryFilter("featured", featured = true, downloaded = false))
        assertTrue(matchesAdhanLibraryFilter("featured", featured = true, downloaded = true))
        assertFalse(matchesAdhanLibraryFilter("featured", featured = false, downloaded = true))
        assertFalse(matchesAdhanLibraryFilter("featured", featured = false, downloaded = false))
    }

    @Test
    fun downloadedFilterShowsOnlySavedRecordings() {
        assertTrue(matchesAdhanLibraryFilter("downloaded", featured = false, downloaded = true))
        assertTrue(matchesAdhanLibraryFilter("downloaded", featured = true, downloaded = true))
        assertFalse(matchesAdhanLibraryFilter("downloaded", featured = true, downloaded = false))
    }

    @Test
    fun wellKnownVerifiedCatalogVoicesAreFeatured() {
        assertTrue(isFeaturedAdhanVoice("adhan-068", "أحمد النفيس"))
        assertTrue(isFeaturedAdhanVoice("adhan-198", "ناصر القطامي"))
        assertTrue(isFeaturedAdhanVoice("adhan-229", "ياسر الدوسري"))
        assertTrue(isFeaturedAdhanVoice("adhan-082", "أحمد الكردي"))
        assertTrue(isFeaturedAdhanVoice("adhan-031", "أذان الحرم المدني"))
        assertFalse(isFeaturedAdhanVoice("unknown-recording", "تسجيل أذان غير معروف"))
    }
}
