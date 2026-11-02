package org.nibor.autolink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkExtractorTest {
    @Test
    fun emptyInputAndDisabledTypes() {
        val extractor = LinkExtractor.builder().build()
        assertFalse(extractor.extractLinks("").iterator().hasNext())
        assertFalse(extractor.extractSpans("").iterator().hasNext())
        assertFailsWith<NoSuchElementException> { extractor.extractLinks("").iterator().next() }
        assertFailsWith<NoSuchElementException> { extractor.extractSpans("").iterator().next() }
        val disabled = LinkExtractor.builder().linkTypes(emptySet()).build()
        val input = "http://example.com www.example.com foo@example.com"
        assertTrue(disabled.extractLinks(input).none())
        val span = disabled.extractSpans(input).single()
        assertEquals(0, span.beginIndex)
        assertEquals(input.length, span.endIndex)
    }

    @Test
    fun configurationAndIteratorsAreIndependent() {
        val types = mutableSetOf(LinkType.URL)
        val builder = LinkExtractor.builder().linkTypes(types)
        types.clear()
        val extractor = builder.build()
        builder.linkTypes(setOf(LinkType.EMAIL))
        val links = extractor.extractLinks("http://one.org http://two.org a@example.com")
        val first = links.iterator()
        val second = links.iterator()
        assertEquals(0, first.next().beginIndex)
        assertEquals(15, first.next().beginIndex)
        assertEquals(0, second.next().beginIndex)
        assertFalse(first.hasNext())
        assertTrue(second.hasNext())
    }

    @Test
    fun utf16IndicesAndSurrogates() {
        val extractor = LinkExtractor.builder().build()
        for (suffix in listOf("\uD83D\uDE00", "\uD800", "\uDC00")) {
            val url = "https://example.com/$suffix"
            val input = "\uD83D\uDE00 $url tail"
            val link = extractor.extractLinks(input).single()
            assertEquals(3, link.beginIndex)
            assertEquals(3 + url.length, link.endIndex)
            assertEquals(url, input.substring(link.beginIndex, link.endIndex))
            assertEquals(listOf("\uD83D\uDE00 ", url, " tail"),
                extractor.extractSpans(input).map { input.substring(it.beginIndex, it.endIndex) })
            val email = "$suffix@example.com"
            assertEquals(email.length, extractor.extractLinks(email).single().endIndex)
        }
    }

    @Test
    fun uppercaseWwwIsNotAnUpstreamLink() {
        val extractor = LinkExtractor.builder().build()
        assertTrue(extractor.extractLinks("WWW.example.com wWw.example.com").none())
    }
}
