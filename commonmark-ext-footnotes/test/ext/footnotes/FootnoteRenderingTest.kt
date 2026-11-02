package io.heapy.kotmark.ext.footnotes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class FootnoteRenderingTest {
    private val extensions: List<Extension> = listOf(FootnotesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()

    @Test
    fun rendersOneFootnote() {
        assertHtml(
            "Test [^foo]\n\n[^foo]: note\n",
            "<p>Test <sup class=\"footnote-ref\"><a href=\"#fn-foo\" id=\"fnref-foo\" data-footnote-ref>1</a></sup></p>\n" +
                "<section class=\"footnotes\" data-footnotes>\n" +
                "<ol>\n" +
                "<li id=\"fn-foo\">\n" +
                "<p>note <a href=\"#fnref-foo\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"1\" aria-label=\"Back to reference 1\">↩</a></p>\n" +
                "</li>\n" +
                "</ol>\n" +
                "</section>\n",
        )
    }

    @Test
    fun numberingAndMultipleBacklinksFollowReferenceOrder() {
        assertHtml(
            "First [^foo]\n\nThen [^bar]\n\nThen [^foo] again\n\n[^bar]: b\n[^foo]: f\n",
            "<p>First <sup class=\"footnote-ref\"><a href=\"#fn-foo\" id=\"fnref-foo\" data-footnote-ref>1</a></sup></p>\n" +
                "<p>Then <sup class=\"footnote-ref\"><a href=\"#fn-bar\" id=\"fnref-bar\" data-footnote-ref>2</a></sup></p>\n" +
                "<p>Then <sup class=\"footnote-ref\"><a href=\"#fn-foo\" id=\"fnref-foo-2\" data-footnote-ref>1</a></sup> again</p>\n" +
                "<section class=\"footnotes\" data-footnotes>\n" +
                "<ol>\n" +
                "<li id=\"fn-foo\">\n" +
                "<p>f <a href=\"#fnref-foo\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"1\" aria-label=\"Back to reference 1\">↩</a> <a href=\"#fnref-foo-2\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"1-2\" aria-label=\"Back to reference 1-2\"><sup class=\"footnote-ref\">2</sup>↩</a></p>\n" +
                "</li>\n" +
                "<li id=\"fn-bar\">\n" +
                "<p>b <a href=\"#fnref-bar\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"2\" aria-label=\"Back to reference 2\">↩</a></p>\n" +
                "</li>\n" +
                "</ol>\n" +
                "</section>\n",
        )
    }

    @Test
    fun nestedDefinitionsAreDiscoveredBreadthFirst() {
        assertHtml(
            "[^foo1]\n\n[^foo1]: one [^foo2]\n[^foo2]: two\n",
            "<p><sup class=\"footnote-ref\"><a href=\"#fn-foo1\" id=\"fnref-foo1\" data-footnote-ref>1</a></sup></p>\n" +
                "<section class=\"footnotes\" data-footnotes>\n" +
                "<ol>\n" +
                "<li id=\"fn-foo1\">\n" +
                "<p>one <sup class=\"footnote-ref\"><a href=\"#fn-foo2\" id=\"fnref-foo2\" data-footnote-ref>2</a></sup> <a href=\"#fnref-foo1\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"1\" aria-label=\"Back to reference 1\">↩</a></p>\n" +
                "</li>\n" +
                "<li id=\"fn-foo2\">\n" +
                "<p>two <a href=\"#fnref-foo2\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"2\" aria-label=\"Back to reference 2\">↩</a></p>\n" +
                "</li>\n" +
                "</ol>\n" +
                "</section>\n",
        )
        assertEquals("", html.render(parser.parse("[^foo]: foo[^bar]\n[^bar]: bar\n")))
    }

    @Test
    fun inlineFootnotesAndNestedInlineFootnotes() {
        val inlineExtension = FootnotesExtension.builder().inlineFootnotes(true).build()
        val inlineParser = Parser.builder().extensions(listOf(inlineExtension)).build()
        val inlineHtml = HtmlRenderer.builder().extensions(listOf(inlineExtension)).build()
        assertEquals(
            "<p>Test <sup class=\"footnote-ref\"><a href=\"#fn1\" id=\"fnref1\" data-footnote-ref>1</a></sup></p>\n" +
                "<section class=\"footnotes\" data-footnotes>\n" +
                "<ol>\n" +
                "<li id=\"fn1\">\n" +
                "<p>inline <sup class=\"footnote-ref\"><a href=\"#fn2\" id=\"fnref2\" data-footnote-ref>2</a></sup> <a href=\"#fnref1\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"1\" aria-label=\"Back to reference 1\">↩</a></p>\n" +
                "</li>\n" +
                "<li id=\"fn2\">\n" +
                "<p>nested <a href=\"#fnref2\" class=\"footnote-backref\" data-footnote-backref data-footnote-backref-idx=\"2\" aria-label=\"Back to reference 2\">↩</a></p>\n" +
                "</li>\n" +
                "</ol>\n" +
                "</section>\n",
            inlineHtml.render(inlineParser.parse("Test ^[inline ^[nested]]")),
        )
    }

    @Test
    fun markdownRoundTrips() {
        val inlineExtension = FootnotesExtension.builder().inlineFootnotes(true).build()
        val roundTripParser = Parser.builder().extensions(listOf(inlineExtension)).build()
        val markdown = MarkdownRenderer.builder().extensions(listOf(inlineExtension)).build()
        for (
            input in
                listOf(
                    "Test [^foo]\n\n[^foo]: note\n",
                    "Test [^foo]\n\n[^foo]: one\n\n[^bar]: two\n",
                    "Test [^foo]\n\n[^foo]: - foo\n    - bar\n",
                    "[^\\foo]\n\n[^\\foo]: note\n",
                    "Test [^1]\n\n[^1]: footnote l1\n    footnote l2\n",
                    "Test [^1]\n\n[^1]: footnote p1\n    \n    footnote p2\n",
                    "^[test *foo*]\n",
                )
        ) {
            assertEquals(input, markdown.render(roundTripParser.parse(input)))
        }
    }

    private fun assertHtml(source: String, expected: String) {
        assertEquals(expected, html.render(parser.parse(source)))
    }
}
