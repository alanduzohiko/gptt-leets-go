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

    /**
     * Resolve one dominant rule colour for each whitespace-delimited Quran word.
     * The SVG Mushaf stores every printed word as its own tagged group with
     * data-word-index-in-ayah, allowing the same Tajweed rules to colour the
     * corresponding vector word without replacing the printed page layout.
     */
    internal fun wordRuleColors(encoded: String, darkMode: Boolean = false): List<Color?> {
        if (encoded.isBlank()) return emptyList()
        val palette = if (darkMode) darkPalette else lightPalette
        val result = mutableListOf<Color?>()
        val counts = linkedMapOf<Color, Int>()
        var wordHasCharacters = false

        fun finishWord() {
            if (wordHasCharacters) {
                result += counts.maxByOrNull { it.value }?.key
                counts.clear()
                wordHasCharacters = false
            }
        }

        fun appendChunk(chunk: String, color: Color?) {
            chunk.forEach { character ->
                if (character.isWhitespace()) {
                    finishWord()
                } else {
                    wordHasCharacters = true
                    if (color != null) counts[color] = (counts[color] ?: 0) + 1
                }
            }
        }

        var cursor = 0
        while (cursor < encoded.length) {
            val tagStart = encoded.indexOf('[', cursor)
            if (tagStart < 0) {
                appendChunk(encoded.substring(cursor), null)
                break
            }
            if (tagStart > cursor) appendChunk(encoded.substring(cursor, tagStart), null)

            val ruleCode = encoded.getOrNull(tagStart + 1)
            val color = ruleCode?.let(palette::get)
            val markerEnd = encoded.indexOf('[', tagStart + 2)
            val tagEnd = if (markerEnd >= 0) encoded.indexOf(']', markerEnd + 1) else -1

            if (color != null && markerEnd > tagStart && tagEnd > markerEnd) {
                appendChunk(encoded.substring(markerEnd + 1, tagEnd), color)
                cursor = tagEnd + 1
            } else {
                appendChunk(encoded.substring(tagStart, tagStart + 1), null)
                cursor = tagStart + 1
            }
        }
        finishWord()
        return result
    }

    /**
     * Add the Tajweed colour as an inherited SVG fill on the precise tagged
     * word group (surah, ayah and word index). Geometry, SVG paths, page order,
     * typography and every original database row remain untouched.
     */
    internal fun colorMushafSvgWords(
        svgSource: String,
        taggedAyahsBySurah: Map<Int, Map<Int, String>>,
        darkMode: Boolean = false
    ): String {
        if (svgSource.isBlank() || taggedAyahsBySurah.isEmpty()) return svgSource
        val colorCache = mutableMapOf<Pair<Int, Int>, List<Color?>>()
        val wordGroup = Regex("""<g\b[^>]*data-word-index-in-ayah="[^"]+"[^>]*>""")
        return wordGroup.replace(svgSource) { match ->
            val tag = match.value
            fun attribute(name: String): String? =
                Regex("""\b${Regex.escape(name)}="([^"]*)"""")
                    .find(tag)?.groupValues?.getOrNull(1)

            val surah = attribute("data-surah")?.toIntOrNull() ?: return@replace tag
            val ayah = attribute("data-aya")?.toIntOrNull() ?: return@replace tag
            val wordIndex = attribute("data-word-index-in-ayah")?.toIntOrNull() ?: return@replace tag
            if (wordIndex < 1) return@replace tag
            val encoded = taggedAyahsBySurah[surah]?.get(ayah) ?: return@replace tag
            val wordColors = colorCache.getOrPut(surah to ayah) {
                wordRuleColors(encoded, darkMode)
            }
            val color = wordColors.getOrNull(wordIndex - 1) ?: return@replace tag
            val hex = String.format(
                Locale.US,
                "#%02X%02X%02X",
                (color.red * 255f).toInt().coerceIn(0, 255),
                (color.green * 255f).toInt().coerceIn(0, 255),
                (color.blue * 255f).toInt().coerceIn(0, 255)
            )
            val existingFill = Regex("""\sfill="[^"]*"""")
            if (existingFill.containsMatchIn(tag)) {
                existingFill.replace(tag, " fill=" + '"' + hex + '"')
            } else {
                tag.dropLast(1) + " fill=" + '"' + hex + '"' + ">"
            }
        }
    }

    internal fun ruleColor(ruleCode: Char, darkMode: Boolean = false): Color? =
        (if (darkMode) darkPalette else lightPalette)[ruleCode]
}
