package io.heapy.kotmark.text

/**
 * Matcher interface for `char` values.
 *
 * Note that because this matches on `char` values only (as opposed to `int` code points),
 * this only operates on the level of code units and doesn't support supplementary characters.
 */
fun interface CharMatcher {
    fun matches(c: Char): Boolean
}
