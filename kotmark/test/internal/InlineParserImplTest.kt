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
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

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
    fun mergesTextAcrossNestedChildrenAndSiblingsWithSourceSpans() {
        val prefix = """left\* &amp; """
        val outerPrefix = """outer\! &amp; """
        val inner = """inner\? &lt;"""
        val outerSuffix = """ tail\+"""
        val suffix = """ right\_ &gt;"""
        val source = "$prefix*$outerPrefix**$inner**$outerSuffix*$suffix"
        val paragraph = parse(SourceLines.of(SourceLine.of(source, SourceSpan.of(0, 0, 0, source.length))))

        fun assertMergedText(node: Node?, expected: String, raw: String): Text {
            val text = assertIs<Text>(node)
            assertEquals(expected, text.literal)
            val offset = source.indexOf(raw)
            assertEquals(listOf(SourceSpan.of(0, offset, offset, raw.length)), text.getSourceSpans())
            return text
        }

        val first = assertMergedText(paragraph.firstChild, "left* & ", prefix)
        val emphasis = assertIs<Emphasis>(first.next)
        val leading = assertMergedText(emphasis.firstChild, "outer! & ", outerPrefix)
        val strong = assertIs<StrongEmphasis>(leading.next)
        val innerText = assertMergedText(strong.firstChild, "inner? <", inner)
        assertSame(innerText, strong.lastChild)
        val trailing = assertMergedText(strong.next, " tail+", outerSuffix)
        assertSame(trailing, emphasis.lastChild)
        assertNull(trailing.next)
        val last = assertMergedText(emphasis.next, " right_ >", suffix)
        assertSame(last, paragraph.lastChild)
        assertNull(last.next)
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
                100,
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
