package com.mashxall.unified.notifications

import com.mashxall.unified.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerAlarmScheduleStatusTest {
    @Test
    fun missingSourceTimetableExplainsThatCityOrGpsMustBeCorrected() {
        val message = prayerAlarmScheduleMessage(
            PrayerAlarmScheduleResult(sourceTimetableAvailable = false, scheduledCount = 0),
            AppLanguage.EN
        )
        assertTrue(message.contains("No valid Salawati timetable"))
        assertTrue(message.contains("GPS"))
    }

    @Test
    fun zeroFutureAlarmsDoesNotClaimThatAdhanWillPlay() {
        val message = prayerAlarmScheduleMessage(
            PrayerAlarmScheduleResult(sourceTimetableAvailable = true, scheduledCount = 0),
            AppLanguage.AR
        )
        assertTrue(message.contains("لم يُجدول أي تنبيه"))
    }

    @Test
    fun scheduledAlarmsReportTheActualNumberInEachLanguage() {
        val english = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 5), AppLanguage.EN)
        val arabic = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 5), AppLanguage.AR)
        val sorani = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 3), AppLanguage.CKB)
        assertTrue(english.contains("5"))
        assertTrue(arabic.contains("خمسة"))
        assertTrue(sorani.contains("3"))
    }

    @Test
    fun schedulerErrorsAreExplainedInAllSupportedLanguages() {
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.EN).contains("permissions"))
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.AR).contains("أذونات"))
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.CKB).contains("مۆڵەتی"))
    }
}
