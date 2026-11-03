package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.internal.Delimiter
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.Scanner
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
    fun autolinkEmailDomainLabels() {
        val emails = listOf(
            "a@a",
            "A1@Z9",
            "a@1.a-b.C9",
            "a@" + "a".repeat(63),
            "a@a" + "-".repeat(61) + "b",
            "a@" + "a".repeat(63) + "." + "b".repeat(63),
        )
        for (email in emails) {
            val link = assertIs<Link>(parse(AutolinkInlineParser(), "<$email>").node, email)
            assertEquals("mailto:$email", link.destination, email)
            assertEquals(email, assertIs<Text>(link.firstChild).literal, email)
        }
    }

    @Test
    fun autolinkEmailRejectsInvalidDomainsAndLocalParts() {
        val emails = listOf(
            "a@" + "a".repeat(64),
            "a@x." + "a".repeat(64),
            "a@",
            "@a",
            "a@.x",
            "a@x.",
            "a@x..y",
            "a@-x",
            "a@x-",
            "a@x.-y",
            "a@x.y-",
            "a@x_y",
            "a@é",
            "é@a",
            "a b@x",
            "a@b@c",
        )
        for (email in emails) {
            assertNull(tryParse(AutolinkInlineParser(), "<$email>"), email)
        }
    }

    @Test
    fun autolinkEmailLocalPartUsesCommonMarkGrammar() {
        for (email in listOf(""".!#$%&'*+/=?^_`{|}~-..@a""", "a..b.@x")) {
            val link = assertIs<Link>(parse(AutolinkInlineParser(), "<$email>").node)
            assertEquals("mailto:$email", link.destination)
        }
        val uri = assertIs<Link>(parse(AutolinkInlineParser(), "<https://a@b>").node)
        assertEquals("https://a@b", uri.destination)
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
            """<tag attr="value">""",
            "</tag>",
            "<?test?>",
            "<!---->",
            "<![CDATA[value]]>",
            "<!DOCTYPE html>",
        )
        for (sample in samples) {
            assertEquals(sample, assertIs<HtmlInline>(parse(HtmlInlineParser(), sample).node).literal)
        }
        assertNull(tryParse(HtmlInlineParser(), "<!1>"))
    }

    @Test
    fun processingInstructionConsumesConsecutiveQuestionMarksAsOneNode() {
        val source = "<?? <?php ?>"
        val inline = assertIs<HtmlInline>(parse(HtmlInlineParser(), source).node)
        assertEquals(source, inline.literal)
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
