// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package org.nibor.autolink

import kotlin.test.*

class SpanIterableTest {
    @Test fun iteratorIsNew() {
        val iterable = extractSpans("test")
        assertEquals(4, iterable.iterator().next().endIndex)
        assertEquals(4, iterable.iterator().next().endIndex)
    }

    @Test fun hasNextOnlyAdvancesOnce() {
        val iterator = extractSpans("test").iterator()
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertNotNull(iterator.next())
        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())
    }

    @Test fun spanAndLinkSequences() {
        assertEquals(listOf("test"), extractSpansAsText("test"))
        assertEquals(listOf("http://example.org"), extractSpansAsText("http://example.org"))
        assertEquals(listOf("test ", "http://example.org"), extractSpansAsText("test http://example.org"))
        assertEquals(listOf("http://example.org", " test"), extractSpansAsText("http://example.org test"))
        assertEquals(listOf("http://example.org", " ", "https://example.com"), extractSpansAsText("http://example.org https://example.com"))
    }

    @Test fun nextThrowsNoSuchElementException() {
        val iterator = extractSpans("test").iterator()
        assertNotNull(iterator.next())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    private fun extractSpans(input: String): Iterable<Span> = LinkExtractor.builder().build().extractSpans(input)
    private fun extractSpansAsText(input: String): List<String> =
        extractSpans(input).map { input.substring(it.beginIndex, it.endIndex) }
}
