package io.heapy.kotmark.test

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class PathologicalTest {
    private val parser = parser()
    private val renderer = htmlRenderer()
    private val size = 100_000

    @Test
    fun nestedStrongEmphasis() {
        val count = 500
        assertRendering(
            "*a **a ".repeat(count) + "b" + " a** a*".repeat(count),
            "<p>" + "<em>a <strong>a ".repeat(count) + "b" +
                " a</strong> a</em>".repeat(count) + "</p>\n",
        )
    }

    @Test
    fun emphasisClosersWithNoOpeners() {
        assertRendering(
            "a_ ".repeat(size),
            "<p>" + "a_ ".repeat(size - 1) + "a_</p>\n",
        )
    }

    @Test
    fun emphasisOpenersWithNoClosers() {
        assertRendering(
            "_a ".repeat(size),
            "<p>" + "_a ".repeat(size - 1) + "_a</p>\n",
        )
    }

    @Test
    fun linkClosersWithNoOpeners() {
        assertRendering(
            "a] ".repeat(size),
            "<p>" + "a] ".repeat(size - 1) + "a]</p>\n",
        )
    }

    @Test
    fun linkOpenersWithNoClosers() {
        assertRendering(
            "[a ".repeat(size),
            "<p>" + "[a ".repeat(size - 1) + "[a</p>\n",
        )
    }

    @Test
    fun linkOpenersAndEmphasisClosers() {
        assertRendering(
            "[ a_ ".repeat(size),
            "<p>" + "[ a_ ".repeat(size - 1) + "[ a_</p>\n",
        )
    }

    @Test
    fun mismatchedOpenersAndClosers() {
        assertRendering(
            "*a_ ".repeat(size),
            "<p>" + "*a_ ".repeat(size - 1) + "*a_</p>\n",
        )
    }

    @Test
    fun openerSearchLowerBound() {
        val source = "a**b" + "c* ".repeat(80_000)
        assertRendering(source, "<p>${source.trim()}</p>\n")
    }

    @Test
    fun nestedBrackets() {
        val source = "[".repeat(size) + "a" + "]".repeat(size)
        assertRendering(source, "<p>$source</p>\n")
    }

    @Test
    fun nestedBlockQuotes() {
        val count = 1_000
        val source = "> ".repeat(count) + "a\n"
        val unlimitedParser = parser { maxOpenBlockParsers = Int.MAX_VALUE }
        assertEquals(
            "<blockquote>\n".repeat(count) + "<p>a</p>\n" +
                "</blockquote>\n".repeat(count),
            renderer.render(unlimitedParser.parse(source)),
        )
    }

    @Test
    fun hugeHorizontalRule() {
        assertRendering("*".repeat(10_000) + "\n", "<hr />\n")
    }

    @Test
    fun backslashInLink() {
        assertRendering(
            "[" + "\\".repeat(size) + "\n",
            "<p>[" + "\\".repeat(size / 2) + "</p>\n",
        )
    }

    @Test
    fun unclosedInlineLinks() {
        assertRendering(
            "[](".repeat(size) + "\n",
            "<p>" + "[](".repeat(size) + "</p>\n",
        )
    }

    // Nearby `>` characters keep the separate autolink scan cheap. Leading text prevents HTML
    // block parsing, so these exercise repeated inline scans for a terminator that never occurs.
    @Test
    fun htmlProcessingInstructionsWithNoEnd() {
        assertRendering("x <? >".repeat(size), "<p>" + "x &lt;? &gt;".repeat(size) + "</p>\n")
    }

    @Test
    fun htmlCommentsWithNoEnd() {
        assertRendering("x <!-- >".repeat(size), "<p>" + "x &lt;!-- &gt;".repeat(size) + "</p>\n")
    }

    @Test
    fun htmlCdataWithNoEnd() {
        assertRendering("x <![CDATA[ >".repeat(size), "<p>" + "x &lt;![CDATA[ &gt;".repeat(size) + "</p>\n")
    }

    @Test
    fun autolinkWithoutCloser() {
        val count = 160_000
        assertRendering("<".repeat(count), "<p>" + "&lt;".repeat(count) + "</p>\n")
    }

    @Test
    fun backticksDistinctLengths() {
        val source = buildString {
            for (length in 1..3_000) {
                append("`".repeat(length))
                append('x')
            }
        }
        assertRendering(source, "<p>$source</p>\n")
    }

    @Test
    fun htmlBlockAttributes() {
        val attributes = " a".repeat(4_000)
        assertRendering("<a$attributes", "<p>&lt;a$attributes</p>\n")
    }

    private fun assertRendering(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
