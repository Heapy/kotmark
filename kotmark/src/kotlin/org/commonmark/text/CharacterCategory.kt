package org.commonmark.text

/**
 * Unicode general category constants, matching the values used by `java.lang.Character`
 * (and the common subset needed by this library).
 */
object CharacterCategory {
    const val SPACE_SEPARATOR = 12
    const val DASH_PUNCTUATION = 20
    const val START_PUNCTUATION = 21
    const val END_PUNCTUATION = 22
    const val CONNECTOR_PUNCTUATION = 23
    const val OTHER_PUNCTUATION = 24
    const val MATH_SYMBOL = 25
    const val CURRENCY_SYMBOL = 26
    const val MODIFIER_SYMBOL = 27
    const val OTHER_SYMBOL = 28
    const val INITIAL_QUOTE_PUNCTUATION = 29
    const val FINAL_QUOTE_PUNCTUATION = 30
}

/**
 * Character helpers that are implemented per-platform.
 */
internal expect fun codePointAt(s: CharSequence, index: Int): Int

internal expect fun isLetter(codePoint: Int): Boolean

internal expect fun getType(codePoint: Int): Int

internal expect fun toChars(codePoint: Int): String

internal expect fun isSupplementaryCodePoint(codePoint: Int): Boolean
