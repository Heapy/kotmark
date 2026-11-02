// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package org.nibor.autolink

import kotlin.test.assertEquals

abstract class AutolinkTestCase {
    protected fun assertLinked(extractor: LinkExtractor, type: LinkType, input: String, expected: String) {
        assertEquals(expected, renderLinks(extractor, input, type), "extractLinks: $input")
        assertEquals(expected, renderSpans(extractor, input, type), "extractSpans: $input")
    }

    protected fun assertNotLinked(extractor: LinkExtractor, input: String) {
        assertEquals(input, renderLinks(extractor, input, null), "extractLinks: $input")
        assertEquals(input, renderSpans(extractor, input, null), "extractSpans: $input")
    }

    // Upstream accidentally uses extractSpans in both helpers. Exercise both APIs here.
    private fun renderLinks(extractor: LinkExtractor, input: String, type: LinkType?): String = buildString {
        var index = 0
        for (link in extractor.extractLinks(input)) {
            if (type != null) assertEquals(type, link.type)
            append(input.substring(index, link.beginIndex))
            append('|').append(input.substring(link.beginIndex, link.endIndex)).append('|')
            index = link.endIndex
        }
        append(input.substring(index))
    }

    private fun renderSpans(extractor: LinkExtractor, input: String, type: LinkType?): String = buildString {
        for (span in extractor.extractSpans(input)) {
            if (span is LinkSpan) {
                if (type != null) assertEquals(type, span.type)
                append('|').append(input.substring(span.beginIndex, span.endIndex)).append('|')
            } else {
                append(input.substring(span.beginIndex, span.endIndex))
            }
        }
    }
}
