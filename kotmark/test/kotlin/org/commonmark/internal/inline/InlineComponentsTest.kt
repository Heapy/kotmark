package org.commonmark.internal.inline

import org.commonmark.internal.Delimiter
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Link
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.Scanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class InlineComponentsTest {
    @Test
    fun autolinks() {
        val uri = assertIs<Link>(parse(AutolinkInlineParser(), "<https://example.com/a>").node)
        assertEquals("https://example.com/a", uri.destination)
        assertEquals("https://example.com/a", assertIs<Text>(uri.firstChild).literal)

        val email = assertIs<Link>(parse(AutolinkInlineParser(), "<me@example.com>").node)
        assertEquals("mailto:me@example.com", email.destination)

        assertNull(tryParse(AutolinkInlineParser(), "<not a link>"))
    }

    @Test
    fun backslashEscapesAndBreaks() {
        assertEquals("*", assertIs<Text>(parse(BackslashInlineParser(), "\\*").node).literal)
        assertEquals("\\", assertIs<Text>(parse(BackslashInlineParser(), "\\a").node).literal)
        assertIs<HardLineBreak>(parse(BackslashInlineParser(), SourceLines.of(
            listOf(SourceLine.of("\\", null), SourceLine.of("", null)),
        )).node)
    }

    @Test
    fun codeSpans() {
        assertEquals(
            "code",
            assertIs<Code>(parse(BackticksInlineParser(), "`code`").node).literal,
        )
        assertEquals(
            "code",
            assertIs<Code>(parse(BackticksInlineParser(), "` code `").node).literal,
        )
        assertEquals(
            "multi line",
            assertIs<Code>(
                parse(
                    BackticksInlineParser(),
                    SourceLines.of(
                        listOf(
                            SourceLine.of("`multi", null),
                            SourceLine.of("line`", null),
                        ),
                    ),
                ).node,
            ).literal,
        )
        assertEquals("``", assertIs<Text>(parse(BackticksInlineParser(), "``x`").node).literal)
    }

    @Test
    fun entities() {
        assertEquals("&", assertIs<Text>(parse(EntityInlineParser(), "&amp;").node).literal)
        assertEquals("A", assertIs<Text>(parse(EntityInlineParser(), "&#65;").node).literal)
        assertEquals("A", assertIs<Text>(parse(EntityInlineParser(), "&#x41;").node).literal)
        assertNull(tryParse(EntityInlineParser(), "&invalid"))
    }

    @Test
    fun inlineHtml() {
        val samples = listOf(
            "<tag attr=\"value\">",
            "</tag>",
            "<?test?>",
            "<!---->",
            "<![CDATA[value]]>",
            "<!DOCTYPE html>",
        )
        for (sample in samples) {
            assertEquals(sample, assertIs<HtmlInline>(parse(HtmlInlineParser(), sample).node).literal)
        }
        assertNull(tryParse(HtmlInlineParser(), "<!FOO>"))
    }

    @Test
    fun emphasisDelimiterProcessing() {
        val parent = Paragraph()
        val opener = Text("*")
        val content = Text("content")
        val closer = Text("*")
        parent.appendChild(opener)
        parent.appendChild(content)
        parent.appendChild(closer)

        val openingRun = Delimiter(mutableListOf(opener), '*', true, false, null)
        val closingRun = Delimiter(mutableListOf(closer), '*', false, true, openingRun)
        assertEquals(1, AsteriskDelimiterProcessor().process(openingRun, closingRun))

        val emphasis = assertIs<Emphasis>(opener.next)
        assertSame(content, emphasis.firstChild)
        assertEquals("*", emphasis.openingDelimiter)
    }

    private fun parse(parser: InlineContentParser, input: String): ParsedInlineImpl =
        parse(parser, SourceLines.of(SourceLine.of(input, null)))

    private fun parse(parser: InlineContentParser, input: SourceLines): ParsedInlineImpl =
        requireNotNull(tryParse(parser, input))

    private fun tryParse(parser: InlineContentParser, input: String): ParsedInlineImpl? =
        tryParse(parser, SourceLines.of(SourceLine.of(input, null)))

    private fun tryParse(parser: InlineContentParser, input: SourceLines): ParsedInlineImpl? {
        val scanner = Scanner.of(input)
        val state = InlineParserState { scanner }
        return parser.tryParse(state) as ParsedInlineImpl?
    }
}
