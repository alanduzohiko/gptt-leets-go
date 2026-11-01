package com.mashxall.unified.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReciterDisplayNameTest {
    @Test
    fun removesTheSourceDatabaseGaplessSuffixFromArabicAndSoraniNames() {
        assertEquals("سعد الغامدي", reciterDisplayName("سعد الغامدي (بێ بچڕان)"))
        assertEquals("أبو بكر الشاطري", reciterDisplayName("أبو بكر الشاطري (بێ بچڕان)"))
    }

    @Test
    fun removesGaplessEnglishLabelWithoutRemovingTheActualReciterStyle() {
        assertEquals("Abu Bakr Al-Shatri", reciterDisplayName("Abu Bakr Al-Shatri (Gapless)"))
        assertEquals("Abd Al-Basit (Mujawwad)", reciterDisplayName("Abd Al-Basit (Mujawwad) (Gapless)"))
    }

    @Test
    fun keepsOtherParentheticalNamesAndCollapsesSpacing() {
        assertEquals("Mahmood Khaleel Al-Husary (Muratal)", reciterDisplayName("  Mahmood   Khaleel Al-Husary (Muratal)  "))
    }
}
