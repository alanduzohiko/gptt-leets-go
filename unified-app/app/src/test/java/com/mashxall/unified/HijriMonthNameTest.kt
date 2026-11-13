package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HijriMonthNameTest {
    @Test
    fun jumadaAlThaniUsesTheHijriNameInsteadOfTheGregorianJuneName() {
        assertEquals("جمادى الآخرة", hijriMonthName(5, AppLanguage.AR))
        assertEquals("جومادای دووەم", hijriMonthName(5, AppLanguage.CKB))
        assertEquals("Jumada al-Thani", hijriMonthName(5, AppLanguage.EN))
    }

    @Test
    fun allTwelveHijriMonthIndexesAreMappedAndInvalidIndexesAreRejected() {
        for (month in 0..11) {
            org.junit.Assert.assertNotNull(hijriMonthName(month, AppLanguage.AR))
        }
        assertNull(hijriMonthName(-1, AppLanguage.AR))
        assertNull(hijriMonthName(12, AppLanguage.AR))
    }
}
