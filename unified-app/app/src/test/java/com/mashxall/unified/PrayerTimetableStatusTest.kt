package com.mashxall.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimetableStatusTest {
    @Test
    fun loadingTakesPriorityWhileSourceIsBeingRefreshed() {
        assertEquals(
            PrayerTimetableStatus.UPDATING,
            resolvePrayerTimetableStatus(loading = true, hasSchedule = true)
        )
        assertEquals(
            PrayerTimetableStatus.UPDATING,
            resolvePrayerTimetableStatus(loading = true, hasSchedule = false)
        )
    }

    @Test
    fun scheduleIsReadyOnlyWhenARealScheduleWasLoaded() {
        assertEquals(
            PrayerTimetableStatus.SCHEDULE_READY,
            resolvePrayerTimetableStatus(loading = false, hasSchedule = true)
        )
        assertEquals(
            PrayerTimetableStatus.UNAVAILABLE,
            resolvePrayerTimetableStatus(loading = false, hasSchedule = false)
        )
    }

    @Test
    fun statusLabelsAreLocalizedInAllSupportedLanguages() {
        assertEquals("UPDATING", prayerTimetableStatusLabel(PrayerTimetableStatus.UPDATING, AppLanguage.EN))
        assertEquals("جارٍ التحديث", prayerTimetableStatusLabel(PrayerTimetableStatus.UPDATING, AppLanguage.AR))
        assertEquals("نوێ دەکرێتەوە", prayerTimetableStatusLabel(PrayerTimetableStatus.UPDATING, AppLanguage.CKB))

        assertEquals("SOURCE SCHEDULE READY", prayerTimetableStatusLabel(PrayerTimetableStatus.SCHEDULE_READY, AppLanguage.EN))
        assertEquals("جدول المصدر جاهز", prayerTimetableStatusLabel(PrayerTimetableStatus.SCHEDULE_READY, AppLanguage.AR))
        assertEquals("خشتەی سەرچاوە ئامادەیە", prayerTimetableStatusLabel(PrayerTimetableStatus.SCHEDULE_READY, AppLanguage.CKB))

        assertEquals("NOT AVAILABLE", prayerTimetableStatusLabel(PrayerTimetableStatus.UNAVAILABLE, AppLanguage.EN))
        assertEquals("غير متاح", prayerTimetableStatusLabel(PrayerTimetableStatus.UNAVAILABLE, AppLanguage.AR))
        assertEquals("بەردەست نییە", prayerTimetableStatusLabel(PrayerTimetableStatus.UNAVAILABLE, AppLanguage.CKB))
    }
}
