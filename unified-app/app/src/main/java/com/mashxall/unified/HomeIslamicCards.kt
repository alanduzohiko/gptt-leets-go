package com.mashxall.unified

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mashxall.unified.data.Surah
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

internal data class DailyAyahReference(val surah: Int, val ayah: Int)

internal data class DailyAyahCardContent(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabic: String,
    val kurdishTranslation: String,
    val kurdishTafsir: String
)

internal data class IslamicOccasion(
    val key: String,
    val date: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val titleArabic: String,
    val titleKurdish: String,
    val titleEnglish: String
) {
    fun title(language: AppLanguage): String = when (language) {
        AppLanguage.AR -> titleArabic
        AppLanguage.CKB -> titleKurdish
        AppLanguage.EN -> titleEnglish
    }

    fun hijriDateLabel(language: AppLanguage): String =
        hijriDay.toString() + " " + hijriMonthName(hijriMonth, language) + " " + hijriYear.toString() +
            if (language == AppLanguage.AR) " هـ" else " AH"

    fun daysFrom(today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, date).coerceAtLeast(0L)
}

private data class HijriOccasionSpec(
    val key: String,
    val month: Int,
    val day: Int,
    val titleArabic: String,
    val titleKurdish: String,
    val titleEnglish: String
)

private val hijriOccasionSpecs = listOf(
    HijriOccasionSpec("islamic_new_year", 1, 1, "رأس السنة الهجرية", "ساڵی نوێی کۆچی", "Islamic New Year"),
    HijriOccasionSpec("ashura", 1, 10, "يوم عاشوراء", "ڕۆژی عاشوراء", "Day of Ashura"),
    HijriOccasionSpec("ramadan", 9, 1, "بداية شهر رمضان", "دەستپێکی مانگی ڕەمەزان", "Start of Ramadan"),
    HijriOccasionSpec("eid_fitr", 10, 1, "عيد الفطر", "جەژنی ڕەمەزان", "Eid al-Fitr"),
    HijriOccasionSpec("shawwal_six_days", 10, 2, "بداية صيام الست من شوال", "دەستپێکی ڕۆژووی شەش ڕۆژی شەوال", "Six Days of Shawwal"),
    HijriOccasionSpec("dhul_hijjah_first_ten", 12, 1, "العشر الأوائل من ذي الحجة", "دەستپێکی دە ڕۆژی یەکەمی زولحەجە", "First Ten Days of Dhul Hijjah"),
    HijriOccasionSpec("tarwiyah", 12, 8, "يوم التروية", "ڕۆژی تەروویە", "Day of Tarwiyah"),
    HijriOccasionSpec("arafah", 12, 9, "يوم عرفة", "ڕۆژی عەرەفە", "Day of Arafah"),
    HijriOccasionSpec("eid_adha", 12, 10, "عيد الأضحى", "جەژنی قوربان", "Eid al-Adha")
)

private fun hijriMonthName(month: Int, language: AppLanguage): String = when (language) {
    AppLanguage.AR -> listOf("", "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة").getOrElse(month) { "" }
    AppLanguage.CKB -> listOf("", "موحەڕەم", "سەفەر", "ڕەبیعی یەکەم", "ڕەبیعی دووەم", "جومادای یەکەم", "جومادای دووەم", "ڕەجەب", "شەعبان", "ڕەمەزان", "شەوال", "زولقەعدە", "زولحەجە").getOrElse(month) { "" }
    AppLanguage.EN -> listOf("", "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah").getOrElse(month) { "" }
}

/**
 * Uses Android/Java's Umm al-Qura Hijrah chronology for an estimated date.
 * Local moon sighting can shift an occasion by a day, so the UI says so.
 */
internal fun upcomingIslamicOccasions(today: LocalDate, limit: Int = 6): List<IslamicOccasion> {
    if (limit <= 0) return emptyList()
    val hijriYear = HijrahDate.from(today).get(ChronoField.YEAR)
    val candidates = buildList {
        for (year in hijriYear..(hijriYear + 1)) {
            hijriOccasionSpecs.forEach { spec ->
                val hijrahDate = runCatching {
                    HijrahChronology.INSTANCE.date(year, spec.month, spec.day)
                }.getOrNull() ?: return@forEach
                val gregorianDate = LocalDate.from(hijrahDate)
                if (!gregorianDate.isBefore(today)) {
                    add(
                        IslamicOccasion(
                            key = spec.key,
                            date = gregorianDate,
                            hijriDay = spec.day,
                            hijriMonth = spec.month,
                            hijriYear = year,
                            titleArabic = spec.titleArabic,
                            titleKurdish = spec.titleKurdish,
                            titleEnglish = spec.titleEnglish
                        )
                    )
                }
            }
        }
    }
    return candidates.distinctBy { it.key to it.date }.sortedBy { it.date }.take(limit)
}

/** Choose a repeatable Quran verse for each local calendar day without inventing text. */
internal fun dailyAyahReference(surahs: List<Surah>, date: LocalDate): DailyAyahReference? {
    val orderedSurahs = surahs.filter { it.number in 1..114 && it.ayahCount > 0 }.sortedBy { it.number }
    val totalAyahs = orderedSurahs.sumOf { it.ayahCount.toLong() }
    if (totalAyahs <= 0L) return null

    var offset = Math.floorMod(date.toEpochDay(), totalAyahs)
    for (surah in orderedSurahs) {
        if (offset < surah.ayahCount.toLong()) {
            return DailyAyahReference(surah.number, offset.toInt() + 1)
        }
        offset -= surah.ayahCount.toLong()
    }
    return null
}

private fun occasionCountdown(days: Long, language: AppLanguage): String = when (language) {
    AppLanguage.AR -> when (days) {
        0L -> "اليوم"
        1L -> "غدًا"
        else -> "بعد " + days.toString() + " يومًا"
    }
    AppLanguage.CKB -> when (days) {
        0L -> "ئەمڕۆ"
        1L -> "سبەی"
        else -> "دوای " + days.toString() + " ڕۆژ"
    }
    AppLanguage.EN -> when (days) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> "In " + days.toString() + " days"
    }
}

@Composable
internal fun IslamicOccasionsHomeCard(
    language: AppLanguage,
    today: LocalDate,
    foreground: Color,
    cardColor: Color,
    accent: Color
) {
    val occasions = remember(today) { upcomingIslamicOccasions(today, limit = 12) }
    val next = occasions.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.12f), cardColor, Color(0xFF1B6A55).copy(alpha = 0.035f))))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(39.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) {
                            AppLanguage.AR -> "المناسبات الإسلامية"
                            AppLanguage.CKB -> "بۆنە ئیسلامییەکان"
                            AppLanguage.EN -> "Islamic occasions"
                        },
                        color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.AR -> "المناسبة القادمة حسب التقويم الهجري"
                            AppLanguage.CKB -> "بۆنەی داهاتوو بەپێی ساڵنامەی کۆچی"
                            AppLanguage.EN -> "Upcoming dates in the Hijri calendar"
                        },
                        color = foreground.copy(alpha = 0.60f), fontSize = 10.sp
                    )
                }
            }
            if (next != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.055f))), RoundedCornerShape(18.dp))
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(next.title(language), color = foreground, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.size(3.dp))
                        Text(next.hijriDateLabel(language), color = foreground.copy(alpha = 0.63f), fontSize = 10.sp)
                        Text(occasionCountdown(next.daysFrom(today), language), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(next.daysFrom(today).toString(), color = accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            when (language) {
                                AppLanguage.AR -> "يوم"
                                AppLanguage.CKB -> "ڕۆژ"
                                AppLanguage.EN -> "days"
                            },
                            color = foreground.copy(alpha = 0.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    when (language) {
                        AppLanguage.AR -> "لا توجد مناسبة قريبة في نطاق التقويم."
                        AppLanguage.CKB -> "لە ماوەی ئەم ساڵنامەیەدا بۆنەیەکی داهاتوو نییە."
                        AppLanguage.EN -> "No upcoming date is available in this calendar range."
                    },
                    color = foreground.copy(alpha = 0.65f), fontSize = 11.sp
                )
            }
            // Keep all of the user's requested milestones visible in the home calendar.
            occasions.drop(1).take(8).forEach { event ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(accent.copy(alpha = 0.82f), RoundedCornerShape(50)))
                    Spacer(Modifier.size(8.dp))
                    Text(event.title(language), color = foreground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(occasionCountdown(event.daysFrom(today), language), color = foreground.copy(alpha = 0.58f), fontSize = 9.sp)
                }
            }
            Text(
                when (language) {
                    AppLanguage.AR -> "التواريخ تقديرية وقد تتقدم أو تتأخر يومًا بحسب رؤية الهلال."
                    AppLanguage.CKB -> "بەروارەکان خەمڵێنراون و بە پێی بینینی مانگ ڕەنگە یەک ڕۆژ بگۆڕێن."
                    AppLanguage.EN -> "Dates are estimates and may shift by a day with local moon sighting."
                },
                color = foreground.copy(alpha = 0.48f), fontSize = 9.sp, lineHeight = 14.sp
            )
        }
    }
}

@Composable
internal fun AyahOfTheDayHomeCard(
    language: AppLanguage,
    content: DailyAyahCardContent?,
    loading: Boolean,
    foreground: Color,
    cardColor: Color,
    accent: Color,
    onOpenTafsir: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A6B55).copy(alpha = 0.075f), cardColor, accent.copy(alpha = 0.055f))))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).background(accent.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) {
                            AppLanguage.AR -> "آية اليوم"
                            AppLanguage.CKB -> "ئایەتی ئەمڕۆ"
                            AppLanguage.EN -> "Ayah of the day"
                        },
                        color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.AR -> "معنى وتفسير باللغة الكردية"
                            AppLanguage.CKB -> "مانا و تەفسیری کوردی"
                            AppLanguage.EN -> "Kurdish meaning and tafsir"
                        },
                        color = foreground.copy(alpha = 0.60f), fontSize = 10.sp
                    )
                }
            }
            when {
                loading -> Text(
                    when (language) {
                        AppLanguage.AR -> "جارٍ تحميل الآية والتفسير الكردي…"
                        AppLanguage.CKB -> "ئایەت و تەفسیری کوردی بار دەکرێت…"
                        AppLanguage.EN -> "Loading the verse and Kurdish tafsir…"
                    },
                    color = foreground.copy(alpha = 0.64f), fontSize = 11.sp
                )
                content == null -> Text(
                    when (language) {
                        AppLanguage.AR -> "تعذر تحميل آية اليوم من قاعدة القرآن المحلية."
                        AppLanguage.CKB -> "نەتوانرا ئایەتی ئەمڕۆ لە داتابەیسی ناوخۆیی قورئان باربکرێت."
                        AppLanguage.EN -> "The daily verse could not be loaded from the local Quran database."
                    },
                    color = foreground.copy(alpha = 0.64f), fontSize = 11.sp
                )
                else -> {
                    Text(
                        content.arabic,
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
                        color = foreground, fontSize = 24.sp, lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Text(
                        content.kurdishTranslation.ifBlank {
                            when (language) {
                                AppLanguage.AR -> "الترجمة الكردية غير متاحة لهذه الآية."
                                AppLanguage.CKB -> "وەرگێڕانی کوردی بۆ ئەم ئایەتە بەردەست نییە."
                                AppLanguage.EN -> "Kurdish translation is unavailable for this verse."
                            }
                        },
                        color = foreground.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 21.sp,
                        textAlign = TextAlign.Start
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (language) {
                                AppLanguage.AR -> "التفسير الكردي"
                                AppLanguage.CKB -> "تەفسیری کوردی"
                                AppLanguage.EN -> "Kurdish tafsir"
                            },
                            color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            content.surahName + " • " + content.ayahNumber.toString(),
                            color = foreground.copy(alpha = 0.56f), fontSize = 9.sp
                        )
                    }
                    val tafsirPreview = content.kurdishTafsir.trim().let { full ->
                        if (full.length > 520) full.take(520).trimEnd() + "…" else full
                    }
                    Text(
                        tafsirPreview.ifBlank {
                            when (language) {
                                AppLanguage.AR -> "لا يتوفر شرح لهذه الآية في كتب التفسير الكردية المتاحة."
                                AppLanguage.CKB -> "لە تەفسیرە کوردییە بەردەستەکاندا ڕوونکردنەوەی ئەم ئایەتە نییە."
                                AppLanguage.EN -> "No explanation for this verse was found in the available Kurdish tafsir sources."
                            }
                        },
                        color = foreground.copy(alpha = 0.76f), fontSize = 11.sp, lineHeight = 18.sp
                    )
                    TextButton(
                        onClick = onOpenTafsir,
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) {
                        Text(
                            when (language) {
                                AppLanguage.AR -> "افتح التفسير الكامل"
                                AppLanguage.CKB -> "تەفسیری تەواو بکەرەوە"
                                AppLanguage.EN -> "Open full tafsir"
                            },
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
