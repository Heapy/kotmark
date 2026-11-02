// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
@file:Suppress("DEPRECATION")
package org.nibor.autolink

import kotlin.jvm.JvmStatic

@Deprecated("Use LinkExtractor.extractSpans instead")
fun interface LinkRenderer {
    fun render(link: LinkSpan, input: CharSequence, output: StringBuilder)
}

object Autolink {
    @Deprecated("Use LinkExtractor.extractSpans instead")
    @JvmStatic
    fun renderLinks(input: CharSequence, links: Iterable<LinkSpan>, linkRenderer: LinkRenderer): String = buildString {
        var lastIndex = 0
        for (link in links) {
            append(input, lastIndex, link.beginIndex)
            linkRenderer.render(link, input, this)
            lastIndex = link.endIndex
        }
        if (lastIndex < input.length) append(input, lastIndex, input.length)
    }
}
