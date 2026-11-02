// Port of commonmark-java 9477a93b6b0965efc54c55bd40ad88fbbe25bc6f AutolinkTest.
// Copyright (c) 2015 Robin Stocker. BSD 2-Clause; see UPSTREAM.md.
package io.heapy.kotmark.ext.autolink

import io.heapy.kotmark.node.*
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.*

class UpstreamAutolinkTest {
    private val extensions = listOf(AutolinkExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val renderer = HtmlRenderer.builder().extensions(extensions).build()
    private val noWwwParser = Parser.builder().extensions(listOf(AutolinkExtension.builder().linkTypes(AutolinkType.URL, AutolinkType.EMAIL).build())).build()

    @Test
    fun oneTextNode() {
        assertRendering("foo http://one.org/ bar http://two.org/",
                """<p>foo <a href="http://one.org/">http://one.org/</a> bar <a href="http://two.org/">http://two.org/</a></p>""" + "\n")

    }

    @Test
    fun textNodeAndOthers() {
        assertRendering("foo http://one.org/ bar `code` baz http://two.org/",
                """<p>foo <a href="http://one.org/">http://one.org/</a> bar <code>code</code> baz <a href="http://two.org/">http://two.org/</a></p>""" + "\n")

    }

    @Test
    fun tricky() {
        assertRendering("http://example.com/one. Example 2 (see http://example.com/two). Example 3: http://example.com/foo_(bar)",
                """<p><a href="http://example.com/one">http://example.com/one</a>. """ +
                        """Example 2 (see <a href="http://example.com/two">http://example.com/two</a>). """ +
                        """Example 3: <a href="http://example.com/foo_(bar)">http://example.com/foo_(bar)</a></p>""" + "\n")

    }

    @Test
    fun emailUsesMailto() {
        assertRendering("foo@example.com",
                """<p><a href="mailto:foo@example.com">foo@example.com</a></p>""" + "\n")

    }

    @Test
    fun emailWithTldNotLinked() {
        assertRendering("foo@com",
                "<p>foo@com</p>\n")

    }

    @Test
    fun dontLinkTextWithinLinks() {
        assertRendering("<http://example.com>",
                """<p><a href="http://example.com">http://example.com</a></p>""" + "\n")

    }

    @Test
    fun wwwLinks() {
        assertRendering("www.example.com",
                """<p><a href="http://www.example.com">www.example.com</a></p>""" + "\n")

    }

    @Test
    fun noWwwLinks() {
        val html = renderer.render(noWwwParser.parse("www.example.com"))
        assertEquals("<p>www.example.com</p>\n", html)

    }

    @Test
    fun sourceSpans() {
        val parser = Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val document = parser.parse("abc\n" +
                """
                    |http://example.com/one
                    |def http://example.com/two
                    |ghi http://example.com/three jkl
                """.trimMargin())

        val paragraph = assertIs<Paragraph>(document.firstChild)
        val abc = assertIs<Text>(paragraph.firstChild)
        assertEquals(listOf(SourceSpan.of(0, 0, 0, 3)), abc.getSourceSpans())

        assertIs<SoftLineBreak>(abc.next)

        val one = assertIs<Link>(abc.next!!.next)
        assertEquals("http://example.com/one", one.destination)
        assertEquals(listOf(SourceSpan.of(1, 0, 4, 22)), one.getSourceSpans())

        assertIs<SoftLineBreak>(one.next)

        val def = assertIs<Text>(one.next!!.next)
        assertEquals("def ", def.literal)
        assertEquals(listOf(SourceSpan.of(2, 0, 27, 4)), def.getSourceSpans())

        val two = assertIs<Link>(def.next)
        assertEquals("http://example.com/two", two.destination)
        assertEquals(listOf(SourceSpan.of(2, 4, 31, 22)), two.getSourceSpans())

        assertIs<SoftLineBreak>(two.next)

        val ghi = assertIs<Text>(two.next!!.next)
        assertEquals("ghi ", ghi.literal)
        assertEquals(listOf(SourceSpan.of(3, 0, 54, 4)), ghi.getSourceSpans())

        val three = assertIs<Link>(ghi.next)
        assertEquals("http://example.com/three", three.destination)
        assertEquals(listOf(SourceSpan.of(3, 4, 58, 24)), three.getSourceSpans())

        val jkl = assertIs<Text>(three.next)
        assertEquals(" jkl", jkl.literal)
        assertEquals(listOf(SourceSpan.of(3, 28, 82, 4)), jkl.getSourceSpans())

    }

    private fun assertRendering(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
