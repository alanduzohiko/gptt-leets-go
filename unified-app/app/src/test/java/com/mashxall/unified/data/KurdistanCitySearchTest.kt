package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KurdistanCitySearchTest {
    @Test
    fun soraniAndArabicCityNamesExpandToSalawatiSourceKeys() {
        assertTrue(expandKurdistanCitySearchTerms("هەولێر").any { it.equals("erbil", ignoreCase = true) })
        assertTrue(expandKurdistanCitySearchTerms("سلێمانی").any { it.equals("sulaymaniyah", ignoreCase = true) })
        assertTrue(expandKurdistanCitySearchTerms("دهۆک").any { it.equals("duhok", ignoreCase = true) })
        assertTrue(expandKurdistanCitySearchTerms("زاخۆ").any { it.equals("zakho", ignoreCase = true) })
        assertTrue(expandKurdistanCitySearchTerms("أربيل").any { it.equals("erbil", ignoreCase = true) })
    }

    @Test
    fun partialCityQueriesExpandOnlyWhenTheyIdentifyAKnownAlias() {
        assertTrue(expandKurdistanCitySearchTerms("سلێم").any { it.equals("sulaymaniyah", ignoreCase = true) })
        assertTrue(expandKurdistanCitySearchTerms("Duh").any { it.equals("duhok", ignoreCase = true) })
        assertEquals(listOf("London"), expandKurdistanCitySearchTerms("London"))
    }

    @Test
    fun regionSearchSelectsTheCuratedKurdistanCityList() {
        assertTrue(isKurdistanRegionSearchQuery("کوردستان"))
        assertTrue(isKurdistanRegionSearchQuery("Kurdistan Region"))
        assertTrue(isKurdistanRegionSearchQuery("إقليم كردستان"))
        assertFalse(isKurdistanRegionSearchQuery("Baghdad"))
    }

    @Test
    fun clientCityFilteringUsesTheSameAliasExpansionAsDatabaseSearch() {
        assertTrue(citySearchMatchesQuery("سلێمانی", "Sulaymaniyah", "Iraq"))
        assertTrue(citySearchMatchesQuery("هەولێر", "Erbil", "Iraq"))
        assertFalse(citySearchMatchesQuery("هەولێر", "Duhok", "Iraq"))
    }

    @Test
    fun commonKurdistanCitiesHaveNativeSoraniAndArabicLabels() {
        assertEquals("هەولێر", localizedKurdistanCityName("Erbil", "ckb"))
        assertEquals("أربيل", localizedKurdistanCityName("Erbil", "ar"))
        assertEquals("سلێمانی", localizedKurdistanCityName("Sulaymaniyah", "ckb"))
        assertEquals("دهوك", localizedKurdistanCityName("Duhok", "ar"))
        assertEquals("London", localizedKurdistanCityName("London", "ckb"))
    }
}
