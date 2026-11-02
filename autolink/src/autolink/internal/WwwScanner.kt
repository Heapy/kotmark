// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink.internal

import io.heapy.kotmark.autolink.LinkSpan
import io.heapy.kotmark.autolink.LinkType

internal class WwwScanner : Scanner {
    override fun scan(input: CharSequence, triggerIndex: Int, rewindIndex: Int): LinkSpan? {
        val afterDot = triggerIndex + 4
        if (afterDot >= input.length || input[triggerIndex + 1] != 'w' ||
            input[triggerIndex + 2] != 'w' || input[triggerIndex + 3] != '.') return null
        if (triggerIndex != rewindIndex) {
            val before = input[triggerIndex - 1]
            if (before == '.' || Scanners.isAlnum(before)) return null
        }
        val last = Scanners.findUrlEnd(input, afterDot)
        if (last == -1) return null
        for (i in last - 1 downTo afterDot + 1) {
            if (input[i] == '.') return LinkSpanImpl(LinkType.WWW, triggerIndex, last + 1)
        }
        return null
    }
}
