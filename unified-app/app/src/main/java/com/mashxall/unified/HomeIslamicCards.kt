package com.mashxall.unified

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mashxall.unified.data.Surah
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class DailyAyahReference(val surah: Int, val ayah: Int)

internal data class DailyAyahCardContent(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabic: String,
    val kurdishTranslation: String,
    val kurdishTafsir: String
)


internal data class DailyHadithCardContent(
    val hadithId: Int,
    val arabic: String,
    val sorani: String,
    val reference: String
)


/**
 * The home carousel follows the natural rightward swipe from the Ayah to the
 * Hadith, and the opposite swipe returns to the Ayah.
 */
internal fun dailyAyahHadithSwipeTarget(showingHadith: Boolean, horizontalDragPx: Float): Boolean? {
    if (!horizontalDragPx.isFinite()) return null
    return when {
        !showingHadith && horizontalDragPx > 72f -> true
        showingHadith && horizontalDragPx < -72f -> false
        else -> null
    }
}

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

    /** Gregorian calendar date, displayed separately from the estimated Hijri date. */
    fun gregorianDateLabel(): String =
        String.format(Locale.ROOT, "%02d/%02d/%04d", date.dayOfMonth, date.monthValue, date.year)

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

internal fun hijriMonthName(month: Int, language: AppLanguage): String = when (language) {
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

/** The home card keeps only the next occasion visible until the user expands it. */
internal fun visibleAdditionalIslamicOccasions(
    occasions: List<IslamicOccasion>,
    expanded: Boolean
): List<IslamicOccasion> = if (expanded) occasions.drop(1) else emptyList()

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
    var expanded by remember(today) { androidx.compose.runtime.mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(260)),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.13f), cardColor, Color(0xFF1B6A55).copy(alpha = 0.045f))))
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.size(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (language) {
                            AppLanguage.AR -> "المناسبات الإسلامية"
                            AppLanguage.CKB -> "بۆنەکان"
                            AppLanguage.EN -> "Islamic occasions"
                        },
                        color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (language) {
                            AppLanguage.AR -> "المناسبة القادمة حسب التقويم الهجري"
                            AppLanguage.CKB -> "بۆنەی داهاتوو بەپێی ساڵنامەی کۆچی"
                            AppLanguage.EN -> "Upcoming dates in the Hijri calendar"
                        },
                        color = foreground.copy(alpha = 0.58f), fontSize = 9.sp
                    )
                }
                Box(
                    Modifier.background(accent.copy(alpha = 0.10f), RoundedCornerShape(50))
                        .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        when (language) {
                            AppLanguage.AR -> "التقويم"
                            AppLanguage.CKB -> "ساڵنامە"
                            AppLanguage.EN -> "Calendar"
                        },
                        color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (next != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.065f))),
                            RoundedCornerShape(17.dp)
                        )
                        .border(1.dp, accent.copy(alpha = 0.23f), RoundedCornerShape(17.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(next.title(language), color = foreground, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            (if (language == AppLanguage.AR) "هجري  " else if (language == AppLanguage.CKB) "کۆچی  " else "Hijri  ") + next.hijriDateLabel(language),
                            color = foreground.copy(alpha = 0.75f), fontSize = 9.sp
                        )
                        Text(
                            (if (language == AppLanguage.AR) "ميلادي  " else if (language == AppLanguage.CKB) "زایینی  " else "Gregorian  ") + next.gregorianDateLabel(),
                            color = foreground.copy(alpha = 0.60f), fontSize = 9.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            occasionCountdown(next.daysFrom(today), language),
                            color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Column(
                        Modifier.padding(start = 10.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(next.daysFrom(today).toString(), color = accent, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            when (language) {
                                AppLanguage.AR -> "يوم"
                                AppLanguage.CKB -> "ڕۆژ"
                                AppLanguage.EN -> "days"
                            },
                            color = foreground.copy(alpha = 0.62f), fontSize = 8.sp, fontWeight = FontWeight.Bold
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
                    color = foreground.copy(alpha = 0.65f), fontSize = 10.sp
                )
            }

            if (occasions.size > 1) {
                Card(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.085f)),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(28.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.size(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (language) {
                                    AppLanguage.AR -> if (expanded) "إخفاء المناسبات" else "استكشف بقية المناسبات"
                                    AppLanguage.CKB -> if (expanded) "شاردنەوەی بۆنەکان" else "بینینی بۆنەکانی دیکە"
                                    AppLanguage.EN -> if (expanded) "Collapse calendar" else "Explore other occasions"
                                },
                                color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                when (language) {
                                    AppLanguage.AR -> (occasions.size - 1).toString() + " مناسبة إضافية"
                                    AppLanguage.CKB -> (occasions.size - 1).toString() + " بۆنەی تر"
                                    AppLanguage.EN -> (occasions.size - 1).toString() + " more dates"
                                },
                                color = foreground.copy(alpha = 0.56f), fontSize = 8.sp
                            )
                        }
                        Text(
                            occasions.size.toString(),
                            color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(expandFrom = Alignment.Top, animationSpec = tween(240)),
                exit = fadeOut(animationSpec = tween(130)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200))
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(accent.copy(alpha = 0.045f), RoundedCornerShape(17.dp))
                        .border(1.dp, accent.copy(alpha = 0.13f), RoundedCornerShape(17.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    visibleAdditionalIslamicOccasions(occasions, expanded).forEachIndexed { index, event ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(listOf(cardColor.copy(alpha = 0.96f), accent.copy(alpha = 0.055f))),
                                    RoundedCornerShape(13.dp)
                                )
                                .border(1.dp, accent.copy(alpha = 0.11f), RoundedCornerShape(13.dp))
                                .padding(horizontal = 9.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(27.dp).background(accent.copy(alpha = 0.13f), RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text((index + 2).toString(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.size(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(event.title(language), color = foreground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    event.hijriDateLabel(language) + "  •  " + event.gregorianDateLabel(),
                                    color = foreground.copy(alpha = 0.53f), fontSize = 8.sp
                                )
                            }
                            Box(
                                Modifier.background(accent.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    occasionCountdown(event.daysFrom(today), language),
                                    color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Text(
                when (language) {
                    AppLanguage.AR -> "التواريخ الهجرية تقديرية وقد تختلف يومًا بحسب رؤية الهلال."
                    AppLanguage.CKB -> "بەروارە کۆچییەکان خەمڵێنراون و ڕەنگە بە پێی بینینی مانگ یەک ڕۆژ بگۆڕێن."
                    AppLanguage.EN -> "Hijri dates are estimates and may shift by a day with local moon sighting."
                },
                color = foreground.copy(alpha = 0.46f), fontSize = 8.sp, lineHeight = 12.sp
            )
        }
    }
}


@Composable
internal fun AyahOfTheDayHomeCard(
    language: AppLanguage,
    content: DailyAyahCardContent?,
    loading: Boolean,
    hadithContent: DailyHadithCardContent?,
    hadithLoading: Boolean,
    foreground: Color,
    cardColor: Color,
    accent: Color,
    onOpenTafsir: () -> Unit
) {
    var showHadith by remember(content?.surahNumber, content?.ayahNumber, hadithContent?.hadithId) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    Card(
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    dailyAyahHadithSwipeTarget(showHadith, totalDrag)?.let { showHadith = it }
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f }
            )
        },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A6B55).copy(alpha = 0.075f), cardColor, accent.copy(alpha = 0.055f))))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedContent(
                targetState = showHadith,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(220)) { -it } + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { it } + fadeOut(animationSpec = tween(140)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(animationSpec = tween(180))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(220)) { -it } + fadeOut(animationSpec = tween(140)))
                    }
                },
                label = "daily-ayah-hadith-swipe"
            ) { hadithPage ->
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(35.dp).background(accent.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.size(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    hadithPage && language == AppLanguage.AR -> "حديث اليوم"
                                    hadithPage && language == AppLanguage.CKB -> "فەرموودەی ئەمڕۆ"
                                    hadithPage -> "Hadith of the day"
                                    language == AppLanguage.AR -> "آية اليوم"
                                    language == AppLanguage.CKB -> "ئایەتی ئەمڕۆ"
                                    else -> "Ayah of the day"
                                },
                                color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    hadithPage && language == AppLanguage.AR -> "النص العربي وترجمته بالسورانية"
                                    hadithPage && language == AppLanguage.CKB -> "دەقی عەرەبی و وەرگێڕانی سۆرانی"
                                    hadithPage -> "Arabic narration with Sorani translation"
                                    language == AppLanguage.AR -> "المعنى والتفسير باللغة الكردية"
                                    language == AppLanguage.CKB -> "مانا و تەفسیری کوردی"
                                    else -> "Kurdish meaning and tafsir"
                                },
                                color = foreground.copy(alpha = 0.58f), fontSize = 9.sp
                            )
                        }
                        Box(
                            Modifier.background(accent.copy(alpha = 0.10f), RoundedCornerShape(50))
                                .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (hadithPage) "02 / 02" else "01 / 02", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when {
                        hadithPage && hadithLoading -> Text(
                            when (language) {
                                AppLanguage.AR -> "جارٍ تحميل حديث اليوم والترجمة السورانية…"
                                AppLanguage.CKB -> "فەرموودەی ئەمڕۆ و وەرگێڕانی سۆرانی بار دەکرێت…"
                                AppLanguage.EN -> "Loading today's Hadith and Sorani translation…"
                            },
                            color = foreground.copy(alpha = 0.65f), fontSize = 10.sp
                        )
                        hadithPage && hadithContent == null -> Text(
                            when (language) {
                                AppLanguage.AR -> "لا تتوفر حاليًا رواية بترجمة سورانية ضمن البيانات المحلية."
                                AppLanguage.CKB -> "لە داتای ناوخۆییدا ئێستا فەرموودەیەکی وەرگێڕدراو بۆ سۆرانی بەردەست نییە."
                                AppLanguage.EN -> "No Hadith with a Sorani translation is available in the local data."
                            },
                            color = foreground.copy(alpha = 0.65f), fontSize = 10.sp, lineHeight = 16.sp
                        )
                        hadithPage -> {
                            Text(
                                hadithContent!!.arabic,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                color = foreground, fontSize = 19.sp, lineHeight = 31.sp,
                                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 5
                            )
                            Text(
                                hadithContent.sorani,
                                color = foreground.copy(alpha = 0.84f), fontSize = 11.sp, lineHeight = 18.sp,
                                textAlign = TextAlign.Start, maxLines = 5
                            )
                            Text(
                                hadithContent.reference,
                                color = accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                        loading -> Text(
                            when (language) {
                                AppLanguage.AR -> "جارٍ تحميل آية اليوم…"
                                AppLanguage.CKB -> "ئایەتی ئەمڕۆ بار دەکرێت…"
                                AppLanguage.EN -> "Loading today's ayah…"
                            },
                            color = foreground.copy(alpha = 0.64f), fontSize = 10.sp
                        )
                        content == null -> Text(
                            when (language) {
                                AppLanguage.AR -> "تعذر تحميل آية اليوم من قاعدة القرآن المحلية."
                                AppLanguage.CKB -> "نەتوانرا ئایەتی ئەمڕۆ لە داتابەیسی ناوخۆیی قورئان باربکرێت."
                                AppLanguage.EN -> "Today's verse could not be loaded from the local Quran database."
                            },
                            color = foreground.copy(alpha = 0.64f), fontSize = 10.sp
                        )
                        else -> {
                            Text(
                                content.arabic,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                color = foreground, fontSize = 22.sp, lineHeight = 35.sp,
                                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3
                            )
                            Text(
                                content.kurdishTranslation.ifBlank {
                                    when (language) {
                                        AppLanguage.AR -> "الترجمة الكردية غير متاحة لهذه الآية."
                                        AppLanguage.CKB -> "وەرگێڕانی کوردی بۆ ئەم ئایەتە بەردەست نییە."
                                        AppLanguage.EN -> "Kurdish translation is unavailable for this verse."
                                    }
                                },
                                color = foreground.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 18.sp,
                                textAlign = TextAlign.Start, maxLines = 3
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(top = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when (language) {
                                        AppLanguage.AR -> "تفسير كردي"
                                        AppLanguage.CKB -> "تەفسیری کوردی"
                                        AppLanguage.EN -> "Kurdish tafsir"
                                    },
                                    color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    content.surahName + " • " + content.ayahNumber.toString(),
                                    color = foreground.copy(alpha = 0.55f), fontSize = 8.sp
                                )
                            }
                            val tafsirPreview = content.kurdishTafsir.trim().let { full ->
                                if (full.length > 115) full.take(115).trimEnd() + "…" else full
                            }
                            if (tafsirPreview.isNotBlank()) {
                                Text(
                                    tafsirPreview,
                                    color = foreground.copy(alpha = 0.70f), fontSize = 9.sp, lineHeight = 14.sp, maxLines = 2
                                )
                            }
                            TextButton(
                                onClick = onOpenTafsir,
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = accent)
                            ) {
                                Text(
                                    when (language) {
                                        AppLanguage.AR -> "افتح التفسير الكامل"
                                        AppLanguage.CKB -> "تەفسیری تەواو بکەرەوە"
                                        AppLanguage.EN -> "Open full tafsir"
                                    },
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.width(15.dp).height(3.dp).background(
                        if (!showHadith) accent else accent.copy(alpha = 0.22f),
                        RoundedCornerShape(50)
                    )
                )
                Spacer(Modifier.size(5.dp))
                Box(
                    Modifier.width(15.dp).height(3.dp).background(
                        if (showHadith) accent else accent.copy(alpha = 0.22f),
                        RoundedCornerShape(50)
                    )
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    when (language) {
                        AppLanguage.AR -> if (showHadith) "اسحب لليمين للعودة إلى الآية" else "اسحب لليسار لعرض حديث اليوم"
                        AppLanguage.CKB -> if (showHadith) "بۆ گەڕانەوە بۆ ئایەت بۆ لای ڕاست ڕابکێشە" else "بۆ بینینی فەرموودە بۆ لای چەپ ڕابکێشە"
                        AppLanguage.EN -> if (showHadith) "Swipe right to return to the ayah" else "Swipe left to reveal today's Hadith"
                    },
                    color = foreground.copy(alpha = 0.48f), fontSize = 8.sp
                )
            }
        }
    }
}
