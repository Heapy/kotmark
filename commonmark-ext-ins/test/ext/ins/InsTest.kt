package io.heapy.kotmark.ext.ins

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class InsTest {
    private val extensions: List<Extension> = listOf(InsExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()
    private val text = TextContentRenderer.builder().extensions(extensions).build()
    private val markdown = MarkdownRenderer.builder().extensions(extensions).build()

    @Test
    fun syntaxAndRendering() {
        assertHtml("+foo+", "<p>+foo+</p>\n")
        assertHtml("++foo++", "<p><ins>foo</ins></p>\n")
        assertHtml("foo ++++", "<p>foo ++++</p>\n")
        assertHtml("++foo", "<p>++foo</p>\n")
        assertHtml("foo++", "<p>foo++</p>\n")
        assertHtml("+++foo+++", "<p>+<ins>foo</ins>+</p>\n")
        assertHtml("++foo+++", "<p><ins>foo</ins>+</p>\n")
        assertHtml("++foo+bar++", "<p><ins>foo+bar</ins></p>\n")
        assertHtml("++foo++bar++", "<p><ins>foo</ins>bar++</p>\n")
        assertHtml("++foo+++bar++", "<p><ins>foo</ins>+bar++</p>\n")
        assertHtml("++foo++++bar++", "<p><ins>foo</ins><ins>bar</ins></p>\n")
        assertHtml("++foo+++++bar++", "<p><ins>foo</ins>+<ins>bar</ins></p>\n")
        assertHtml("++foo++++++bar++", "<p><ins>foo</ins>++<ins>bar</ins></p>\n")
        assertHtml("++foo+++++++bar++", "<p><ins>foo</ins>+++<ins>bar</ins></p>\n")
        assertHtml(
            "++Paragraph with *emphasis* and __strong emphasis__++",
            "<p><ins>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></ins></p>\n",
        )
        assertHtml(
            "> underline ++that++",
            "<blockquote>\n<p>underline <ins>that</ins></p>\n</blockquote>\n",
        )
    }

    @Test
    fun delimiterTextMarkdownAndSourceSpans() {
        val node = parser.parse("++foo++").firstChild?.firstChild as Ins
        assertEquals("++", node.openingDelimiter)
        assertEquals("++", node.closingDelimiter)
        assertEquals("foo", text.render(parser.parse("++foo++")))

        for (input in listOf("++foo++\n", "\\+\\+foo\\+\\+\n")) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }

        val spansParser =
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val paragraph = spansParser.parse("hey ++there++\n").firstChild as Paragraph
        assertEquals(
            listOf(SourceSpan.of(0, 4, 4, 9)),
            paragraph.lastChild?.getSourceSpans(),
        )
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }
}
