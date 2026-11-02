// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink.internal

import io.heapy.kotmark.autolink.LinkSpan
import io.heapy.kotmark.autolink.LinkType

internal class EmailScanner(private val domainMustHaveDot: Boolean) : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val first = findFirst(input, triggerIndex - 1, rewindIndex)
        if (first == -1) return null
        val last = findLast(input, triggerIndex + 1)
        return if (last == -1) null else LinkSpanImpl(LinkType.EMAIL, first, last + 1)
    }

    private fun findFirst(input: CharSequence, beginIndex: Int, rewindIndex: Int): Int {
        var first = -1
        var atomBoundary = true
        for (i in beginIndex downTo rewindIndex) {
            val c = input[i]
            when {
                localAtomAllowed(c) -> { first = i; atomBoundary = false }
                c == '.' -> { if (atomBoundary) break; atomBoundary = true }
                else -> break
            }
        }
        return first
    }

    private fun findLast(input: CharSequence, beginIndex: Int): Int {
        var firstInSubDomain = true
        var canEndSubDomain = false
        var firstDot = -1
        var last = -1
        for (i in beginIndex..<input.length) {
            val c = input[i]
            if (firstInSubDomain) {
                if (!subDomainAllowed(c)) break
                last = i
                firstInSubDomain = false
                canEndSubDomain = true
            } else {
                when {
                    c == '.' -> {
                        if (!canEndSubDomain) break
                        firstInSubDomain = true
                        if (firstDot == -1) firstDot = i
                    }
                    c == '-' -> canEndSubDomain = false
                    subDomainAllowed(c) -> { last = i; canEndSubDomain = true }
                    else -> break
                }
            }
        }
        return if (domainMustHaveDot && (firstDot == -1 || firstDot > last)) -1 else last
    }

    private fun localAtomAllowed(c: Char): Boolean =
        subDomainAllowed(c) || c in "!#$%&'*+-/=?^_`{|}~"

    private fun subDomainAllowed(c: Char): Boolean = Scanners.isAlnum(c) || Scanners.isNonAscii(c)
}
