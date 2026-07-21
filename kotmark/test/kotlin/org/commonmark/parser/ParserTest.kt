package org.commonmark.parser

import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ParserTest {
    @Test
    fun parsesBlocksAndInlinesEndToEnd() {
        val document =
            assertIs<Document>(
                Parser.builder().build().parse(
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
            Parser.builder().build().parse(
                """
                [label]: /destination "title"

                Use [label].
                """.trimIndent(),
            )

        val definition = assertIs<org.commonmark.node.LinkReferenceDefinition>(document.firstChild)
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
            Parser.builder()
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
                .parse("# a\r\n\r\nb\u0000c")

        val heading = assertIs<Heading>(document.firstChild)
        assertEquals(
            org.commonmark.node.SourceSpan.of(0, 0, 0, 3),
            heading.getSourceSpans().single(),
        )
        assertEquals(
            org.commonmark.node.SourceSpan.of(0, 2, 2, 1),
            assertIs<Text>(heading.firstChild).getSourceSpans().single(),
        )

        val paragraph = assertIs<Paragraph>(heading.next)
        assertEquals("b\uFFFDc", assertIs<Text>(paragraph.firstChild).literal)
        assertEquals(
            org.commonmark.node.SourceSpan.of(2, 0, 7, 3),
            paragraph.getSourceSpans().single(),
        )
    }

    @Test
    fun enabledBlockTypesCanDisableCoreSyntax() {
        val headingOnly =
            Parser.builder()
                .enabledBlockTypes(setOf(Heading::class))
                .build()
                .parse("# heading\n\n---")
        assertIs<Heading>(headingOnly.firstChild)
        assertIs<Paragraph>(headingOnly.lastChild)

        val none =
            Parser.builder()
                .enabledBlockTypes(emptySet())
                .build()
                .parse("# heading")
        assertIs<Paragraph>(none.firstChild)

        assertFailsWith<IllegalArgumentException> {
            Parser.builder().enabledBlockTypes(setOf(BulletList::class))
        }
    }

    @Test
    fun parsesNestedListIndentation() {
        val document =
            Parser.builder().build().parse(
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
            Parser.builder()
                .inlineParserFactory(InlineParserFactory { inlineParser })
                .postProcessor { replacement }
                .build()

        val parsed = parser.parse("input")
        assertSame(replacement, parsed)
    }

    @Test
    fun validatesMaximumOpenBlocks() {
        assertFailsWith<IllegalArgumentException> {
            Parser.builder().maxOpenBlockParsers(-1)
        }

        val document =
            Parser.builder()
                .maxOpenBlockParsers(0)
                .build()
                .parse("# plain")
        assertIs<Paragraph>(document.firstChild)
        assertNull(document.firstChild?.next)
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
