package io.heapy.kotmark.ext.autolink

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SoftLineBreak
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AutolinkTest {
    private val extensions: List<Extension> = listOf(AutolinkExtension.create())
    private val parser = parser { extensions(extensions) }
    private val renderer = htmlRenderer { extensions(extensions) }

    @Test
    fun arbitrarySchemesAndUserInfo() {
        for (url in listOf("git://user@example.com/repo", "custom+v1://user:pass@example.com/path", "HTTP://example.com")) {
            assertHtml(url, """<p><a href="$url">$url</a></p>""" + "\n")
        }
        // autolink-java recognizes lowercase www only.
        assertHtml("WWW.example.com", "<p>WWW.example.com</p>\n")
    }

    @Test
    fun longEmailLikeTextWithoutAtSign() {
        val text = "a.".repeat(10_000)
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        assertHtml(text, "<p>$text</p>\n")
        kotlin.test.assertTrue(started.elapsedNow() < kotlin.time.Duration.parse("5s"))
    }

    @Test
    fun emailCandidateBoundaries() {
        assertHtml("a@b@c.example", """<p>a@<a href="mailto:b@c.example">b@c.example</a></p>""" + "\n")
        assertHtml("a+b@example.com.", """<p><a href="mailto:a+b@example.com">a+b@example.com</a>.</p>""" + "\n")
        assertHtml("a@example.com..tail", """<p><a href="mailto:a@example.com">a@example.com</a>..tail</p>""" + "\n")
        // Reference output from autolink-java 0.12.0: trim the dash from the email.
        assertHtml("a@example.com-", """<p><a href="mailto:a@example.com">a@example.com</a>-</p>""" + "\n")
    }

    @Test
    fun urlsEmailsAndWwwLinks() {
        assertHtml(
            "foo http://one.org/ bar http://two.org/",
            """<p>foo <a href="http://one.org/">http://one.org/</a> bar """ +
                """<a href="http://two.org/">http://two.org/</a></p>""" + "\n",
        )
        assertHtml(
            "foo http://one.org/ bar `code` baz http://two.org/",
            """<p>foo <a href="http://one.org/">http://one.org/</a> bar <code>code</code> baz """ +
                """<a href="http://two.org/">http://two.org/</a></p>""" + "\n",
        )
        assertHtml(
            "foo@example.com",
            """<p><a href="mailto:foo@example.com">foo@example.com</a></p>""" + "\n",
        )
        assertHtml("foo@com", "<p>foo@com</p>\n")
        assertHtml(
            "www.example.com",
            """<p><a href="http://www.example.com">www.example.com</a></p>""" + "\n",
        )
    }

    @Test
    fun trimsExtendedAutolinkPaths() {
        assertHtml(
            "http://example.com/one. Example 2 (see http://example.com/two). " +
                "Example 3: http://example.com/foo_(bar)",
            """<p><a href="http://example.com/one">http://example.com/one</a>. """ +
                """Example 2 (see <a href="http://example.com/two">http://example.com/two</a>). """ +
                """Example 3: <a href="http://example.com/foo_(bar)">http://example.com/foo_(bar)</a></p>""" + "\n",
        )
        assertHtml(
            "www.google.com/search?q=Markup+(business)))",
            """<p><a href="http://www.google.com/search?q=Markup+(business)">""" +
                "www.google.com/search?q=Markup+(business)</a>))</p>\n",
        )
        assertHtml(
            "www.google.com/search?q=commonmark&hl;",
            """<p><a href="http://www.google.com/search?q=commonmark&amp;hl">""" +
                "www.google.com/search?q=commonmark&amp;hl</a>;</p>\n",
        )
    }

    @Test
    fun existingLinksAreNotNested() {
        assertHtml(
            "<http://example.com>",
            """<p><a href="http://example.com">http://example.com</a></p>""" + "\n",
        )
    }

    @Test
    fun linkTypesAreConfigurable() {
        val extension =
            autolinkExtension { linkTypes = setOf(AutolinkType.URL, AutolinkType.EMAIL) }
        val configuredParser = parser { extensions(listOf(extension)) }
        val configuredRenderer = htmlRenderer { extensions(listOf(extension)) }
        assertEquals(
            """<p>www.example.com <a href="mailto:foo@example.com">foo@example.com</a></p>""" + "\n",
            configuredRenderer.render(
                configuredParser.parse("www.example.com foo@example.com"),
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            autolinkExtension { linkTypes = emptySet() }
        }
    }

    @Test
    fun sourceSpansAreSplitWithTextNodes() {
        val spansParser =
            parser {
                extensions(extensions)
                includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
            }
        val paragraph =
            assertIs<Paragraph>(
                spansParser.parse(
                    """
                        |abc
                        |http://example.com/one
                        |def http://example.com/two
                        |ghi http://example.com/three jkl
                    """.trimMargin(),
                ).firstChild,
            )
        val abc = assertIs<Text>(paragraph.firstChild)
        assertEquals(listOf(SourceSpan.of(0, 0, 0, 3)), abc.getSourceSpans())
        assertIs<SoftLineBreak>(abc.next)
        val one = assertIs<Link>(abc.next?.next)
        assertEquals("http://example.com/one", one.destination)
        assertEquals(listOf(SourceSpan.of(1, 0, 4, 22)), one.getSourceSpans())
        val def = assertIs<Text>(one.next?.next)
        assertEquals("def ", def.literal)
        assertEquals(listOf(SourceSpan.of(2, 0, 27, 4)), def.getSourceSpans())
        val two = assertIs<Link>(def.next)
        assertEquals(listOf(SourceSpan.of(2, 4, 31, 22)), two.getSourceSpans())
        val ghi = assertIs<Text>(two.next?.next)
        val three = assertIs<Link>(ghi.next)
        assertEquals(listOf(SourceSpan.of(3, 4, 58, 24)), three.getSourceSpans())
        val jkl = assertIs<Text>(three.next)
        assertEquals(" jkl", jkl.literal)
        assertEquals(listOf(SourceSpan.of(3, 28, 82, 4)), jkl.getSourceSpans())
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
