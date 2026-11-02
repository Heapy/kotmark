// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

internal class UrlScanner : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val afterSlashSlash = triggerIndex + 3
        if (afterSlashSlash >= input.length || input[triggerIndex + 1] != '/' || input[triggerIndex + 2] != '/') return null
        val first = findFirst(input, triggerIndex - 1, rewindIndex)
        if (first == -1) return null
        val last = Scanners.findUrlEnd(input, afterSlashSlash)
        return if (last == -1) null else LinkSpanImpl(LinkType.URL, first, last + 1)
    }

    private fun findFirst(input: CharSequence, beginIndex: Int, rewindIndex: Int): Int {
        var first = -1
        var digit = -1
        for (i in beginIndex downTo rewindIndex) {
            val c = input[i]
            when {
                Scanners.isAlpha(c) -> first = i
                Scanners.isDigit(c) -> digit = i
                c != '+' && c != '-' && c != '.' -> break
            }
        }
        // Do not extract abc://foo from 1abc://foo; leading punctuation is allowed.
        return if (first > 0 && first - 1 == digit) -1 else first
    }
}
