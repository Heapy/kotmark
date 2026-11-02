package io.heapy.kotmark.text

/**
 * Functions for finding characters in strings or checking characters.
 */
object Characters {

    fun find(c: Char, s: CharSequence, startIndex: Int): Int {
        val length = s.length
        for (i in startIndex until length) {
            if (s[i] == c) {
                return i
            }
        }
        return -1
    }

    fun findLineBreak(s: CharSequence, startIndex: Int): Int {
        val length = s.length
        for (i in startIndex until length) {
            when (s[i]) {
                '\n', '\r' -> return i
            }
        }
        return -1
    }

    /**
     * @see <a href="https://spec.commonmark.org/0.31.2/#blank-line">blank line</a>
     */
    fun isBlank(s: CharSequence): Boolean {
        return skipSpaceTab(s, 0, s.length) == s.length
    }

    fun hasNonSpace(s: CharSequence): Boolean {
        val length = s.length
        val skipped = skip(' ', s, 0, length)
        return skipped != length
    }

    fun isLetter(s: CharSequence, index: Int): Boolean {
        val codePoint = codePointAt(s, index)
        return isLetter(codePoint)
    }

    fun isSpaceOrTab(s: CharSequence, index: Int): Boolean {
        if (index < s.length) {
            when (s[index]) {
                ' ', '\t' -> return true
            }
        }
        return false
    }

    /**
     * @see <a href="https://spec.commonmark.org/0.31.2/#unicode-punctuation-character">Unicode punctuation character</a>
     */
    fun isPunctuationCodePoint(codePoint: Int): Boolean {
        val type = getType(codePoint)
        when (type) {
            CharacterCategory.DASH_PUNCTUATION,
            CharacterCategory.START_PUNCTUATION,
            CharacterCategory.END_PUNCTUATION,
            CharacterCategory.CONNECTOR_PUNCTUATION,
            CharacterCategory.OTHER_PUNCTUATION,
            CharacterCategory.INITIAL_QUOTE_PUNCTUATION,
            CharacterCategory.FINAL_QUOTE_PUNCTUATION,
            CharacterCategory.MATH_SYMBOL,
            CharacterCategory.CURRENCY_SYMBOL,
            CharacterCategory.MODIFIER_SYMBOL,
            CharacterCategory.OTHER_SYMBOL -> return true
        }
        when (codePoint) {
            '$'.code, '+'.code, '<'.code, '='.code, '>'.code, '^'.code, '`'.code, '|'.code, '~'.code -> return true
        }
        return false
    }

    /**
     * Check whether the provided code point is a Unicode whitespace character as defined in the spec.
     *
     * @see <a href="https://spec.commonmark.org/0.31.2/#unicode-whitespace-character">Unicode whitespace character</a>
     */
    fun isWhitespaceCodePoint(codePoint: Int): Boolean {
        when (codePoint) {
            ' '.code, '\t'.code, '\n'.code, '\u000C'.code /* \f */, '\r'.code -> return true
        }
        return getType(codePoint) == CharacterCategory.SPACE_SEPARATOR
    }

    fun skip(skip: Char, s: CharSequence, startIndex: Int, endIndex: Int): Int {
        for (i in startIndex until endIndex) {
            if (s[i] != skip) {
                return i
            }
        }
        return endIndex
    }

    fun skipBackwards(skip: Char, s: CharSequence, startIndex: Int, lastIndex: Int): Int {
        for (i in startIndex downTo lastIndex) {
            if (s[i] != skip) {
                return i
            }
        }
        return lastIndex - 1
    }

    fun skipSpaceTab(s: CharSequence, startIndex: Int, endIndex: Int): Int {
        for (i in startIndex until endIndex) {
            when (s[i]) {
                ' ', '\t' -> {}
                else -> return i
            }
        }
        return endIndex
    }

    fun skipSpaceTabBackwards(s: CharSequence, startIndex: Int, lastIndex: Int): Int {
        for (i in startIndex downTo lastIndex) {
            when (s[i]) {
                ' ', '\t' -> {}
                else -> return i
            }
        }
        return lastIndex - 1
    }
}
