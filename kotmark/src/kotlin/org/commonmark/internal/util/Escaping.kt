package org.commonmark.internal.util

/**
 * HTML/entity/URL escaping helpers.
 */
object Escaping {

    const val ESCAPABLE: String = "[!\"#\$%&'()*+,./:;<=>?@\\[\\\\\\]^_`{|}~-]"
    const val ENTITY: String = "&(?:#x[a-f0-9]{1,6}|#[0-9]{1,7}|[a-z][a-z0-9]{1,31});"

    private val BACKSLASH_OR_AMP: Regex = Regex("[\\\\&]")
    private val ENTITY_OR_ESCAPED_CHAR: Regex =
        Regex("\\\\$ESCAPABLE|$ENTITY", setOf(RegexOption.IGNORE_CASE))
    private val ESCAPE_IN_URI: Regex =
        Regex("(%[a-fA-F0-9]{0,2}|[^:/?#@!\$&'()*+,;=a-zA-Z0-9\\-._~])")
    private val WHITESPACE: Regex = Regex("[ \t\r\n]+")

    private val HEX_DIGITS: CharArray =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    fun escapeHtml(input: String): String {
        // Avoid building a new string in the majority of cases (nothing to escape)
        var sb: StringBuilder? = null
        for (i in input.indices) {
            val c = input[i]
            val replacement: String? = when (c) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                else -> null
            }
            if (replacement == null) {
                sb?.append(c)
            } else {
                if (sb == null) {
                    sb = StringBuilder()
                    sb.append(input, 0, i)
                }
                sb.append(replacement)
            }
        }
        return sb?.toString() ?: input
    }

    /** Replace entities and backslash escapes with literal characters. */
    fun unescapeString(s: String): String {
        if (BACKSLASH_OR_AMP.containsMatchIn(s)) {
            return replaceAll(ENTITY_OR_ESCAPED_CHAR, s) { input, sb ->
                if (input[0] == '\\') {
                    sb.append(input, 1, input.length)
                } else {
                    sb.append(Html5Entities.entityToString(input))
                }
            }
        }
        return s
    }

    fun percentEncodeUrl(s: String): String =
        replaceAll(ESCAPE_IN_URI, s) { input, sb ->
            if (input.startsWith("%")) {
                if (input.length == 3) {
                    sb.append(input) // Already percent-encoded, preserve
                } else {
                    sb.append("%25") // %25 is the percent-encoding for %
                    sb.append(input, 1, input.length)
                }
            } else {
                val bytes = encodeUtf8(input)
                for (b in bytes) {
                    sb.append('%')
                    sb.append(HEX_DIGITS[(b.toInt() shr 4) and 0xF])
                    sb.append(HEX_DIGITS[b.toInt() and 0xF])
                }
            }
        }

    fun normalizeLabelContent(input: String): String {
        val trimmed = input.trim { it <= '\u0020' }
        // Case fold U+1E9E to SS via lower then upper.
        val caseFolded = trimmed.lowercase().uppercase()
        return WHITESPACE.replace(caseFolded, " ")
    }

    private inline fun replaceAll(p: Regex, s: String, replacer: (String, StringBuilder) -> Unit): String {
        val sb = StringBuilder(s.length + 16)
        var lastEnd = 0
        for (match in p.findAll(s)) {
            sb.append(s, lastEnd, match.range.first)
            replacer(match.value, sb)
            lastEnd = match.range.last + 1
        }
        if (lastEnd != s.length) {
            sb.append(s, lastEnd, s.length)
        }
        return sb.toString()
    }

    private fun encodeUtf8(input: String): ByteArray {
        return input.encodeToByteArray()
    }
}
