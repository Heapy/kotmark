// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink.internal

import io.heapy.kotmark.autolink.LinkSpan

internal fun interface Scanner {
    fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan?
}

internal object Scanners {
    fun isAlpha(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z'
    fun isDigit(c: Char): Boolean = c in '0'..'9'
    fun isAlnum(c: Char): Boolean = isAlpha(c) || isDigit(c)
    fun isNonAscii(c: Char): Boolean = c >= '\u0080'

    fun findUrlEnd(input: CharSequence, beginIndex: Int): Int {
        var round = 0
        var square = 0
        var curly = 0
        var singleQuote = false
        var last = -1
        for (i in beginIndex..<input.length) {
            when (input[i]) {
                // Explicit upstream character set, independent of backend Unicode tables.
                in '\u0000'..' ', '"', '<', '>', '`', in '\u007F'..'\u00A0',
                in '\u2000'..'\u200A', '\u2028', '\u2029', '\u202F', '\u205F', '\u3000' -> break
                '?', '!', '.', ',', ':', ';' -> Unit
                '/' -> if (last == i - 1) last = i
                '(' -> round++
                ')' -> { round--; if (round >= 0) last = i else break }
                '[' -> square++
                ']' -> { square--; if (square >= 0) last = i else break }
                '{' -> curly++
                '}' -> { curly--; if (curly >= 0) last = i else break }
                '\'' -> { singleQuote = !singleQuote; if (!singleQuote) last = i }
                else -> last = i
            }
        }
        return last
    }
}
