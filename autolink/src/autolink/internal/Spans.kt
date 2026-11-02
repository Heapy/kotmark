// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink.internal

import io.heapy.kotmark.autolink.LinkSpan
import io.heapy.kotmark.autolink.LinkType
import io.heapy.kotmark.autolink.Span

internal class SpanImpl(override val beginIndex: Int, override val endIndex: Int) : Span {
    override fun toString(): String = "Span{beginIndex=$beginIndex, endIndex=$endIndex}"
}

internal class LinkSpanImpl(
    override val type: LinkType,
    override val beginIndex: Int,
    override val endIndex: Int,
) : LinkSpan {
    override fun toString(): String = "Link{type=$type, beginIndex=$beginIndex, endIndex=$endIndex}"
}
