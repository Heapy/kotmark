package io.heapy.kotmark.internal

import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SoftLineBreak
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InlineParserImplTest {
    @Test
    fun parsesCoreInlineSyntax() {
        val paragraph = parse(
            "plain *em* **strong** `code` &amp; <i>x</i> " +
                """[link](/url "title") ![image](/img)""",
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
