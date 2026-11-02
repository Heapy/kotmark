package io.heapy.kotmark.ext.footnotes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class FootnoteRenderingTest {
    private val extensions: List<Extension> = listOf(FootnotesExtension.create())
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }

    @Test
    fun rendersOneFootnote() {
        assertHtml(
            """
                |Test [^foo]
                |
                |[^foo]: note
                |
            """.trimMargin(),
            """
                |<p>Test <sup class="footnote-ref"><a href="#fn-foo" id="fnref-foo" data-footnote-ref>1</a></sup></p>
                |<section class="footnotes" data-footnotes>
                |<ol>
                |<li id="fn-foo">
                |<p>note <a href="#fnref-foo" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1" aria-label="Back to reference 1">↩</a></p>
                |</li>
                |</ol>
                |</section>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun numberingAndMultipleBacklinksFollowReferenceOrder() {
        assertHtml(
            """
                |First [^foo]
                |
                |Then [^bar]
                |
                |Then [^foo] again
                |
                |[^bar]: b
                |[^foo]: f
                |
            """.trimMargin(),
            """
                |<p>First <sup class="footnote-ref"><a href="#fn-foo" id="fnref-foo" data-footnote-ref>1</a></sup></p>
                |<p>Then <sup class="footnote-ref"><a href="#fn-bar" id="fnref-bar" data-footnote-ref>2</a></sup></p>
                |<p>Then <sup class="footnote-ref"><a href="#fn-foo" id="fnref-foo-2" data-footnote-ref>1</a></sup> again</p>
                |<section class="footnotes" data-footnotes>
                |<ol>
                |<li id="fn-foo">
                |<p>f <a href="#fnref-foo" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1" aria-label="Back to reference 1">↩</a> <a href="#fnref-foo-2" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1-2" aria-label="Back to reference 1-2"><sup class="footnote-ref">2</sup>↩</a></p>
                |</li>
                |<li id="fn-bar">
                |<p>b <a href="#fnref-bar" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="2" aria-label="Back to reference 2">↩</a></p>
                |</li>
                |</ol>
                |</section>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun nestedDefinitionsAreDiscoveredBreadthFirst() {
        assertHtml(
            """
                |[^foo1]
                |
                |[^foo1]: one [^foo2]
                |[^foo2]: two
                |
            """.trimMargin(),
            """
                |<p><sup class="footnote-ref"><a href="#fn-foo1" id="fnref-foo1" data-footnote-ref>1</a></sup></p>
                |<section class="footnotes" data-footnotes>
                |<ol>
                |<li id="fn-foo1">
                |<p>one <sup class="footnote-ref"><a href="#fn-foo2" id="fnref-foo2" data-footnote-ref>2</a></sup> <a href="#fnref-foo1" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1" aria-label="Back to reference 1">↩</a></p>
                |</li>
                |<li id="fn-foo2">
                |<p>two <a href="#fnref-foo2" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="2" aria-label="Back to reference 2">↩</a></p>
                |</li>
                |</ol>
                |</section>
                |
            """.trimMargin(),
        )
        assertEquals("", html.render(parser.parse("[^foo]: foo[^bar]\n[^bar]: bar\n")))
    }

    @Test
    fun inlineFootnotesAndNestedInlineFootnotes() {
        val inlineExtension = footnotesExtension { inlineFootnotes = true }
        val inlineParser = parser { extensions(listOf(inlineExtension)) }
        val inlineHtml = htmlRenderer { extensions(listOf(inlineExtension)) }
        assertEquals(
            """
                |<p>Test <sup class="footnote-ref"><a href="#fn1" id="fnref1" data-footnote-ref>1</a></sup></p>
                |<section class="footnotes" data-footnotes>
                |<ol>
                |<li id="fn1">
                |<p>inline <sup class="footnote-ref"><a href="#fn2" id="fnref2" data-footnote-ref>2</a></sup> <a href="#fnref1" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1" aria-label="Back to reference 1">↩</a></p>
                |</li>
                |<li id="fn2">
                |<p>nested <a href="#fnref2" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="2" aria-label="Back to reference 2">↩</a></p>
                |</li>
                |</ol>
                |</section>
                |
            """.trimMargin(),
            inlineHtml.render(inlineParser.parse("Test ^[inline ^[nested]]")),
        )
    }

    @Test
    fun markdownRoundTrips() {
        val inlineExtension = footnotesExtension { inlineFootnotes = true }
        val roundTripParser = parser { extensions(listOf(inlineExtension)) }
        val markdown = markdownRenderer { extensions(listOf(inlineExtension)) }
        for (
            input in
                listOf(
                    """
                        |Test [^foo]
                        |
                        |[^foo]: note
                        |
                    """.trimMargin(),
                    """
                        |Test [^foo]
                        |
                        |[^foo]: one
                        |
                        |[^bar]: two
                        |
                    """.trimMargin(),
                    """
                        |Test [^foo]
                        |
                        |[^foo]: - foo
                        |    - bar
                        |
                    """.trimMargin(),
                    """
                        |[^\foo]
                        |
                        |[^\foo]: note
                        |
                    """.trimMargin(),
                    """
                        |Test [^1]
                        |
                        |[^1]: footnote l1
                        |    footnote l2
                        |
                    """.trimMargin(),
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
