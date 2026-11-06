package com.mashxall.unified

import android.graphics.RectF
import androidx.core.graphics.PathParser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Common printed-Mushaf Tajweed palette:
 * - blue: Madd
 * - red: Qalqalah
 * - green: Ikhfa and Idgham
 * - orange: Iqlab and Ghunnah
 * - soft yellow word highlight: Madd
 *
 * The palette is presentation-only. Source Quran text and Tajweed markup are
 * never rewritten. The same rules drive both the text reader and SVG Mushaf.
 */
internal object TajweedText {
    private val lightPalette: Map<Char, Color?> = mapOf(
        'h' to null, // Hamzat al-wasl: retain the normal text colour
        's' to null, // Silent: retain the normal text colour
        'l' to null, // Lam shamsiyyah: retain the normal text colour
        'n' to Color(0xFF287DA8), // Madd
        'p' to Color(0xFF287DA8), // Permissible madd
        'm' to Color(0xFF287DA8), // Necessary madd
        'q' to Color(0xFFD23935), // Qalqalah
        'o' to Color(0xFF287DA8), // Obligatory madd
        'c' to Color(0xFF2B945C), // Ikhfa shafawi
        'f' to Color(0xFF2B945C), // Ikhfa
        'w' to Color(0xFF2B945C), // Idgham shafawi
        'i' to Color(0xFFDB7B2D), // Iqlab
        'a' to Color(0xFF2B945C), // Idgham with ghunnah
        'u' to Color(0xFF2B945C), // Idgham without ghunnah
        'd' to Color(0xFF2B945C), // Idgham mutajanisayn
        'b' to Color(0xFF2B945C), // Idgham mutaqaribayn
        'g' to Color(0xFFDB7B2D)  // Ghunnah
    )

    private val darkPalette: Map<Char, Color?> = mapOf(
        'h' to null,
        's' to null,
        'l' to null,
        'n' to Color(0xFF72C2E0),
        'p' to Color(0xFF72C2E0),
        'm' to Color(0xFF72C2E0),
        'q' to Color(0xFFFF7974),
        'o' to Color(0xFF72C2E0),
        'c' to Color(0xFF79D29B),
        'f' to Color(0xFF79D29B),
        'w' to Color(0xFF79D29B),
        'i' to Color(0xFFFFB369),
        'a' to Color(0xFF79D29B),
        'u' to Color(0xFF79D29B),
        'd' to Color(0xFF79D29B),
        'b' to Color(0xFF79D29B),
        'g' to Color(0xFFFFB369)
    )

    private val maddRuleCodes = setOf('n', 'p', 'm', 'o')
    private val lightMaddHighlight = Color(0xFFFFF1BE)
    private val darkMaddHighlight = Color(0xFF594A26)
    private val wordGroupOpen = Regex("""<g\\b[^>]*data-word-index-in-ayah="[^"]+"[^>]*>""")
    private val groupToken = Regex("""</?g\\b[^>]*>""")
    private val pathTag = Regex("""<path\\b[^>]*>""")
    private val pathDataAttribute = Regex("""\\bd="([^"]*)"""")

    private data class WordRuleStyle(
        val color: Color?,
        val hasMaddHighlight: Boolean
    )

    private data class StyledRange(
        val start: Int,
        val end: Int,
        val color: Color
    )

    private fun parseKnownRule(
        encoded: String,
        cursor: Int
    ): Triple<Char, Int, Int>? {
        val tagStart = encoded.indexOf('[', cursor)
        if (tagStart < 0) return null
        val ruleCode = encoded.getOrNull(tagStart + 1) ?: return null
        if (!lightPalette.containsKey(ruleCode)) return null
        val markerEnd = encoded.indexOf('[', tagStart + 2)
        val tagEnd = if (markerEnd >= 0) encoded.indexOf(']', markerEnd + 1) else -1
        if (markerEnd <= tagStart || tagEnd <= markerEnd) return null
        return Triple(ruleCode, markerEnd + 1, tagEnd)
    }

    fun annotate(encoded: String, darkMode: Boolean = false): AnnotatedString {
        val palette = if (darkMode) darkPalette else lightPalette
        val highlightColor = if (darkMode) darkMaddHighlight else lightMaddHighlight
        return buildAnnotatedString {
            val colourRanges = mutableListOf<StyledRange>()
            val highlightedWordRanges = mutableListOf<Pair<Int, Int>>()
            var currentWordStart: Int? = null
            var currentWordHasMadd = false

            fun finishWord() {
                val start = currentWordStart
                if (start != null && currentWordHasMadd && length > start) {
                    highlightedWordRanges += start to length
                }
                currentWordStart = null
                currentWordHasMadd = false
            }

            fun appendChunk(chunk: String, color: Color?, isMadd: Boolean) {
                val start = length
                chunk.forEach { character ->
                    if (character.isWhitespace()) {
                        finishWord()
                        append(character)
                    } else {
                        if (currentWordStart == null) currentWordStart = length
                        if (isMadd) currentWordHasMadd = true
                        append(character)
                    }
                }
                if (color != null && length > start) {
                    colourRanges += StyledRange(start, length, color)
                }
            }

            var cursor = 0
            while (cursor < encoded.length) {
                val tagStart = encoded.indexOf('[', cursor)
                if (tagStart < 0) {
                    appendChunk(encoded.substring(cursor), null, false)
                    break
                }
                if (tagStart > cursor) {
                    appendChunk(encoded.substring(cursor, tagStart), null, false)
                }

                val parsed = parseKnownRule(encoded, tagStart)
                if (parsed != null) {
                    val (ruleCode, textStart, tagEnd) = parsed
                    appendChunk(
                        encoded.substring(textStart, tagEnd),
                        palette[ruleCode],
                        ruleCode in maddRuleCodes
                    )
                    cursor = tagEnd + 1
                } else {
                    // Preserve malformed or unknown source markup rather than dropping it.
                    appendChunk(encoded.substring(tagStart, tagStart + 1), null, false)
                    cursor = tagStart + 1
                }
            }
            finishWord()

            colourRanges.forEach { range ->
                addStyle(SpanStyle(color = range.color), range.start, range.end)
            }
            highlightedWordRanges.forEach { range ->
                addStyle(SpanStyle(background = highlightColor), range.first, range.second)
            }
        }
    }

    /**
     * Resolve a dominant Tajweed colour for each whitespace-delimited Quran
     * word, and independently record whether that word contains a Madd rule.
     */
    private fun wordRuleStyles(encoded: String, darkMode: Boolean = false): List<WordRuleStyle> {
        if (encoded.isBlank()) return emptyList()
        val palette = if (darkMode) darkPalette else lightPalette
        val result = mutableListOf<WordRuleStyle>()
        val counts = linkedMapOf<Color, Int>()
        var wordHasCharacters = false
        var wordHasMadd = false

        fun finishWord() {
            if (wordHasCharacters) {
                result += WordRuleStyle(
                    color = counts.maxByOrNull { it.value }?.key,
                    hasMaddHighlight = wordHasMadd
                )
                counts.clear()
                wordHasCharacters = false
                wordHasMadd = false
            }
        }

        fun appendChunk(chunk: String, color: Color?, isMadd: Boolean) {
            chunk.forEach { character ->
                if (character.isWhitespace()) {
                    finishWord()
                } else {
                    wordHasCharacters = true
                    if (color != null) counts[color] = (counts[color] ?: 0) + 1
                    if (isMadd) wordHasMadd = true
                }
            }
        }

        var cursor = 0
        while (cursor < encoded.length) {
            val tagStart = encoded.indexOf('[', cursor)
            if (tagStart < 0) {
                appendChunk(encoded.substring(cursor), null, false)
                break
            }
            if (tagStart > cursor) appendChunk(encoded.substring(cursor, tagStart), null, false)

            val parsed = parseKnownRule(encoded, tagStart)
            if (parsed != null) {
                val (ruleCode, textStart, tagEnd) = parsed
                appendChunk(
                    encoded.substring(textStart, tagEnd),
                    palette[ruleCode],
                    ruleCode in maddRuleCodes
                )
                cursor = tagEnd + 1
            } else {
                appendChunk(encoded.substring(tagStart, tagStart + 1), null, false)
                cursor = tagStart + 1
            }
        }
        finishWord()
        return result
    }

    internal fun wordRuleColors(encoded: String, darkMode: Boolean = false): List<Color?> =
        wordRuleStyles(encoded, darkMode).map { it.color }

    internal fun wordMaddHighlightFlags(encoded: String): List<Boolean> =
        wordRuleStyles(encoded).map { it.hasMaddHighlight }

    /**
     * Add the shared Tajweed palette to the precise SVG word group
     * (surah, ayah and word index). Words containing Madd also receive the
     * soft yellow rectangular highlight visible in the reference page.
     *
     * SVG geometry, page order, Quran source text and database rows are not
     * modified. Only inherited paint fills and a decorative background rect
     * are added while the page is rendered.
     */
    internal fun colorMushafSvgWords(
        svgSource: String,
        taggedAyahsBySurah: Map<Int, Map<Int, String>>,
        darkMode: Boolean = false
    ): String {
        if (svgSource.isBlank() || taggedAyahsBySurah.isEmpty()) return svgSource
        val styleCache = mutableMapOf<Pair<Int, Int>, List<WordRuleStyle>>()
        val output = StringBuilder(svgSource.length + 1024)
        var copiedUntil = 0
        var searchFrom = 0

        while (true) {
            val opening = wordGroupOpen.find(svgSource, searchFrom) ?: break
            val openEnd = opening.range.last + 1
            var depth = 1
            var scanFrom = openEnd
            var closingStart = -1
            var closingEnd = -1

            // Match the closing tag of the complete word group, not its first
            // nested ligature group. A word may contain several SVG subgroups.
            while (depth > 0) {
                val token = groupToken.find(svgSource, scanFrom) ?: break
                if (token.value.startsWith("</")) {
                    depth--
                    if (depth == 0) {
                        closingStart = token.range.first
                        closingEnd = token.range.last + 1
                        break
                    }
                } else if (!token.value.endsWith("/>")) {
                    depth++
                }
                scanFrom = token.range.last + 1
            }
            if (closingStart < 0 || closingEnd <= openEnd) break

            output.append(svgSource, copiedUntil, opening.range.first)
            val openingTag = opening.value
            val body = svgSource.substring(openEnd, closingStart)

            fun attribute(name: String): String? =
                Regex("""\\b${Regex.escape(name)}="([^"]*)"""")
                    .find(openingTag)?.groupValues?.getOrNull(1)

            val surah = attribute("data-surah")?.toIntOrNull()
            val ayah = attribute("data-aya")?.toIntOrNull()
            val wordIndex = attribute("data-word-index-in-ayah")?.toIntOrNull()
            val encodedAyah = if (surah != null && ayah != null && wordIndex != null && wordIndex > 0) {
                taggedAyahsBySurah[surah]?.get(ayah)
            } else null

            if (encodedAyah.isNullOrBlank()) {
                output.append(svgSource, opening.range.first, closingEnd)
            } else {
                val wordStyles = styleCache.getOrPut(surah!! to ayah!!) {
                    wordRuleStyles(encodedAyah, darkMode)
                }
                val style = wordStyles.getOrNull(wordIndex!! - 1)
                val color = style?.color
                val renderedColor = color?.let { precompensateForNightMode(it, darkMode) }
                val hex = renderedColor?.let(::toHex)
                val colouredOpening = if (hex != null) setFill(openingTag, hex) else openingTag
                val colouredBody = if (hex != null) {
                    pathTag.replace(body) { match -> setFill(match.value, hex) }
                } else body
                val background = if (style?.hasMaddHighlight == true) {
                    svgMaddHighlightRect(body, darkMode)
                } else ""
                output.append(colouredOpening).append(background).append(colouredBody).append("</g>")
            }

            copiedUntil = closingEnd
            searchFrom = closingEnd
        }

        output.append(svgSource, copiedUntil, svgSource.length)
        return output.toString()
    }

    private fun svgMaddHighlightRect(body: String, darkMode: Boolean): String {
        val bounds = svgPathBounds(body) ?: return ""
        if (bounds.width() <= 0f || bounds.height() <= 0f) return ""
        val x = bounds.left - 0.75f
        val y = bounds.top - 0.65f
        val width = bounds.width() + 1.5f
        val height = bounds.height() + 1.3f
        val target = if (darkMode) darkMaddHighlight else lightMaddHighlight
        val source = precompensateForNightMode(target, darkMode)
        return String.format(
            Locale.US,
            """<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" rx="0.75" fill="%s"/>""",
            x, y, width, height, toHex(source)
        )
    }

    private fun svgPathBounds(body: String): RectF? {
        val union = RectF()
        var hasBounds = false
        pathTag.findAll(body).forEach { tag ->
            val pathData = pathDataAttribute.find(tag.value)?.groupValues?.getOrNull(1) ?: return@forEach
            val path = runCatching { PathParser.createPathFromPathData(pathData) }.getOrNull()
                ?: return@forEach
            val bounds = RectF()
            path.computeBounds(bounds, true)
            if (bounds.width() <= 0f || bounds.height() <= 0f) return@forEach
            if (!hasBounds) {
                union.set(bounds)
                hasBounds = true
            } else {
                union.union(bounds)
            }
        }
        return union.takeIf { hasBounds }
    }

    private fun setFill(element: String, hex: String): String {
        val fillAttribute = Regex("""\\bfill="[^"]*"""")
        var result = if (fillAttribute.containsMatchIn(element)) {
            fillAttribute.replace(element, """fill="$hex"""")
        } else {
            val closeIndex = element.lastIndexOf('>')
            if (closeIndex < 0) element else
                element.substring(0, closeIndex).trimEnd() + """ fill="$hex">"""
        }

        // Inline SVG styles override inherited fill attributes, so keep them aligned.
        val styleAttribute = Regex("""\\bstyle="([^"]*)"""")
        val style = styleAttribute.find(result)
        if (style != null) {
            val oldStyle = style.groupValues[1]
            val fillRule = Regex("""(?:^|;)\\s*fill\\s*:\\s*[^;]*""")
            val newStyle = if (fillRule.containsMatchIn(oldStyle)) {
                fillRule.replace(oldStyle) { match ->
                    val prefix = if (match.value.startsWith(";")) ";" else ""
                    prefix + "fill:$hex"
                }
            } else {
                oldStyle.trimEnd().trimEnd(';') + ";fill:$hex"
            }
            result = styleAttribute.replace(result, """style="$newStyle"""")
        }
        return result
    }

    private fun precompensateForNightMode(color: Color, darkMode: Boolean): Color =
        if (darkMode) Color(1f - color.red, 1f - color.green, 1f - color.blue) else color

    private fun toHex(color: Color): String = String.format(
        Locale.US,
        "#%02X%02X%02X",
        (color.red * 255f).roundToInt().coerceIn(0, 255),
        (color.green * 255f).roundToInt().coerceIn(0, 255),
        (color.blue * 255f).roundToInt().coerceIn(0, 255)
    )

    internal fun ruleColor(ruleCode: Char, darkMode: Boolean = false): Color? =
        (if (darkMode) darkPalette else lightPalette)[ruleCode]
}
