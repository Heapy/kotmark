package io.heapy.kotmark.ext.toc

import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TocTest {
    private val toc = tocExtension()

    private fun html(source: String, extension: TocExtension = toc): String {
        val extensions = listOf(extension)
        return htmlRenderer { extensions(extensions) }.render(parser { extensions(extensions) }.parse(source))
    }

    @Test
    fun bothMarkersAndSkippedLevels() {
        val expected = """
            |<ul>
            |<li><a href="#one">One</a>
            |<ul>
            |<li><a href="#three">Three</a></li>
            |<li><a href="#two">Two</a></li>
            |</ul>
            |</li>
            |<li><a href="#next">Next</a></li>
            |</ul>
            |<h1 id="one">One</h1>
            |<h3 id="three">Three</h3>
            |<h2 id="two">Two</h2>
            |<h1 id="next">Next</h1>
            |
        """.trimMargin()
        for (marker in listOf("[TOC]", "[[_TOC_]]")) {
            assertEquals(expected, html("$marker\n# One\n### Three\n## Two\n# Next"))
        }
    }

    @Test
    fun markerGrammarAndOptIn() {
        val literals = listOf("inline [TOC]", "[TOC] after", "[toc]", "\\[TOC]", "`[TOC]`",
            "    [TOC]", "\t[TOC]", "```\n[TOC]\n```", "<div>\n[TOC]\n</div>", "[TOC]: /target")
        for (source in literals) {
            assertEquals(htmlRenderer().render(parser().parse(source)), html(source), source)
        }
        assertEquals("<p>[TOC]</p>\n", htmlRenderer().render(parser().parse("[TOC]")))
        assertTrue(html("paragraph\n   [TOC] \t\n# A").startsWith("<p>paragraph</p>\n<ul>"))
        assertTrue(html("> [TOC]\n> # A").contains("<a href=\"#a\">A</a>"))
        assertTrue(html("- [TOC]\n\n# A").contains("<a href=\"#a\">A</a>"))
    }

    @Test
    fun noHeadingsMissingAndRepeatedMarkers() {
        assertEquals("", html("[TOC]\n\n[[_TOC_]]"))
        assertEquals("<h1 id=\"a\">A</h1>\n", html("# A"))
        val single = html("[TOC]\n\n# A")
        val list = single.substringBefore("<h1")
        assertEquals(list + list + "<h1 id=\"a\">A</h1>\n", html("[TOC]\n[[_TOC_]]\n# A"))
        val filtered = tocExtension { headingLevels = 3..4 }
        assertEquals("<h1 id=\"a\">A</h1>\n", html("[TOC]\n# A", filtered))
    }

    @Test
    fun setextAndNestedHeadingsUseDocumentOrder() {
        val result = html("[TOC]\n\nTitle\n=====\n\n> ## Quote\n\n- ### Item\n\n## Last")
        val list = result.substringBefore("<h1")
        assertEquals(listOf("title", "quote", "item", "last"),
            Regex("href=\"#([^\"]+)\"").findAll(list).map { it.groupValues[1] }.toList())
        assertEquals(3, Regex("<ul>").findAll(list).count())
    }

    @Test
    fun htmlSubtreeRenderingDoesNotLinkOutsideTheSubtree() {
        val extensions = listOf(toc)
        val document = parser { extensions(extensions) }.parse("# Outside\n\n> [TOC]\n> ## Inside")
        val result = htmlRenderer { extensions(extensions) }.render(document.lastChild!!)
        assertTrue(result.contains("href=\"#inside\""))
        assertFalse(result.contains("outside"))
    }

    @Test
    fun placementAndCustomMarkers() {
        val prepend = tocExtension { placement = TocPlacement.PREPEND_IF_MISSING }
        val append = tocExtension { placement = TocPlacement.APPEND_IF_MISSING }
        assertEquals(html("[TOC]\n# A"), html("# A", prepend))
        assertEquals(html("# A\n[TOC]"), html("# A", append))
        assertEquals(html("# A\n[TOC]"), html("# A\n[TOC]", prepend))
        assertEquals(html("[TOC]\n# A"), html("[TOC]\n# A", append))
        val custom = tocExtension { markers = listOf("{{contents}}") }
        assertEquals(html("[TOC]\n# A"), html("{{contents}}\n# A", custom))
        assertTrue(html("[TOC]\n\n# A", custom).startsWith("<p>[TOC]</p>"))
    }

    @Test
    fun formattingLabelsArePlainSafeText() {
        val result = html("[TOC]\n# **Bold** *em* `code` [link](/x) ![alt](img) <b>raw</b> &amp; &lt;x&gt;")
        val list = result.substringBefore("<h1")
        assertTrue(list.contains(">Bold em code link alt raw &amp; &lt;x&gt;</a>"), list)
        assertFalse(list.contains("<b>"))
        assertFalse(list.contains("<img"))
        assertEquals(1, Regex("<a ").findAll(list).count())
        assertTrue(result.contains("<strong>Bold</strong>"))
    }

    @Test
    fun filteredHeadingsStillReserveIdsAndRenderingDoesNotMutateAst() {
        val extensions = listOf(tocExtension { headingLevels = 2..3 })
        val document = parser { extensions(extensions) }.parse("[TOC]\n# Same\n## Same\n### Same-1\n## Same")
        val markdown = markdownRenderer { extensions(extensions) }
        val before = markdown.render(document)
        val renderer = htmlRenderer { extensions(extensions) }
        val result = renderer.render(document)
        assertFalse(result.contains("href=\"#same\""))
        for (id in listOf("same-1", "same-1-1", "same-2")) {
            assertTrue(result.contains("href=\"#$id\""), result)
            assertTrue(result.contains("id=\"$id\""), result)
        }
        assertEquals(result, renderer.render(document))
        assertEquals(before, markdown.render(document))
    }

    @Test
    fun customIdsAreReadOnceAndFragmentEncoded() {
        val extensions = listOf(toc)
        var calls = 0
        val renderer = htmlRenderer {
            extensions(extensions)
            attributeProviderFactory {
                AttributeProvider { node, _, attrs ->
                    if (node is Heading) {
                        calls++
                        attrs["id"] = "custom %2F#\"&Ж-$calls"
                        attrs["class"] = "section"
                    }
                }
            }
        }
        val document = parser { extensions(extensions) }.parse("[TOC]\n# A\n# A\n[TOC]")
        val result = renderer.render(document)
        assertEquals(2, calls)
        assertTrue(result.contains("href=\"#custom%20%252F%23%22%26%D0%96-1\""), result)
        assertTrue(result.contains("id=\"custom %2F#&quot;&amp;Ж-1\" class=\"section\""), result)
    }

    @Test
    fun fallbackIdsAvoidSuppliedIdsAndSupportUnicode() {
        val extensions = listOf(toc)
        val renderer = htmlRenderer {
            extensions(extensions)
            attributeProviderFactory {
                AttributeProvider { node, _, attrs ->
                    if (node is Heading && (node.firstChild as? Text)?.literal == "Custom") attrs["id"] = "a"
                }
            }
        }
        val result = renderer.render(parser { extensions(extensions) }.parse("[TOC]\n# A\n# Custom\n# Ж\n# Ж\n# 😀"))
        for (id in listOf("a-1", "a", "ж", "ж-1", "id")) assertTrue(result.contains("id=\"$id\""))
        assertTrue(result.contains("href=\"#%D0%B6-1\""))
        assertTrue(result.contains("href=\"#%D0%B6\""))
    }

    @Test
    fun markdownPreservesMarkersAndTextRendersOutline() {
        val extensions = listOf(toc)
        val document = parser { extensions(extensions) }.parse("[[_TOC_]]\n\n# One\n\n### Three")
        val markdown = markdownRenderer { extensions(extensions) }.render(document)
        assertEquals("[[_TOC_]]\n\n# One\n\n### Three\n", markdown)
        assertEquals("- One\n  - Three\nOne\nThree", textContentRenderer { extensions(extensions) }.render(document))
        val stripped = textContentRenderer { extensions(extensions); lineBreakRendering = LineBreakRendering.STRIP }.render(document)
        assertFalse(stripped.contains('\n'))
        assertEquals(html("[[_TOC_]]\n\n# One\n\n### Three"), html(markdown))
    }

    @Test
    fun markerSpansAndConfigurationSnapshots() {
        val markers = mutableListOf("[TOC]")
        lateinit var configuration: TocExtension.Configuration
        val extension = tocExtension { configuration = this; this.markers = markers }
        markers.clear()
        configuration.headingLevels = 6..6
        val document = parser {
            extensions(listOf(extension))
            includeSourceSpans = IncludeSourceSpans.BLOCKS
        }.parse("[TOC]\n# A")
        val marker = assertIs<TableOfContents>(document.firstChild)
        assertEquals(0, marker.getSourceSpans().single().lineIndex)
        assertTrue(htmlRenderer { extensions(listOf(extension)) }.render(document).contains("href=\"#a\""))
        for (invalid in listOf(emptyList(), listOf(""), listOf(" [TOC]"), listOf("a\nb"))) {
            assertFailsWith<IllegalArgumentException> { tocExtension { this.markers = invalid } }
        }
        for (invalid in listOf(0..6, 1..7, 3..2)) {
            assertFailsWith<IllegalArgumentException> { tocExtension { headingLevels = invalid } }
        }
    }
}
