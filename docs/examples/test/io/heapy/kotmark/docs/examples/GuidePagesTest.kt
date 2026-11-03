package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.guides.NoteBlockParser
import io.heapy.kotmark.docs.examples.guides.baseUrlRenderer
import io.heapy.kotmark.docs.examples.guides.renderConfigured
import io.heapy.kotmark.docs.examples.guides.routingRenderer
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

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

    @Test
    fun blockContinuationReturnsTheDocumentedOutcomes() {
        val parser = NoteBlockParser()
        fun state(content: String, index: Int = 0, column: Int = 0, indent: Int = 0) = object : ParserState {
            override val line = SourceLine.of(content, null)
            override val index = index
            override val column = column
            override val indent = indent
            override val isBlank = content.substring(index).isBlank()
            override val nextNonSpaceIndex = content.indexOfFirst { !it.isWhitespace() }
                .takeIf { it >= 0 } ?: content.length
            override val activeBlockParser = parser
        }
        assertEquals(BlockContinue.Finished, parser.tryContinue(state("END")))
        assertEquals(BlockContinue.AtColumn(4), parser.tryContinue(state("\ttext", indent = 4)))
        assertEquals(BlockContinue.AtIndex(1), parser.tryContinue(state(" \t", index = 1, column = 1, indent = 3)))
        assertNull(parser.tryContinue(state("next paragraph")))
    }
}
