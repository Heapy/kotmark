package io.heapy.kotmark.test

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class PathologicalTest {
    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().build()
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
    fun nestedBrackets() {
        val source = "[".repeat(size) + "a" + "]".repeat(size)
        assertRendering(source, "<p>$source</p>\n")
    }

    @Test
    fun nestedBlockQuotes() {
        val count = 1_000
        assertRendering(
            "> ".repeat(count) + "a\n",
            "<blockquote>\n".repeat(count) + "<p>a</p>\n" +
                "</blockquote>\n".repeat(count),
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

    private fun assertRendering(source: String, expected: String) {
        assertEquals(expected, renderer.render(parser.parse(source)))
    }
}
