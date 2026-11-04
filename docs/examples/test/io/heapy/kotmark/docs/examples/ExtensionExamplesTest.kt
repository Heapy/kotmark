package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.extensions.findLinks
import io.heapy.kotmark.docs.examples.extensions.mathAsCodeRenderer
import io.heapy.kotmark.docs.examples.extensions.readPage
import io.heapy.kotmark.docs.examples.extensions.renderArticle
import io.heapy.kotmark.docs.examples.extensions.renderWithAlerts
import io.heapy.kotmark.docs.examples.extensions.renderWithAutolinks
import io.heapy.kotmark.docs.examples.extensions.renderWithEmoji
import io.heapy.kotmark.docs.examples.extensions.renderWithFootnotes
import io.heapy.kotmark.docs.examples.extensions.renderWithHeadingIds
import io.heapy.kotmark.docs.examples.extensions.renderWithHighlight
import io.heapy.kotmark.docs.examples.extensions.renderWithImageAttributes
import io.heapy.kotmark.docs.examples.extensions.renderWithIns
import io.heapy.kotmark.docs.examples.extensions.renderWithMath
import io.heapy.kotmark.docs.examples.extensions.renderWithStrikethrough
import io.heapy.kotmark.docs.examples.extensions.renderWithTables
import io.heapy.kotmark.docs.examples.extensions.renderWithTaskLists
import io.heapy.kotmark.docs.examples.extensions.renderWithToc
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals

/** Inputs and outputs mirror the examples shown on the site's extension pages. */
class ExtensionExamplesTest {
    @Test
    fun tableOfContents() {
        assertEquals(
            """
                |<ul>
                |<li><a href="#section-getting-started">Getting started</a>
                |<ul>
                |<li><a href="#section-install">Install</a></li>
                |</ul>
                |</li>
                |<li><a href="#section-usage">Usage</a></li>
                |</ul>
                |<h2 id="section-getting-started">Getting started</h2>
                |<h3 id="section-install">Install</h3>
                |<h2 id="section-usage">Usage</h2>
                |
            """.trimMargin(),
            renderWithToc("[TOC]\n\n## Getting started\n### Install\n## Usage"),
        )
    }

    @Test
    fun emoji() {
        assertEquals(
            "<p>A 🐛 fixed, a 🌟 earned. People ❤️ that!</p>\n",
            renderWithEmoji("A :bug: fixed, a :star2: earned. People :heart: that!"),
        )
    }

    @Test
    fun autolink() {
        assertEquals(
            "<p>Visit <a href=\"https://example.com\">https://example.com</a> or write to " +
                "<a href=\"mailto:team@example.com\">team@example.com</a>.</p>\n",
            renderWithAutolinks("Visit https://example.com or write to team@example.com."),
        )
        assertEquals(
            listOf("WWW: www.kotlinlang.org", "URL: https://example.com"),
            findLinks("See www.kotlinlang.org and https://example.com."),
        )
    }

    @Test
    fun footnotes() {
        val html = renderWithFootnotes(
            "Kotmark ports commonmark-java.[^port] Inline notes work too.^[Like this one.]\n\n" +
                "[^port]: Revisions are recorded upstream.\n",
        )
        assertEquals(
            """
            <p>Kotmark ports commonmark-java.<sup class="footnote-ref"><a href="#fn-port" id="fnref-port" data-footnote-ref>1</a></sup> Inline notes work too.<sup class="footnote-ref"><a href="#fn2" id="fnref2" data-footnote-ref>2</a></sup></p>
            <section class="footnotes" data-footnotes>
            <ol>
            <li id="fn-port">
            <p>Revisions are recorded upstream. <a href="#fnref-port" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="1" aria-label="Back to reference 1">↩</a></p>
            </li>
            <li id="fn2">
            <p>Like this one. <a href="#fnref2" class="footnote-backref" data-footnote-backref data-footnote-backref-idx="2" aria-label="Back to reference 2">↩</a></p>
            </li>
            </ol>
            </section>
            """.trimIndent() + "\n",
            html,
        )
    }

    @Test
    fun alerts() {
        assertEquals(
            """
            <div class="markdown-alert markdown-alert-info" data-alert-type="info">
            <p class="markdown-alert-title">Information</p>
            <p>Extensions are opt-in.</p>
            </div>
            <div class="markdown-alert markdown-alert-warning" data-alert-type="warning">
            <p class="markdown-alert-title">Warning</p>
            <p>Register them on both sides.</p>
            </div>
            """.trimIndent() + "\n",
            renderWithAlerts("> [!INFO]\n> Extensions are opt-in.\n\n> [!WARNING]\n> Register them on both sides.\n"),
        )
    }

    @Test
    fun strikethrough() {
        assertEquals(
            "<p><del>Old</del> new, and ~one tilde~ stays.</p>\n",
            renderWithStrikethrough("~~Old~~ new, and ~one tilde~ stays."),
        )
    }

    @Test
    fun tables() {
        assertEquals(
            """
            <table>
            <thead>
            <tr>
            <th align="left">Module</th>
            <th align="right">Kind</th>
            </tr>
            </thead>
            <tbody>
            <tr>
            <td align="left">math</td>
            <td align="right">block</td>
            </tr>
            <tr>
            <td align="left">ins</td>
            <td align="right">inline</td>
            </tr>
            </tbody>
            </table>
            """.trimIndent() + "\n",
            renderWithTables("| Module | Kind |\n| :--- | ---: |\n| math | block |\n| ins | inline |\n"),
        )
    }

    @Test
    fun headingAnchor() {
        assertEquals(
            "<h1 id=\"section-getting-started\">Getting started</h1>\n" +
                "<h2 id=\"section-getting-started-1\">Getting started</h2>\n" +
                "<h1 id=\"section-untitled\"></h1>\n",
            renderWithHeadingIds("# Getting started\n## Getting started\n#\n"),
        )
    }

    @Test
    fun highlight() {
        assertEquals("<p><mark>important <strong>text</strong></mark></p>\n", renderWithHighlight("==important **text**=="))
    }

    @Test
    fun imageAttributes() {
        assertEquals(
            "<p><img src=\"logo.svg\" alt=\"Logo\" width=\"120\" height=\"40\" /></p>\n",
            renderWithImageAttributes("![Logo](logo.svg){width=120 height=40}"),
        )
    }

    @Test
    fun ins() {
        assertEquals("<p>Kotmark is <ins>multiplatform</ins>.</p>\n", renderWithIns("Kotmark is ++multiplatform++."))
    }

    @Test
    fun math() {
        assertEquals(
            "<p>Energy: <span class=\"math math-inline\">E=mc^2</span>.</p>\n" +
                "<div class=\"math math-block\">\\sum_{k=1}^n a_k^2\n</div>\n",
            renderWithMath("Energy: \$E=mc^2\$.\n\n\$\$\n\\sum_{k=1}^n a_k^2\n\$\$\n"),
        )
        val extensions = extensions { math() }
        val document = parser { extensions(extensions) }.parse("Energy: \$E=mc^2\$.")
        assertEquals("<p>Energy: <code class=\"tex\">E=mc^2</code>.</p>\n", mathAsCodeRenderer().render(document))
    }

    @Test
    fun taskListItems() {
        assertEquals(
            "<ul>\n<li><input type=\"checkbox\" disabled=\"\" checked=\"\"> Parse</li>\n" +
                "<li><input type=\"checkbox\" disabled=\"\"> Render</li>\n</ul>\n",
            renderWithTaskLists("- [x] Parse\n- [ ] Render\n"),
        )
    }

    @Test
    fun yamlFrontMatter() {
        val page = readPage("---\ntitle: Release notes\ntags:\n  - kotlin\n  - markdown\n---\n\n# Hello\n")
        assertEquals(mapOf("title" to listOf("Release notes"), "tags" to listOf("kotlin", "markdown")), page.metadata)
        assertEquals("<h1>Hello</h1>\n", page.html)
    }

    @Test
    fun kotlinxHtml() {
        assertEquals(
            "<article><h1>Notes</h1><p>Inline <span class=\"math math-inline\">x^2</span>.</p>" +
                "<table><thead><tr><th>a</th><th>b</th></tr></thead><tbody><tr><td>1</td><td>2</td></tr></tbody></table></article>",
            renderArticle("Notes", "Inline \$x^2\$.\n\n| a | b |\n| - | - |\n| 1 | 2 |\n"),
        )
    }
}
