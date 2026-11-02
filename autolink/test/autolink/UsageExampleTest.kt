// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

import kotlin.test.Test
import kotlin.test.assertEquals

class UsageExampleTest {
    @Test fun linkify() {
        val input = "wow http://test.com such linked"
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).build()
        val html = buildString {
            for (span in extractor.extractSpans(input)) {
                val text = input.substring(span.beginIndex, span.endIndex)
                // Upstream Encode helpers are identities for this example too.
                if (span is LinkSpan) append("""<a href="""").append(text).append("\">").append(text).append("</a>")
                else append(text)
            }
        }
        assertEquals("""wow <a href="http://test.com">http://test.com</a> such linked""", html)
    }

    @Test fun extractLinks() {
        val input = "hey https://test.com and https://example.com"
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).build()
        val links = extractor.extractLinks(input).map { input.substring(it.beginIndex, it.endIndex) }
        assertEquals(listOf("https://test.com", "https://example.com"), links)
    }
}
