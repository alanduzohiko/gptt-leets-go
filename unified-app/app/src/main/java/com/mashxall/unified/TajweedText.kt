package com.mashxall.unified

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Parses the compact bracketed Tajweed markup used by AlQuranCloud's
 * quran-tajweed edition. Only the tagged letters receive rule colours; the
 * underlying Arabic text is kept in its original order.
 *
 * Palette and rule identifiers follow the MIT-licensed tajweed-ts metadata
 * (spa5k/Tajweed). A higher-contrast palette is used on dark reader surfaces.
 */
internal object TajweedText {
    private val lightPalette = mapOf(
        'h' to Color(0xFFAAAAAA), // Hamzat al-wasl
        's' to Color(0xFFAAAAAA), // Silent
        'l' to Color(0xFFAAAAAA), // Lam shamsiyyah
        'n' to Color(0xFF537FFF), // Normal madd
        'p' to Color(0xFF4050FF), // Permissible madd
        'm' to Color(0xFF000EBC), // Necessary madd
        'q' to Color(0xFFDD0008), // Qalqalah
        'o' to Color(0xFF2144C1), // Obligatory madd
        'c' to Color(0xFFD500B7), // Ikhfa shafawi
        'f' to Color(0xFF9400A8), // Ikhfa
        'w' to Color(0xFF58B800), // Idgham shafawi
        'i' to Color(0xFF26BFFD), // Iqlab
        'a' to Color(0xFF169200), // Idgham with ghunnah
        'u' to Color(0xFF169200), // Idgham without ghunnah
        'd' to Color(0xFFA1A1A1), // Idgham mutajanisayn
        'b' to Color(0xFFA1A1A1), // Idgham mutaqaribayn
        'g' to Color(0xFFFF7E1E)  // Ghunnah
    )

    private val darkPalette = mapOf(
        'h' to Color(0xFFB9B9B9),
        's' to Color(0xFFB9B9B9),
        'l' to Color(0xFFB9B9B9),
        'n' to Color(0xFF91B5FF),
        'p' to Color(0xFFA1A5FF),
        'm' to Color(0xFF929CFF),
        'q' to Color(0xFFFF7379),
        'o' to Color(0xFF9CB6FF),
        'c' to Color(0xFFF48BE4),
        'f' to Color(0xFFD49AE7),
        'w' to Color(0xFF93DA56),
        'i' to Color(0xFF64D8FF),
        'a' to Color(0xFF80DA7F),
        'u' to Color(0xFF80DA7F),
        'd' to Color(0xFFC5C5C5),
        'b' to Color(0xFFC5C5C5),
        'g' to Color(0xFFFFB16F)
    )

    fun annotate(encoded: String, darkMode: Boolean = false): AnnotatedString {
        val palette = if (darkMode) darkPalette else lightPalette
        return buildAnnotatedString {
            var cursor = 0
            while (cursor < encoded.length) {
                val tagStart = encoded.indexOf('[', cursor)
                if (tagStart < 0) {
                    append(encoded.substring(cursor))
                    break
                }
                if (tagStart > cursor) append(encoded.substring(cursor, tagStart))

                val ruleCode = encoded.getOrNull(tagStart + 1)
                val color = ruleCode?.let(palette::get)
                val markerEnd = encoded.indexOf('[', tagStart + 2)
                val tagEnd = if (markerEnd >= 0) encoded.indexOf(']', markerEnd + 1) else -1

                if (color != null && markerEnd > tagStart && tagEnd > markerEnd) {
                    // The marker may carry a numeric rule identifier, e.g. [h:9421[ٱ].
                    val textStart = markerEnd + 1
                    val spanStart = length
                    append(encoded.substring(textStart, tagEnd))
                    if (length > spanStart) {
                        addStyle(SpanStyle(color = color), spanStart, length)
                    }
                    cursor = tagEnd + 1
                } else {
                    // Keep unknown or malformed markup verbatim rather than dropping text.
                    append(encoded[tagStart])
                    cursor = tagStart + 1
                }
            }
        }
    }

    internal fun ruleColor(ruleCode: Char, darkMode: Boolean = false): Color? =
        (if (darkMode) darkPalette else lightPalette)[ruleCode]
}
