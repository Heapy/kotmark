package io.heapy.kotmark.ext.highlight

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

class HighlightTest {
    private val extensions: List<Extension> = listOf(HighlightExtension.create())
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }
    private val text = textContentRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }

    @Test
    fun syntaxAndRendering() {
        assertHtml("=foo=", "<p>=foo=</p>\n")
        assertHtml("==foo==", "<p><mark>foo</mark></p>\n")
        assertHtml("foo ====", "<p>foo ====</p>\n")
        assertHtml("==foo", "<p>==foo</p>\n")
        assertHtml("foo==", "<p>foo==</p>\n")
        for (literal in listOf("===foo===", "==foo===", "====foo====", "== foo==", "==foo ==", "a == b")) {
            assertHtml(literal, "<p>$literal</p>\n")
        }
        assertHtml("==foo=bar==", "<p><mark>foo=bar</mark></p>\n")
        assertHtml("a==word==b", "<p>a<mark>word</mark>b</p>\n")
        assertHtml("==foo====bar==", "<p><mark>foo====bar</mark></p>\n")
        assertHtml("==foo== ==bar==", "<p><mark>foo</mark> <mark>bar</mark></p>\n")
        assertHtml("`==foo==`", "<p><code>==foo==</code></p>\n")
        assertHtml("==[link](/url)==", "<p><mark><a href=\"/url\">link</a></mark></p>\n")
        assertHtml("==a\nb==", "<p><mark>a\nb</mark></p>\n")
        assertHtml("title\n==", "<h1>title</h1>\n")
        assertHtml(
            "==Paragraph with *emphasis* and __strong emphasis__==",
            "<p><mark>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></mark></p>\n",
        )
        assertHtml(
            "> underline ==that==",
            """
                |<blockquote>
                |<p>underline <mark>that</mark></p>
                |</blockquote>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun delimiterTextMarkdownAndSourceSpans() {
        val node = parser.parse("==foo==").firstChild?.firstChild as Highlight
        assertEquals("==", node.openingDelimiter)
        assertEquals("==", node.closingDelimiter)
        assertEquals("foo", text.render(parser.parse("==foo==")))

        for (input in listOf("==foo==\n", """\=\=foo\=\=""" + "\n")) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }

        val spansParser =
            parser {
                extensions(extensions)
                includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
            }
        val paragraph = spansParser.parse("hey ==there==\n").firstChild as Paragraph
        assertEquals(
            listOf(SourceSpan.of(0, 4, 4, 9)),
            paragraph.lastChild?.getSourceSpans(),
        )
    }

    @Test
    fun optInEscapingAndAttributes() {
        assertEquals("<p>==text==</p>\n", html.render(io.heapy.kotmark.parser.parser().parse("==text==")))
        assertHtml("\\==text==", "<p>==text==</p>\n")
        assertHtml("==a &amp; b==", "<p><mark>a &amp; b</mark></p>\n")
        val renderer = htmlRenderer {
            extensions(extensions)
            attributeProviderFactory {
                io.heapy.kotmark.renderer.html.AttributeProvider { node, _, attributes ->
                    if (node is Highlight) attributes["class"] = "highlight"
                }
            }
        }
        assertEquals("<p><mark class=\"highlight\">text</mark></p>\n", renderer.render(parser.parse("==text==")))
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }
}
