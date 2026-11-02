// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package org.nibor.autolink

import kotlin.test.*

class LinkIterableTest {
    @Test fun iteratorIsNew() {
        val iterable = getSingleLinkIterable()
        assertEquals(LinkType.URL, iterable.iterator().next().type)
        assertEquals(LinkType.URL, iterable.iterator().next().type)
    }

    @Test fun hasNextOnlyAdvancesOnce() {
        val iterator = getSingleLinkIterable().iterator()
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertNotNull(iterator.next())
        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasNext())
    }

    @Test fun nextThrowsNoSuchElementException() {
        val iterator = getSingleLinkIterable().iterator()
        assertNotNull(iterator.next())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    private fun getSingleLinkIterable(): Iterable<LinkSpan> =
        LinkExtractor.builder().build().extractLinks("foo http://example.com")
}
