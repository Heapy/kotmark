package io.heapy.kotmark.text

private val letterRegex = js("new RegExp('^\\\\p{L}+$', 'u')")
private val spaceSeparatorRegex = js("new RegExp('^\\\\p{Zs}+$', 'u')")
private val dashPunctRegex = js("new RegExp('^\\\\p{Pd}$', 'u')")
private val startPunctRegex = js("new RegExp('^\\\\p{Ps}$', 'u')")
private val endPunctRegex = js("new RegExp('^\\\\p{Pe}$', 'u')")
private val connectorPunctRegex = js("new RegExp('^\\\\p{Pc}$', 'u')")
private val otherPunctRegex = js("new RegExp('^\\\\p{Po}$', 'u')")
private val mathSymbolRegex = js("new RegExp('^\\\\p{Sm}$', 'u')")
private val currencySymbolRegex = js("new RegExp('^\\\\p{Sc}$', 'u')")
private val modifierSymbolRegex = js("new RegExp('^\\\\p{Sk}$', 'u')")
private val otherSymbolRegex = js("new RegExp('^\\\\p{So}$', 'u')")
private val initialQuoteRegex = js("new RegExp('^\\\\p{Pi}$', 'u')")
private val finalQuoteRegex = js("new RegExp('^\\\\p{Pf}$', 'u')")

private fun codePointToString(codePoint: Int): String =
    js("String.fromCodePoint(codePoint)").unsafeCast<String>()

private fun regexTest(regex: dynamic, input: String): Boolean =
    regex.test(input).unsafeCast<Boolean>()

internal actual fun codePointAt(s: CharSequence, index: Int): Int {
    val str = s.toString()
    val cp = js("str.codePointAt(index)")
    return cp?.unsafeCast<Int>() ?: s[index].code
}

internal actual fun isLetter(codePoint: Int): Boolean {
    val str = codePointToString(codePoint)
    return regexTest(letterRegex, str)
}

internal actual fun getType(codePoint: Int): Int {
    val str = codePointToString(codePoint)
    return when {
        regexTest(spaceSeparatorRegex, str) -> CharacterCategory.SPACE_SEPARATOR
        regexTest(dashPunctRegex, str) -> CharacterCategory.DASH_PUNCTUATION
        regexTest(startPunctRegex, str) -> CharacterCategory.START_PUNCTUATION
        regexTest(endPunctRegex, str) -> CharacterCategory.END_PUNCTUATION
        regexTest(connectorPunctRegex, str) -> CharacterCategory.CONNECTOR_PUNCTUATION
        regexTest(otherPunctRegex, str) -> CharacterCategory.OTHER_PUNCTUATION
        regexTest(mathSymbolRegex, str) -> CharacterCategory.MATH_SYMBOL
        regexTest(currencySymbolRegex, str) -> CharacterCategory.CURRENCY_SYMBOL
        regexTest(modifierSymbolRegex, str) -> CharacterCategory.MODIFIER_SYMBOL
        regexTest(otherSymbolRegex, str) -> CharacterCategory.OTHER_SYMBOL
        regexTest(initialQuoteRegex, str) -> CharacterCategory.INITIAL_QUOTE_PUNCTUATION
        regexTest(finalQuoteRegex, str) -> CharacterCategory.FINAL_QUOTE_PUNCTUATION
        else -> 0
    }
}

internal actual fun toChars(codePoint: Int): String = codePointToString(codePoint)

internal actual fun isSupplementaryCodePoint(codePoint: Int): Boolean =
    codePoint >= 0x10000 && codePoint <= 0x10FFFF
