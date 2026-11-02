package io.heapy.kotmark.ext.gfm.strikethrough

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class StrikethroughTest {
    private val extensions: List<Extension> = listOf(StrikethroughExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()
    private val text = TextContentRenderer.builder().extensions(extensions).build()
    private val markdown = MarkdownRenderer.builder().extensions(extensions).build()

    @Test
    fun syntaxAndRendering() {
        // All examples from the GFM "strikethrough" spec section.
        assertHtml("~~Hi~~ Hello, world!", "<p><del>Hi</del> Hello, world!</p>\n")
        assertHtml(
            """
                |This ~~has a
                |
                |new paragraph~~.
            """.trimMargin(),
            """
                |<p>This ~~has a</p>
                |<p>new paragraph~~.</p>
                |
            """.trimMargin(),
        )

        assertHtml("~foo~", "<p><del>foo</del></p>\n")
        assertHtml("~~foo~~", "<p><del>foo</del></p>\n")
        assertHtml("foo ~~~~", "<p>foo ~~~~</p>\n")
        assertHtml("~~foo", "<p>~~foo</p>\n")
        assertHtml("foo~~", "<p>foo~~</p>\n")
        assertHtml("a ~~~foo~~~", "<p>a ~~~foo~~~</p>\n")
        assertHtml("~~foo~~~", "<p>~~foo~~~</p>\n")
        assertHtml("~~foo~bar~~", "<p><del>foo~bar</del></p>\n")
        assertHtml("~~foo~~bar~~", "<p><del>foo</del>bar~~</p>\n")
        assertHtml("~~foo~~~bar~~", "<p><del>foo~~~bar</del></p>\n")
        assertHtml("~~foo~~~~bar~~", "<p><del>foo~~~~bar</del></p>\n")
        assertHtml("~~foo~~~~~bar~~", "<p><del>foo~~~~~bar</del></p>\n")
        assertHtml("~~foo~~~~~~bar~~", "<p><del>foo~~~~~~bar</del></p>\n")
        assertHtml(
            "~~Paragraph with *emphasis* and __strong emphasis__~~",
            "<p><del>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></del></p>\n",
        )
        assertHtml(
            "> strike ~~that~~",
            """
                |<blockquote>
                |<p>strike <del>that</del></p>
                |</blockquote>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun delimiterTextMarkdownAndSourceSpans() {
        val node = parser.parse("~~foo~~").firstChild?.firstChild as Strikethrough
        assertEquals("~~", node.openingDelimiter)
        assertEquals("~~", node.closingDelimiter)
        assertEquals("/foo/", text.render(parser.parse("~~foo~~")))

        for (input in listOf("~foo~ ~bar~\n", "~~foo~~ ~~bar~~\n", """~~f\~oo~~ ~~bar~~""" + "\n", """\~foo\~""" + "\n")) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }

        val spansParser =
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val block = spansParser.parse("hey ~~there~~\n").firstChild as Paragraph
        assertEquals(
            listOf(SourceSpan.of(0, 4, 4, 9)),
            block.lastChild?.getSourceSpans(),
        )
    }

    @Test
    fun canRequireTwoTildesAlongsideSingleTildeProcessor() {
        val configuredParser =
            Parser.builder()
                .extensions(
                    listOf(
                        StrikethroughExtension.builder()
                            .requireTwoTildes(true)
                            .build(),
                    ),
                )
                .customDelimiterProcessor(SubscriptDelimiterProcessor())
                .build()

        assertEquals("(sub)foo(/sub) /bar/", text.render(configuredParser.parse("~foo~ ~~bar~~")))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }

    private class SubscriptDelimiterProcessor : DelimiterProcessor {
        override val openingCharacter: Char = '~'
        override val closingCharacter: Char = '~'
        override val minLength: Int = 1

        override fun process(
            openingRun: DelimiterRun,
            closingRun: DelimiterRun,
        ): Int {
            openingRun.opener.insertAfter(Text("(sub)"))
            closingRun.closer.insertBefore(Text("(/sub)"))
            return 1
        }
    }
}
