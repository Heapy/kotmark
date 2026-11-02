package org.commonmark.internal.util

private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
private const val MIN_HIGH_SURROGATE = 0xD800
private const val MIN_LOW_SURROGATE = 0xDC00

internal actual fun codePointToUtf(codePoint: Int): String {
    require(codePoint in 0..0x10FFFF) { "Invalid Unicode code point: $codePoint" }
    if (codePoint <= Char.MAX_VALUE.code) {
        return codePoint.toChar().toString()
    }
    val cp = codePoint - MIN_SUPPLEMENTARY_CODE_POINT
    val high = ((cp ushr 10) + MIN_HIGH_SURROGATE).toChar()
    val low = ((cp and 0x3ff) + MIN_LOW_SURROGATE).toChar()
    return high.toString() + low.toString()
}
