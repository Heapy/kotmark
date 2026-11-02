package org.commonmark.renderer.text

import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class TextContentRendererTest {
    private val parser = Parser.builder().build()
    private val compact = TextContentRenderer.builder().build()
    private val separate =
        TextContentRenderer.builder()
            .lineBreakRendering(LineBreakRendering.SEPARATE_BLOCKS)
            .build()
    private val stripped =
        TextContentRenderer.builder()
            .lineBreakRendering(LineBreakRendering.STRIP)
            .build()

    @Test
    fun textHeadingsEmphasisAndQuotes() {
        assertCompact("foo bar", "foo bar")
        assertStripped("foo bar", "foo bar")
        assertCompact("foo foo\n\nbar\nbar", "foo foo\nbar\nbar")
        assertSeparate("foo foo\n\nbar\nbar", "foo foo\n\nbar\nbar")
        assertStripped("foo foo\n\nbar\nbar", "foo foo bar bar")

        assertCompact("# Heading\n\nFoo", "Heading\nFoo")
        assertSeparate("# Heading\n\nFoo", "Heading\n\nFoo")
        assertStripped("# Heading\n\nFoo", "Heading: Foo")

        assertAll("***foo***", "foo")
        assertCompact("foo\n***foo***\nbar\n\n***bar***", "foo\nfoo\nbar\nbar")
        assertSeparate("foo\n***foo***\nbar\n\n***bar***", "foo\nfoo\nbar\n\nbar")
        assertStripped("foo\n***foo***\nbar\n\n***bar***", "foo foo bar bar")

        val quote = "foo\n>foo\nbar\n\nbar"
        assertCompact(quote, "foo\n«foo\nbar»\nbar")
        assertSeparate(quote, "foo\n\n«foo\nbar»\n\nbar")
        assertStripped(quote, "foo «foo bar» bar")
    }

    @Test
    fun linksImagesAndInlineCode() {
        assertAll(
            "foo [text](http://link \"title\") bar",
            "foo \"text\" (title: http://link) bar",
        )
        assertAll(
            "foo [text](http://link \"http://link\") bar",
            "foo \"text\" (http://link) bar",
        )
        assertAll("foo [text](http://link) bar", "foo \"text\" (http://link) bar")
        assertAll("foo [text]() bar", "foo \"text\" bar")
        assertAll("foo http://link bar", "foo http://link bar")
        assertAll(
            "foo ![text](http://link \"title\") bar",
            "foo \"text\" (title: http://link) bar",
        )
        assertAll("foo ![text](http://link) bar", "foo \"text\" (http://link) bar")
        assertAll("foo ![text]() bar", "foo \"text\" bar")
        assertAll("foo `code` bar", "foo \"code\" bar")
    }

    @Test
    fun lists() {
        assertModes(
            "foo\n* foo\n* bar\n\nbar",
            "foo\n* foo\n* bar\nbar",
            "foo\n\n* foo\n* bar\n\nbar",
            "foo foo bar bar",
        )
        assertModes(
            "foo\n- foo\n- bar\n\nbar",
            "foo\n- foo\n- bar\nbar",
            "foo\n\n- foo\n- bar\n\nbar",
            "foo foo bar bar",
        )
        assertModes(
            "foo\n1. foo\n2. bar\n\nbar",
            "foo\n1. foo\n2. bar\nbar",
            "foo\n\n1. foo\n2. bar\n\nbar",
            "foo 1. foo 2. bar bar",
        )
        assertModes(
            "foo\n0) foo\n1) bar\n\nbar",
            "foo\n0) foo\n1) bar\nbar",
            "foo\n0) foo\n\n1) bar\n\nbar",
            "foo 0) foo 1) bar bar",
        )
        assertModes(
            "bar\n1. foo\n   1. bar\n2. foo",
            "bar\n1. foo\n   1. bar\n2. foo",
            "bar\n\n1. foo\n   1. bar\n2. foo",
            "bar 1. foo 1. bar 2. foo",
        )
        assertModes(
            "bar\n* foo\n  - bar\n* foo",
            "bar\n* foo\n  - bar\n* foo",
            "bar\n\n* foo\n  - bar\n* foo",
            "bar foo bar foo",
        )
        assertModes(
            "bar\n* foo\n  1. bar\n  2. bar\n* foo",
            "bar\n* foo\n  1. bar\n  2. bar\n* foo",
            "bar\n\n* foo\n  1. bar\n  2. bar\n* foo",
            "bar foo 1. bar 2. bar foo",
        )
        assertModes(
            "bar\n1. foo\n   * bar\n   * bar\n2. foo",
            "bar\n1. foo\n   * bar\n   * bar\n2. foo",
            "bar\n\n1. foo\n   * bar\n   * bar\n2. foo",
            "bar 1. foo bar bar 2. foo",
        )
        assertModes(
            "foo\n\n* bar\n\n* baz",
            "foo\n* bar\n* baz",
            "foo\n\n* bar\n\n* baz",
            "foo bar baz",
        )
        assertCompact("List:\n1. 2) 3. \nend", "List:\n1. 2) 3. \nend")
        assertCompact("1. A\n   1) B\n      1. Test", "1. A\n   1) B\n      1. Test")
    }

    @Test
    fun codeBlocksBreaksAndHtml() {
        assertModes(
            "foo\n```\nfoo\nbar\n```\nbar",
            "foo\nfoo\nbar\nbar",
            "foo\n\nfoo\nbar\n\nbar",
            "foo foo bar bar",
        )
        assertModes(
            "foo\n\n    foo\n     bar\nbar",
            "foo\nfoo\n bar\nbar",
            "foo\n\nfoo\n bar\n\nbar",
            "foo foo bar bar",
        )
        assertModes("foo\nbar", "foo\nbar", "foo\nbar", "foo bar")
        assertModes("foo  \nbar", "foo\nbar", "foo\nbar", "foo bar")
        assertModes(
            "foo\n___\nbar",
            "foo\n***\nbar",
            "foo\n\n***\n\nbar",
            "foo bar",
        )

        val blockHtml =
            "<table>\n  <tr>\n    <td>\n           foobar\n    </td>\n  </tr>\n</table>"
        assertCompact(blockHtml, blockHtml)
        assertSeparate(blockHtml, blockHtml)
        assertAll("foo <foo>foobar</foo> bar", "foo <foo>foobar</foo> bar")
    }

    @Test
    fun customRendererOverridesLinks() {
        val factory =
            TextContentNodeRendererFactory { context ->
                object : NodeRenderer {
                    override val nodeTypes: Set<KClass<out Node>> = setOf(Link::class)

                    override fun render(node: Node) {
                        context.writer.write('"')
                        var child = node.firstChild
                        while (child != null) {
                            val next = child.next
                            context.render(child)
                            child = next
                        }
                        context.writer.write('"')
                    }
                }
            }
        val renderer = TextContentRenderer.builder().nodeRendererFactory(factory).build()
        assertEquals(
            "Hi \"Example\"",
            renderer.render(parser.parse("Hi [Example](https://example.com)")),
        )
    }

    private fun assertModes(
        source: String,
        compactExpected: String,
        separateExpected: String,
        strippedExpected: String,
    ) {
        assertCompact(source, compactExpected)
        assertSeparate(source, separateExpected)
        assertStripped(source, strippedExpected)
    }

    private fun assertCompact(source: String, expected: String) {
        assertEquals(expected, compact.render(parser.parse(source)))
    }

    private fun assertSeparate(source: String, expected: String) {
        assertEquals(expected, separate.render(parser.parse(source)))
    }

    private fun assertStripped(source: String, expected: String) {
        assertEquals(expected, stripped.render(parser.parse(source)))
    }

    private fun assertAll(source: String, expected: String) {
        assertModes(source, expected, expected, expected)
    }

}
