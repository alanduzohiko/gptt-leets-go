package com.mashxall.unified.notifications

import com.mashxall.unified.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerAlarmScheduleStatusTest {
    @Test
    fun eachPrayerCanBeEnabledIndependentlyAndJumuahReplacesDhuhrOnlyOnFriday() {
        val enabled = setOf("fajr", "dhuhr", "asr", "jumuah")
        assertEquals(
            listOf("fajr", "jumuah", "asr"),
            PrayerAlarmScheduler.prayerAlertCodesForDay(enabled, isFriday = true)
        )
        assertEquals(
            listOf("fajr", "dhuhr", "asr"),
            PrayerAlarmScheduler.prayerAlertCodesForDay(enabled, isFriday = false)
        )
    }

    @Test
    fun FridayJumuahCanBeEnabledWithoutEnablingDhuhr() {
        assertEquals(
            listOf("jumuah"),
            PrayerAlarmScheduler.prayerAlertCodesForDay(setOf("jumuah"), isFriday = true)
        )
        assertEquals(emptyList<String>(), PrayerAlarmScheduler.prayerAlertCodesForDay(setOf("jumuah"), isFriday = false))
    }

    @Test
    fun allSixAlarmCodesHaveDistinctPreferenceKeys() {
        assertEquals(
            PrayerAlarmScheduler.ALERT_CODES.size,
            PrayerAlarmScheduler.ALERT_CODES.map(PrayerAlarmScheduler::enabledPreferenceKey).distinct().size
        )
        assertEquals(
            PrayerAlarmScheduler.ALERT_CODES.size,
            PrayerAlarmScheduler.ALERT_CODES.map(PrayerAlarmScheduler::audioPreferenceKey).distinct().size
        )
    }

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
    fun masterAdhanSwitchOverridesEveryIndividualPrayerSwitch() {
        assertEquals(false, PrayerAlarmScheduler.effectivePrayerAlertEnabled(false, true))
        assertEquals(false, PrayerAlarmScheduler.effectivePrayerAlertEnabled(false, false))
        assertEquals(true, PrayerAlarmScheduler.effectivePrayerAlertEnabled(true, true))
        assertEquals(false, PrayerAlarmScheduler.effectivePrayerAlertEnabled(true, false))
    }

    @Test
    fun zeroFutureAlarmsDoesNotClaimThatAdhanWillPlay() {
        val message = prayerAlarmScheduleMessage(
            PrayerAlarmScheduleResult(sourceTimetableAvailable = true, scheduledCount = 0),
            AppLanguage.AR
        )
        assertTrue(message.contains("لم تُجدول تنبيهات قادمة"))
    }

    @Test
    fun scheduledAlarmsReportTheActualNumberInEachLanguage() {
        val english = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 5), AppLanguage.EN)
        val arabic = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 5), AppLanguage.AR)
        val sorani = prayerAlarmScheduleMessage(PrayerAlarmScheduleResult(true, 3), AppLanguage.CKB)
        assertTrue(english.contains("5"))
        assertTrue(arabic.contains("5"))
        assertTrue(sorani.contains("3"))
    }

    @Test
    fun schedulerErrorsAreExplainedInAllSupportedLanguages() {
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.EN).contains("permissions"))
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.AR).contains("أذونات"))
        assertTrue(prayerAlarmScheduleFailureMessage(AppLanguage.CKB).contains("مۆڵەتی"))
    }
}
