// Port of autolink-java 0.12.0. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

import kotlin.jvm.JvmStatic

import io.heapy.kotmark.autolink.internal.EmailScanner
import io.heapy.kotmark.autolink.internal.Scanner
import io.heapy.kotmark.autolink.internal.SpanImpl
import io.heapy.kotmark.autolink.internal.UrlScanner
import io.heapy.kotmark.autolink.internal.WwwScanner

/** Immutable, reusable extractor. Each iterable creates independent, lazy iterators. */
class LinkExtractor private constructor(
    private val urlScanner: Scanner?,
    private val wwwScanner: Scanner?,
    private val emailScanner: Scanner?,
) {
    fun extractLinks(input: CharSequence): Iterable<LinkSpan> = Iterable { LinkIterator(input) }

    fun extractSpans(input: CharSequence): Iterable<Span> = Iterable { SpanIterator(input, LinkIterator(input)) }

    class Builder internal constructor() {
        private var linkTypes = LinkType.entries.toSet()
        private var emailDomainMustHaveDot = true

        fun linkTypes(linkTypes: Set<LinkType>): Builder = apply { this.linkTypes = linkTypes.toSet() }

        fun emailDomainMustHaveDot(emailDomainMustHaveDot: Boolean): Builder = apply {
            this.emailDomainMustHaveDot = emailDomainMustHaveDot
        }

        fun build(): LinkExtractor = LinkExtractor(
            if (LinkType.URL in linkTypes) UrlScanner() else null,
            if (LinkType.WWW in linkTypes) WwwScanner() else null,
            if (LinkType.EMAIL in linkTypes) EmailScanner(emailDomainMustHaveDot) else null,
        )
    }

    private inner class LinkIterator(private val input: CharSequence) : Iterator<LinkSpan> {
        private var nextLink: LinkSpan? = null
        private var index = 0
        private var rewindIndex = 0

        override fun hasNext(): Boolean {
            if (nextLink != null) return true
            while (index < input.length) {
                val scanner = when (input[index]) {
                    ':' -> urlScanner
                    '@' -> emailScanner
                    'w' -> wwwScanner
                    else -> null
                }
                val link = scanner?.scan(input, index, rewindIndex)
                if (link != null) {
                    nextLink = link
                    index = link.endIndex
                    rewindIndex = index
                    return true
                }
                index++
            }
            return false
        }

        override fun next(): LinkSpan {
            if (!hasNext()) throw NoSuchElementException()
            return checkNotNull(nextLink).also { nextLink = null }
        }
    }

    private class SpanIterator(
        private val input: CharSequence,
        private val links: Iterator<LinkSpan>,
    ) : Iterator<Span> {
        private var index = 0
        private var nextLink: LinkSpan? = null

        override fun hasNext(): Boolean = index < input.length

        override fun next(): Span {
            if (!hasNext()) throw NoSuchElementException()
            if (nextLink == null && links.hasNext()) nextLink = links.next()
            val link = nextLink
            if (link == null || index < link.beginIndex) {
                val end = link?.beginIndex ?: input.length
                return SpanImpl(index, end).also { index = end }
            }
            index = link.endIndex
            nextLink = null
            return link
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
