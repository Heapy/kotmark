package io.heapy.kotmark.ext.ins

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class InsTest {
    private val extensions: List<Extension> = listOf(InsExtension.create())
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }
    private val text = textContentRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }

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
            """
                |<blockquote>
                |<p>underline <ins>that</ins></p>
                |</blockquote>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun delimiterTextMarkdownAndSourceSpans() {
        val node = parser.parse("++foo++").firstChild?.firstChild as Ins
        assertEquals("++", node.openingDelimiter)
        assertEquals("++", node.closingDelimiter)
        assertEquals("foo", text.render(parser.parse("++foo++")))

        for (input in listOf("++foo++\n", """\+\+foo\+\+""" + "\n")) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }

        val spansParser =
            parser {
                extensions(extensions)
                includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
            }
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
