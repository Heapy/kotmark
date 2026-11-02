// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

/** A range in the input, measured in UTF-16 code units, with an exclusive end. */
interface Span {
    val beginIndex: Int
    val endIndex: Int
}

/** A link range; indices refer to the original input without copying its text. */
interface LinkSpan : Span {
    val type: LinkType
}

enum class LinkType { URL, WWW, EMAIL }
