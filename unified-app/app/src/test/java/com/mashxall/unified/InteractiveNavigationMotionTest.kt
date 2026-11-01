package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InteractiveNavigationMotionTest {
    @Test
    fun eachBottomTabHasItsOwnSelectedIconMotion() {
        val keys = listOf("home", "quran", "azkar", "more", "settings")
        val rotations = keys.map(::bottomNavigationIconRotation)
        val scales = keys.map(::bottomNavigationIconScale)

        assertEquals(keys.size, rotations.toSet().size)
        assertEquals(keys.size, scales.toSet().size)
        assertEquals(0f, bottomNavigationIconRotation("quran"), 0.001f)
        assertEquals(42f, bottomNavigationIconRotation("settings"), 0.001f)
    }

    @Test
    fun featureTilesHaveDistinctIconArrivalPersonalities() {
        val keys = listOf("quran", "bookmarks", "hadith", "azkar", "prayer", "names", "qibla", "lexicon", "farmuda", "tasbih")
        val rotations = keys.map(::homeModuleIconRestingRotation)

        assertEquals(keys.size, rotations.toSet().size)
        assertNotEquals(homeModuleIconRestingRotation("quran"), homeModuleIconRestingRotation("tasbih"))
    }

    @Test
    fun featureTilesHaveDistinctPressDepths() {
        val keys = listOf("quran", "bookmarks", "hadith", "azkar", "prayer", "names", "qibla", "lexicon", "farmuda", "tasbih")
        val depths = keys.map(::homeModulePressScale)
        assertEquals(keys.size, depths.toSet().size)
    }

    @Test
    fun prayerTilesHavePrayerSpecificPressDepths() {
        val keys = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        val depths = keys.map(::prayerTilePressScale)
        assertEquals(keys.size, depths.toSet().size)
    }
}
