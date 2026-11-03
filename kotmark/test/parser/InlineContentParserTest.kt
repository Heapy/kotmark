package io.heapy.kotmark.parser

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.test.Nodes
import io.heapy.kotmark.text.asciiMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InlineContentParserTest {
    @Test
    fun backwardSearchInlineContentParser() {
        val parser = parser {
            customInlineContentParserFactory(object : InlineContentParserFactory {
                override val triggerCharacters = setOf('(')
                override fun create(): InlineContentParser = DestinationInlineParser()
            })
        }
        val cases = listOf(
            Triple("img(a-link) more text", "a-link", " more text"),
            Triple("img(a-link-)more-text) more text", "a-link-)more-text", " more text"),
            Triple("img(a-link-)more-text)", "a-link-)more-text", null),
            Triple("img() more text", "", " more text"),
        )
        for ([source, destination, followingText] in cases) {
            val inline = Nodes.find<DestinationInline>(parser.parse(source))
            assertEquals("img", assertIs<Text>(inline.previous).literal)
            assertEquals(destination, inline.destination)
            if (followingText == null) assertNull(inline.next)
            else assertEquals(followingText, assertIs<Text>(inline.next).literal)
        }
        for (source in listOf("img(not-a-link more text", "before)img(not-a-link more text")) {
            val document = parser.parse(source)
            assertNull(Nodes.tryFind<DestinationInline>(document))
            assertEquals(source, assertIs<Text>(document.firstChild?.firstChild).literal)
        }
    }

    private class DestinationInline(val destination: String) : CustomNode()

    /** Uses the last ')' before whitespace or EOF, bounded by the opening parenthesis. */
    private class DestinationInlineParser : InlineContentParser {
        override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
            val scanner = inlineParserState.scanner()
            scanner.next()
            val contentStart = scanner.position()
            val _ = scanner.find(WHITESPACE)
            if (scanner.findPrev(')', contentStart) == -1) return ParsedInline.none()
            val destination = scanner.getSource(contentStart, scanner.position()).content
            scanner.next()
            return ParsedInline.of(DestinationInline(destination), scanner.position())
        }

        companion object {
            private val WHITESPACE = asciiMatcher { anyOf(" \t\n\u000B\u000C\r") }
        }
    }
}
