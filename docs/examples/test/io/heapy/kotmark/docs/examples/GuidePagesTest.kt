package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.guides.NoteBlockParser
import io.heapy.kotmark.docs.examples.guides.baseUrlRenderer
import io.heapy.kotmark.docs.examples.guides.renderConfigured
import io.heapy.kotmark.docs.examples.guides.routingRenderer
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Inputs and outputs mirror the examples shown on the site's guide pages. */
class GuidePagesTest {
    @Test
    fun configuration() {
        assertEquals(
            "<p>Hello <strong>Kotlin</strong> and <del>builders</del>!<br />\nNext line</p>\n",
            renderConfigured("Hello **Kotlin** and ~~builders~~!\nNext line"),
        )
    }

    @Test
    fun baseUrl() {
        assertEquals(
            "<p><img src=\"https://example.org/docs/images/example.png\" alt=\"Example\" /> " +
                "<a rel=\"nofollow\" href=\"#top\">Top</a></p>\n",
            baseUrlRenderer.render(parser().parse("![Example](../images/example.png) [Top](#top)")),
        )
    }

    @Test
    fun urlRouting() {
        assertEquals(
            "<p><a href=\"https://example.org/docs/guide.html\">Guide</a> " +
                "<img src=\"https://cdn.example.org/assets/logo.png\" alt=\"Logo\" /></p>\n",
            routingRenderer.render(parser().parse("[Guide](guide.md) ![Logo](logo.png)")),
        )
    }

    @Test
    fun blockContinuationParserCreatesItsNode() {
        assertIs<io.heapy.kotmark.docs.examples.guides.NoteBlock>(NoteBlockParser().block)
    }
}
