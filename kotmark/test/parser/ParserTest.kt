package io.heapy.kotmark.parser

import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.IndentedCodeBlock
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.node.ThematicBreak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun parsesBlocksAndInlinesEndToEnd() {
        val document =
            assertIs<Document>(
                parser().parse(
                    """
                    # Heading *one*

                    > quoted **text**

                    - first
                    - second

                    ``` kotlin
                    val answer = 42
                    ```

                        indented

                    ---
                    """.trimIndent(),
                ),
            )

        val heading = assertIs<Heading>(document.firstChild)
        assertEquals(1, heading.level)
        assertEquals("Heading ", assertIs<Text>(heading.firstChild).literal)
        assertEquals("one", textOf(assertIs<Emphasis>(heading.lastChild)))

        val quote = assertIs<BlockQuote>(heading.next)
        val quotedParagraph = assertIs<Paragraph>(quote.firstChild)
        assertEquals("quoted ", assertIs<Text>(quotedParagraph.firstChild).literal)
        assertEquals("text", textOf(assertIs<StrongEmphasis>(quotedParagraph.lastChild)))

        val list = assertIs<BulletList>(quote.next)
        assertEquals("first", firstText(assertIs<ListItem>(list.firstChild)))
        assertEquals("second", firstText(assertIs<ListItem>(list.lastChild)))

        val fenced = assertIs<FencedCodeBlock>(list.next)
        assertEquals("kotlin", fenced.info)
        assertEquals("val answer = 42\n", fenced.literal)

        val indented = assertIs<IndentedCodeBlock>(fenced.next)
        assertEquals("indented\n", indented.literal)
        assertIs<ThematicBreak>(indented.next)
    }

    @Test
    fun resolvesReferenceLinksAndKeepsDefinitionNode() {
        val document =
            parser().parse(
                """
                [label]: /destination "title"

                Use [label].
                """.trimIndent(),
            )

        val definition = assertIs<io.heapy.kotmark.node.LinkReferenceDefinition>(document.firstChild)
        assertEquals("label", definition.label)
        val paragraph = assertIs<Paragraph>(definition.next)
        val link = assertIs<Link>(paragraph.firstChild?.next)
        assertEquals("/destination", link.destination)
        assertEquals("title", link.title)
        assertEquals("label", textOf(link))
    }

    @Test
    fun handlesCrLfNullReplacementAndSourceSpans() {
        val document =
            parser { includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES }
                .parse("# a\r\n\r\nb\u0000c")

        val heading = assertIs<Heading>(document.firstChild)
        assertEquals(
            io.heapy.kotmark.node.SourceSpan.of(0, 0, 0, 3),
            heading.getSourceSpans().single(),
        )
        assertEquals(
            io.heapy.kotmark.node.SourceSpan.of(0, 2, 2, 1),
            assertIs<Text>(heading.firstChild).getSourceSpans().single(),
        )

        val paragraph = assertIs<Paragraph>(heading.next)
        assertEquals("b\uFFFDc", assertIs<Text>(paragraph.firstChild).literal)
        assertEquals(
            io.heapy.kotmark.node.SourceSpan.of(2, 0, 7, 3),
            paragraph.getSourceSpans().single(),
        )
    }

    @Test
    fun enabledBlockTypesCanDisableCoreSyntax() {
        val headingOnly =
            parser { enabledBlockTypes = setOf(Heading::class) }
                .parse("# heading\n\n---")
        assertIs<Heading>(headingOnly.firstChild)
        assertIs<Paragraph>(headingOnly.lastChild)

        val none =
            parser { enabledBlockTypes = emptySet() }
                .parse("# heading")
        assertIs<Paragraph>(none.firstChild)

        assertFailsWith<IllegalArgumentException> {
            parser { enabledBlockTypes = setOf(BulletList::class) }
        }
    }

    @Test
    fun parsesNestedListIndentation() {
        val document =
            parser().parse(
                " - 1 space\n   - 3 spaces\n     - 5 spaces\n\t - tab + space",
            )

        var list = assertIs<ListBlock>(document.firstChild)
        assertSame(list.firstChild, list.lastChild)
        assertEquals("1 space", firstText(requireNotNull(list.firstChild)))

        list = assertIs<ListBlock>(requireNotNull(list.firstChild).lastChild)
        assertSame(list.firstChild, list.lastChild)
        assertEquals("3 spaces", firstText(requireNotNull(list.firstChild)))

        list = assertIs<ListBlock>(requireNotNull(list.firstChild).lastChild)
        assertEquals("5 spaces", firstText(requireNotNull(list.firstChild)))
        assertEquals("tab + space", firstText(requireNotNull(list.firstChild?.next)))
    }

    @Test
    fun supportsCustomInlineParserAndPostProcessor() {
        val inlineParser = InlineParser { _, node -> node.appendChild(Code("custom")) }
        val replacement = Document()
        val parser =
            parser {
                inlineParserFactory = InlineParserFactory { inlineParser }
                postProcessor { replacement }
            }

        val parsed = parser.parse("input")
        assertSame(replacement, parsed)
    }

    @Test
    fun validatesMaximumOpenBlocks() {
        assertFailsWith<IllegalArgumentException> {
            parser { maxOpenBlockParsers = -1 }
        }

        val document =
            parser { maxOpenBlockParsers = 0 }
                .parse("# plain")
        assertIs<Paragraph>(document.firstChild)
        assertNull(document.firstChild?.next)
    }

    @Test
    fun defaultMaximumOpenBlocksLimitsNesting() {
        fun assertNesting(parser: Parser, sourceDepth: Int, expectedDepth: Int) {
            var node = parser.parse("> ".repeat(sourceDepth) + "text").firstChild
            repeat(expectedDepth) {
                val quote = assertIs<BlockQuote>(node)
                assertNull(quote.next)
                node = quote.firstChild
            }
            val paragraph = assertIs<Paragraph>(node)
            assertNull(paragraph.next)
            assertEquals("> ".repeat(sourceDepth - expectedDepth) + "text", textOf(paragraph))
        }

        val defaultParser = parser { assertEquals(100, maxOpenBlockParsers) }
        assertNesting(defaultParser, sourceDepth = 3, expectedDepth = 3)
        assertNesting(defaultParser, sourceDepth = 99, expectedDepth = 99)
        assertNesting(defaultParser, sourceDepth = 100, expectedDepth = 100)
        assertNesting(defaultParser, sourceDepth = 101, expectedDepth = 100)
        assertNesting(parser { maxOpenBlockParsers = Int.MAX_VALUE }, sourceDepth = 101, expectedDepth = 101)
    }

    @Test
    fun deeplyNestedInlineDoesNotOverflowParsing() {
        val count = 50_000
        var node = parser().parse("*".repeat(count) + "x" + "*".repeat(count))
        var nesting = 0
        // Inspect iteratively: rendering or visiting this deep tree recursively is out of scope.
        while (true) {
            node = node.firstChild ?: break
            nesting++
        }
        assertTrue(nesting > 1_000)
        assertEquals("x", assertIs<Text>(node).literal)
    }

    private fun textOf(root: Node): String {
        val result = StringBuilder()
        fun appendText(node: Node?) {
            var current = node
            while (current != null) {
                if (current is Text) {
                    result.append(current.literal)
                }
                appendText(current.firstChild)
                current = current.next
            }
        }
        appendText(root.firstChild)
        return result.toString()
    }

    private fun firstText(root: Node): String {
        var node = root
        while (node !is Text) {
            node = requireNotNull(node.firstChild)
        }
        return requireNotNull(node.literal)
    }
}
