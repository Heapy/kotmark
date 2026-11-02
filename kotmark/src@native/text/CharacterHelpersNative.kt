package io.heapy.kotmark.text

import io.heapy.kotmark.internal.util.UnicodeCategories

private const val MIN_HIGH_SURROGATE = 0xD800
private const val MAX_HIGH_SURROGATE = 0xDBFF
private const val MIN_LOW_SURROGATE = 0xDC00
private const val MAX_LOW_SURROGATE = 0xDFFF
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000

internal actual fun codePointAt(s: CharSequence, index: Int): Int {
    val c = s[index]
    if (c.code in MIN_HIGH_SURROGATE..MAX_HIGH_SURROGATE && index + 1 < s.length) {
        val c2 = s[index + 1]
        if (c2.code in MIN_LOW_SURROGATE..MAX_LOW_SURROGATE) {
            return ((c.code shl 10) + c2.code) - ((MIN_HIGH_SURROGATE shl 10) + MIN_LOW_SURROGATE) + MIN_SUPPLEMENTARY_CODE_POINT
        }
    }
    return c.code
}

internal actual fun isLetter(codePoint: Int): Boolean =
    UnicodeCategories.isLetter(codePoint)

internal actual fun getType(codePoint: Int): Int =
    UnicodeCategories.category(codePoint).toCharacterCategoryType()

internal actual fun toChars(codePoint: Int): String = codePointToString(codePoint)

internal actual fun isSupplementaryCodePoint(codePoint: Int): Boolean =
    codePoint in MIN_SUPPLEMENTARY_CODE_POINT..0x10FFFF

private fun codePointToString(codePoint: Int): String {
    if (codePoint < 0) return ""
    if (codePoint <= Char.MAX_VALUE.code) {
        return codePoint.toChar().toString()
    }
    if (codePoint > 0x10FFFF) return ""
    val cp = codePoint - MIN_SUPPLEMENTARY_CODE_POINT
    val high = ((cp ushr 10) + MIN_HIGH_SURROGATE).toChar()
    val low = ((cp and 0x3ff) + MIN_LOW_SURROGATE).toChar()
    return high.toString() + low.toString()
}

private fun CharCategory.toCharacterCategoryType(): Int = when (this) {
    CharCategory.SPACE_SEPARATOR -> CharacterCategory.SPACE_SEPARATOR
    CharCategory.DASH_PUNCTUATION -> CharacterCategory.DASH_PUNCTUATION
    CharCategory.START_PUNCTUATION -> CharacterCategory.START_PUNCTUATION
    CharCategory.END_PUNCTUATION -> CharacterCategory.END_PUNCTUATION
    CharCategory.CONNECTOR_PUNCTUATION -> CharacterCategory.CONNECTOR_PUNCTUATION
    CharCategory.OTHER_PUNCTUATION -> CharacterCategory.OTHER_PUNCTUATION
    CharCategory.MATH_SYMBOL -> CharacterCategory.MATH_SYMBOL
    CharCategory.CURRENCY_SYMBOL -> CharacterCategory.CURRENCY_SYMBOL
    CharCategory.MODIFIER_SYMBOL -> CharacterCategory.MODIFIER_SYMBOL
    CharCategory.OTHER_SYMBOL -> CharacterCategory.OTHER_SYMBOL
    CharCategory.INITIAL_QUOTE_PUNCTUATION -> CharacterCategory.INITIAL_QUOTE_PUNCTUATION
    CharCategory.FINAL_QUOTE_PUNCTUATION -> CharacterCategory.FINAL_QUOTE_PUNCTUATION
    else -> 0
}
