// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package org.nibor.autolink.internal

import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType
import org.nibor.autolink.Span

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
