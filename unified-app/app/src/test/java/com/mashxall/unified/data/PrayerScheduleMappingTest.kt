package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerScheduleMappingTest {
    @Test
    fun aDependentLocationUsesItsDeclaredBaseSchedule() {
        assertEquals(901, ContentRepository.prayerScheduleLocationId(17, false, 901))
        assertTrue(ContentRepository.hasInheritedPrayerSchedule(false, 901))
    }

    @Test
    fun anExplicitFixedScheduleAlwaysWinsOverADependency() {
        assertEquals(17, ContentRepository.prayerScheduleLocationId(17, true, 901))
        assertFalse(ContentRepository.hasInheritedPrayerSchedule(true, 901))
    }

    @Test
    fun aLocationWithoutADependencyUsesItsOwnSchedule() {
        assertEquals(17, ContentRepository.prayerScheduleLocationId(17, false, 0))
        assertFalse(ContentRepository.hasInheritedPrayerSchedule(false, 0))
    }

    @Test
    fun manualCityAndGpsQueriesUseTheSameSqlScheduleMapping() {
        val gpsSql = ContentRepository.prayerScheduleLocationSql("l")
        val citySql = ContentRepository.prayerScheduleLocationSql("requested")
        assertEquals(citySql.replace("requested.", "l."), gpsSql)
        assertTrue(gpsSql.contains("has_fixed_prayer_time"))
        assertTrue(gpsSql.contains("prayer_dependent_id"))
        assertTrue(gpsSql.indexOf("has_fixed_prayer_time") < gpsSql.indexOf("prayer_dependent_id"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedSqlAliases() {
        ContentRepository.prayerScheduleLocationSql("location; DROP TABLE prayer_time")
    }
}
