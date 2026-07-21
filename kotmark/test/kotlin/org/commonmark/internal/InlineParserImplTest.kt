package org.commonmark.internal

import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InlineParserImplTest {
    @Test
    fun parsesCoreInlineSyntax() {
        val paragraph = parse(
            "plain *em* **strong** `code` &amp; <i>x</i> " +
                "[link](/url \"title\") ![image](/img)",
        )

        assertEquals("plain ", assertIs<Text>(paragraph.firstChild).literal)
        val emphasis = find<Emphasis>(paragraph)
        assertEquals("em", assertIs<Text>(emphasis.firstChild).literal)
        val strong = find<StrongEmphasis>(paragraph)
        assertEquals("strong", assertIs<Text>(strong.firstChild).literal)
        assertEquals("code", find<Code>(paragraph).literal)
        assertEquals("<i>", find<HtmlInline>(paragraph).literal)

        val link = find<Link>(paragraph)
        assertEquals("/url", link.destination)
        assertEquals("title", link.title)
        assertEquals("link", assertIs<Text>(link.firstChild).literal)

        val image = find<Image>(paragraph)
        assertEquals("/img", image.destination)
        assertEquals("image", assertIs<Text>(image.firstChild).literal)
    }

    @Test
    fun parsesLineBreaks() {
        val lines = SourceLines.of(
            listOf(
                SourceLine.of("soft", null),
                SourceLine.of("hard  ", null),
                SourceLine.of("done", null),
            ),
        )
        val paragraph = parse(lines)
        val soft = assertIs<SoftLineBreak>(paragraph.firstChild?.next)
        val hardText = assertIs<Text>(soft.next)
        assertEquals("hard", hardText.literal)
        assertIs<HardLineBreak>(hardText.next)
    }

    @Test
    fun disallowsNestedLinks() {
        val paragraph = parse("[outer [inner](/in)](/out)")
        val inner = find<Link>(paragraph)
        assertEquals("/in", inner.destination)
        assertNull(findOrNull<Link>(inner.next))
    }

    private fun parse(input: String): Paragraph =
        parse(SourceLines.of(SourceLine.of(input, null)))

    private fun parse(lines: SourceLines): Paragraph {
        val paragraph = Paragraph()
        InlineParserImpl(
            InlineParserContextImpl(
                emptyList(),
                emptyList(),
                emptyList(),
                emptySet(),
                Definitions(),
            ),
        ).parse(lines, paragraph)
        return paragraph
    }

    private inline fun <reified T : Node> find(root: Node): T =
        requireNotNull(findOrNull(root))

    private inline fun <reified T : Node> findOrNull(root: Node?): T? {
        val pending = mutableListOf<Node>()
        var node = root?.firstChild
        while (node != null) {
            if (node is T) return node
            node.firstChild?.let(pending::add)
            node = node.next ?: pending.removeLastOrNull()
        }
        return null
    }
}
