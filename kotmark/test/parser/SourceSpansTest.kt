package io.heapy.kotmark.parser

import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.HtmlBlock
import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.node.ThematicBreak
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceSpansTest {
    private val blocksParser =
        parser { includeSourceSpans = IncludeSourceSpans.BLOCKS }
    private val inlinesParser =
        parser { includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES }

    @Test
    fun blockSpans() {
        assertBlockSpans<Paragraph>(
            "foo\nbar\n",
            span(0, 0, 0, 3),
            span(1, 0, 4, 3),
        )
        assertBlockSpans<Paragraph>(
            "> foo\n> bar\n",
            span(0, 2, 2, 3),
            span(1, 2, 8, 3),
        )
        assertBlockSpans<ThematicBreak>("  ---\n", span(0, 0, 0, 5))
        assertBlockSpans<Heading>("> # foo", span(0, 2, 2, 5))
        assertBlockSpans<Heading>(
            "foo\nbar\n====\n",
            span(0, 0, 0, 3),
            span(1, 0, 4, 3),
            span(2, 0, 8, 4),
        )
        assertBlockSpans<IndentedCodeBlock>(
            "    foo\n\tbar\n",
            span(0, 0, 0, 7),
            span(1, 0, 8, 4),
        )
        assertBlockSpans<FencedCodeBlock>(
            "* ```\n  foo\n  ```\n",
            span(0, 2, 2, 3),
            span(1, 2, 8, 3),
            span(2, 2, 14, 3),
        )
        assertBlockSpans<HtmlBlock>(
            " <div>\n foo\n </div>\n",
            span(0, 0, 0, 6),
            span(1, 0, 7, 4),
            span(2, 0, 12, 7),
        )
        assertBlockSpans<BlockQuote>(
            "> foo\nbar\n> baz\n",
            span(0, 0, 0, 5),
            span(1, 0, 6, 3),
            span(2, 0, 10, 5),
        )
        assertBlockSpans<ListBlock>(
            "* foo\nbar\n* baz",
            span(0, 0, 0, 5),
            span(1, 0, 6, 3),
            span(2, 0, 10, 5),
        )
        assertBlockSpans<ListItem>(
            "*\n  foo\n  bar\n",
            span(0, 0, 0, 1),
            span(1, 0, 2, 5),
            span(2, 0, 8, 5),
        )
    }

    @Test
    fun referenceDefinitionAndReplacementSpans() {
        val document = blocksParser.parse("[foo]: /url\nHeading\n===\n")
        assertEquals(
            listOf(span(0, 0, 0, 11)),
            find<LinkReferenceDefinition>(document).getSourceSpans(),
        )
        assertEquals(
            listOf(span(1, 0, 12, 7), span(2, 0, 20, 3)),
            find<Heading>(document).getSourceSpans(),
        )
    }

    @Test
    fun unclosedReferenceTitleRemainsAParagraphWithItsOwnSpans() {
        for (parser in listOf(blocksParser, inlinesParser)) {
            val document = parser.parse("[foo]: /url\n(partial title\n\n[foo]")
            val definition = find<LinkReferenceDefinition>(document)
            assertEquals(null, definition.title)
            assertEquals(listOf(span(0, 0, 0, 11)), definition.getSourceSpans())
            assertEquals(listOf(span(1, 0, 12, 14)), find<Paragraph>(document).getSourceSpans())
            assertEquals(
                "<p>(partial title</p>\n<p><a href=\"/url\">foo</a></p>\n",
                htmlRenderer().render(document),
            )
        }
    }

    @Test
    fun replacingAllParagraphLinesPreservesReferenceSourceSpans() {
        for (replaceActive in listOf(false, true)) {
            val replacementParser = parser {
                includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
                customBlockParserFactory { state, _ ->
                    if (state.line.content.toString() == "%%%") {
                        val replacement = object : AbstractBlockParser() {
                            override val block = ThematicBreak()
                            override fun tryContinue(parserState: ParserState): BlockContinue? = null
                        }
                        val start = BlockStart.of(replacement).atIndex(3)
                        if (replaceActive) start.replaceActiveBlockParser() else start.replaceParagraphLines(Int.MAX_VALUE)
                    } else {
                        null
                    }
                }
            }
            val document = replacementParser.parse("[foo]: /url\n(partial title\n%%%")
            val definition = find<LinkReferenceDefinition>(document)
            assertEquals(null, definition.title)
            assertEquals(listOf(span(0, 0, 0, 11)), definition.getSourceSpans())
            assertEquals(
                listOf(span(1, 0, 12, 14), span(2, 0, 27, 3)),
                find<ThematicBreak>(document).getSourceSpans(),
            )
        }
    }

    @Test
    fun lazyContinuationSpansPropagateThroughNestedContainers() {
        val document = blocksParser.parse("> > > foo\nbar\n")
        val outer = find<BlockQuote>(document)
        val middle = find<BlockQuote>(requireNotNull(outer.firstChild))
        val inner = find<BlockQuote>(requireNotNull(middle.firstChild))
        val paragraph = find<Paragraph>(requireNotNull(inner.firstChild))

        assertEquals(
            listOf(span(0, 0, 0, 9), span(1, 0, 10, 3)),
            outer.getSourceSpans(),
        )
        assertEquals(
            listOf(span(0, 2, 2, 7), span(1, 0, 10, 3)),
            middle.getSourceSpans(),
        )
        assertEquals(
            listOf(span(0, 4, 4, 5), span(1, 0, 10, 3)),
            inner.getSourceSpans(),
        )
        assertEquals(
            listOf(span(0, 6, 6, 3), span(1, 0, 10, 3)),
            paragraph.getSourceSpans(),
        )
    }

    @Test
    fun inlineSpans() {
        assertInlineSpans<Text>("> foo`bar", span(0, 2, 2, 7))
        assertInlineSpans<Text>("[foo](/url)", span(0, 1, 1, 3))
        assertInlineSpans<Link>("see <https://example.org>", span(0, 4, 4, 21))
        assertInlineSpans<Text>("\\!", span(0, 0, 0, 2))
        assertInlineSpans<Code>(
            "`multi\nline`",
            span(0, 0, 0, 6),
            span(1, 0, 7, 5),
        )
        assertInlineSpans<HtmlInline>(
            "hi <strong>there</strong>",
            span(0, 3, 3, 8),
        )
        assertInlineSpans<Emphasis>("\n*hey*", span(1, 0, 1, 5))
        assertInlineSpans<StrongEmphasis>("\n**hey**", span(1, 0, 1, 7))
        assertInlineSpans<Text>(">\tfoo", span(0, 2, 2, 3))
    }

    @Test
    fun mixedLineTerminatorsKeepAbsoluteInputOffsets() {
        val input = "foo\nbar\rbaz\r\nqux\r\n\r\n> *hi*"
        assertBlockSpans<Paragraph>(
            input,
            span(0, 0, 0, 3),
            span(1, 0, 4, 3),
            span(2, 0, 8, 3),
            span(3, 0, 13, 3),
        )
        assertInlineSpans<Emphasis>(input, span(5, 2, 22, 4))
    }

    private inline fun <reified T : Node> assertBlockSpans(
        input: String,
        vararg expected: SourceSpan,
    ) {
        assertEquals(expected.toList(), find<T>(blocksParser.parse(input)).getSourceSpans())
    }

    private inline fun <reified T : Node> assertInlineSpans(
        input: String,
        vararg expected: SourceSpan,
    ) {
        assertEquals(expected.toList(), find<T>(inlinesParser.parse(input)).getSourceSpans())
    }

    private inline fun <reified T : Node> find(root: Node): T {
        val pending = mutableListOf(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeAt(0)
            if (node is T) {
                return node
            }
            var child = node.firstChild
            while (child != null) {
                pending += child
                child = child.next
            }
        }
        error("No ${T::class.simpleName} found")
    }

    private fun span(
        line: Int,
        column: Int,
        input: Int,
        length: Int,
    ): SourceSpan = SourceSpan.of(line, column, input, length)
}
