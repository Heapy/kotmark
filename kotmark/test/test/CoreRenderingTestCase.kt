package io.heapy.kotmark.test

import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer

/**
 * Base class for Markdown-to-HTML tests, with source context and visible tabs in assertion failures.
 */
abstract class CoreRenderingTestCase {
    protected fun render(source: String): String = RENDERER.render(PARSER.parse(source))

    protected fun assertRendering(source: String, expectedResult: String) {
        val actual = render(source)
        // Include the source and make tabs visible, so failures are easier to read (matches commonmark-java's Asserts).
        val expected = showTabs("$expectedResult\n\n$source")
        val actualShown = showTabs("$actual\n\n$source")
        if (actualShown != expected) {
            throw AssertionError("Expected:\n$expected\nActual:\n$actualShown")
        }
    }

    private fun showTabs(value: String): String = value.replace('\t', '→')

    companion object {
        private val PARSER = Parser.builder().build()
        private val RENDERER = HtmlRenderer.builder().build()
    }
}
